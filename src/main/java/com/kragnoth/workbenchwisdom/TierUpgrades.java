package com.kragnoth.workbenchwisdom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Reforging Bench upgrades from one tier of tool or armour to the next, for every mod at once. Items are found by name:
 * {@code <material>_<type>} (iron_pickaxe, refined_obsidian_helmet, armor_faraday_boots...), the type from the lists in
 * {@code config/workbenchwisdom-upgrades.json}, the material from its upgrade chain. For each material, each next
 * material, and each type both have, every pairing of their items gets a {@link ReforgingRecipe}: the old item plus
 * the next material, as many as crafting that kind of item takes ({@code costs}), gives the new item with the old
 * one's enchantments, name and wear. Upgrades the game already has at the smithing table (diamond to netherite) are
 * left to it.
 */
public final class TierUpgrades {
    private TierUpgrades() {}

    static final String FILE = "workbenchwisdom-upgrades.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** One step of the chain: to {@code next}, adding {@code ingredient}. */
    record Step(String next, Ingredient ingredient) {}

    record Chain(Map<String, List<Step>> steps, List<String> toolTypes, List<String> armorTypes, Map<String, Integer> costs, int defaultCost) {
        int cost(String type) {
            return Math.max(1, Math.min(64, costs.getOrDefault(type, defaultCost)));
        }
    }

    /** The chain from the config folder (written there from the built-in default the first time). */
    static Chain load() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(FILE);
        try {
            if (!Files.exists(path)) {
                try (InputStream in = TierUpgrades.class.getResourceAsStream("/" + WorkbenchWisdom.MODID + "/default_upgrades.json")) {
                    Files.copy(in, path);
                }
            }
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(r).getAsJsonObject());
            }
        } catch (IOException | RuntimeException e) {
            WorkbenchWisdom.LOG.error("Couldn't read {}; no smithing upgrades this time", path, e);
            return new Chain(Map.of(), List.of(), List.of(), Map.of(), 1);
        }
    }

    static Chain parse(JsonObject root) {
        Map<String, List<Step>> steps = new LinkedHashMap<>();
        JsonObject chain = root.getAsJsonObject("chain");
        for (Map.Entry<String, JsonElement> e : chain.entrySet()) {
            List<Step> list = new ArrayList<>();
            JsonArray arr = e.getValue().isJsonArray() ? e.getValue().getAsJsonArray() : new JsonArray();
            if (e.getValue().isJsonObject()) {
                arr.add(e.getValue());
            }
            for (JsonElement s : arr) {
                JsonObject o = s.getAsJsonObject();
                if (!o.has("next") || o.get("next").isJsonNull() || !o.has("ingredient")) {
                    continue; // end of the line
                }
                Ingredient ing = Ingredient.CODEC_NONEMPTY.parse(JsonOps.INSTANCE, o.get("ingredient")).result().orElse(null);
                if (ing == null) {
                    WorkbenchWisdom.LOG.warn("Upgrade {} -> {}: can't read its ingredient {}", e.getKey(), o.get("next"), o.get("ingredient"));
                    continue;
                }
                list.add(new Step(o.get("next").getAsString(), ing));
            }
            steps.put(e.getKey(), list);
        }
        Map<String, Integer> costs = new LinkedHashMap<>();
        if (root.has("costs")) {
            root.getAsJsonObject("costs").entrySet().forEach(e -> costs.put(e.getKey(), e.getValue().getAsInt()));
        }
        int def = root.has("default_cost") ? root.get("default_cost").getAsInt() : 3;
        return new Chain(steps, strings(root, "tool_types"), strings(root, "armor_types"), costs, def);
    }

    /** The automatic upgrades, one per line, so a pack maker can see (and override in the chain) what was chosen. */
    private static void writeReport(List<String> lines) {
        Path path = FMLPaths.CONFIGDIR.get().resolve("workbenchwisdom-automatic-upgrades.txt");
        List<String> out = new ArrayList<>();
        out.add("# Upgrades Workbench Wisdom worked out by itself for gear its chain (workbenchwisdom-upgrades.json) doesn't name.");
        out.add("# Rewritten on every world load and /reload. To change one, add its material to the chain.");
        out.addAll(lines);
        try {
            Files.write(path, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            WorkbenchWisdom.LOG.warn("Couldn't write {}", path, e);
        }
    }

    private static boolean exists(List<SmithingTransformRecipe> already, Item from, Item to) {
        ItemStack base = new ItemStack(from);
        for (SmithingTransformRecipe r : already) {
            try {
                if (r.isBaseIngredient(base) && r.getResultItem(net.minecraft.core.RegistryAccess.EMPTY).is(to)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // a recipe that needs the real registries for its result
            }
        }
        return false;
    }

    private static List<String> strings(JsonObject root, String key) {
        List<String> out = new ArrayList<>();
        if (root.has(key)) {
            root.getAsJsonArray(key).forEach(e -> out.add(e.getAsString()));
        }
        return out;
    }

    /** @return the recipes to add */
    static List<RecipeHolder<?>> generate(Set<ResourceLocation> taken, List<RecipeHolder<?>> existing) {
        // upgrades the game already has (diamond to netherite with the netherite template) are left to the smithing table
        List<SmithingTransformRecipe> already = new ArrayList<>();
        for (RecipeHolder<?> h : existing) {
            if (h.value() instanceof SmithingTransformRecipe r) {
                already.add(r);
            }
        }
        Chain chain = load();
        List<String> types = new ArrayList<>(chain.toolTypes());
        types.addAll(chain.armorTypes());
        types.sort(Comparator.comparingInt(String::length).reversed()); // longest first: "chestplate" before "plate"
        // material -> type -> items
        Map<String, Map<String, List<Item>>> found = new LinkedHashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            String name = BuiltInRegistries.ITEM.getKey(item).getPath();
            if (name.contains("template")) {
                continue;
            }
            for (String type : types) {
                if (name.endsWith("_" + type)) {
                    String material = name.substring(0, name.length() - type.length() - 1);
                    if (chain.steps().containsKey(material)) {
                        found.computeIfAbsent(material, k -> new LinkedHashMap<>()).computeIfAbsent(type, k -> new ArrayList<>()).add(item);
                    }
                    break;
                }
            }
        }
        List<RecipeHolder<?>> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, Map<String, List<Item>>> m : found.entrySet()) {
            for (Step step : chain.steps().get(m.getKey())) {
                Map<String, List<Item>> next = found.get(step.next());
                if (next == null) {
                    continue;
                }
                for (Map.Entry<String, List<Item>> t : m.getValue().entrySet()) {
                    List<Item> targets = next.get(t.getKey());
                    if (targets == null) {
                        continue;
                    }
                    int cost = chain.cost(t.getKey());
                    for (Item from : t.getValue()) {
                        for (Item to : targets) {
                            ResourceLocation a = BuiltInRegistries.ITEM.getKey(from);
                            ResourceLocation b = BuiltInRegistries.ITEM.getKey(to);
                            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(WorkbenchWisdom.MODID,
                                    "upgrade/" + a.getNamespace() + "/" + a.getPath() + "_to_" + b.getNamespace() + "_" + b.getPath());
                            if (from == to || !seen.add(a + ">" + b) || taken.contains(id) || exists(already, from, to)) {
                                continue;
                            }
                            out.add(new RecipeHolder<>(id, new ReforgingRecipe(Ingredient.of(from), step.ingredient(), cost, new ItemStack(to))));
                        }
                    }
                }
            }
        }
        int named = out.size();
        // everything else: worked out from what the items are (AutoTiers), the chain's own items left to the chain
        List<String> report = new ArrayList<>();
        if (Config.AUTO_UPGRADES.get()) {
            Set<Item> chained = new HashSet<>();
            found.values().forEach(byType -> byType.values().forEach(chained::addAll));
            List<String> named2 = new ArrayList<>(chain.toolTypes());
            named2.sort(Comparator.comparingInt(String::length).reversed());
            Set<Item> smithed = new HashSet<>();
            for (SmithingTransformRecipe r : already) {
                try {
                    smithed.add(r.getResultItem(net.minecraft.core.RegistryAccess.EMPTY).getItem());
                } catch (RuntimeException ignored) {
                    // a result that needs the real registries
                }
            }
            for (Map.Entry<AutoTiers.Entry, List<AutoTiers.Entry>> e : AutoTiers.next(AutoTiers.scan(named2), chained, smithed).entrySet()) {
                Item from = e.getKey().item();
                int cost = chain.cost(AutoTiers.costKey(e.getKey()));
                for (AutoTiers.Entry target : e.getValue()) {
                    Item to = target.item();
                    ResourceLocation a = BuiltInRegistries.ITEM.getKey(from);
                    ResourceLocation b = BuiltInRegistries.ITEM.getKey(to);
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(WorkbenchWisdom.MODID,
                            "upgrade/" + a.getNamespace() + "/" + a.getPath() + "_to_" + b.getNamespace() + "_" + b.getPath());
                    if (from == to || !seen.add(a + ">" + b) || taken.contains(id) || exists(already, from, to)) {
                        continue;
                    }
                    out.add(new RecipeHolder<>(id, new ReforgingRecipe(Ingredient.of(from), target.material(), cost, new ItemStack(to))));
                    ItemStack[] mats = target.material().getItems();
                    report.add(a + " -> " + b + "  (" + cost + " x " + (mats.length > 0 ? BuiltInRegistries.ITEM.getKey(mats[0].getItem()) : "?")
                            + (mats.length > 1 ? " or similar" : "") + ")");
                }
            }
        }
        writeReport(report);
        WorkbenchWisdom.LOG.info("Reforging: {} upgrade recipes from the chain ({} materials), {} worked out automatically "
                + "(listed in config/workbenchwisdom-automatic-upgrades.txt)", named, found.size(), out.size() - named);
        return out;
    }

}
