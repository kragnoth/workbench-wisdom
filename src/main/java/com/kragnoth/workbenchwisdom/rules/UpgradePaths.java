package com.kragnoth.workbenchwisdom.rules;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Datapack data under {@code data/<namespace>/workbenchwisdom/}:
 * <ul>
 *   <li>{@code upgrade_paths/*.json}: {@code {"materials": {"iron": [{"to": "diamond", "ingredient": {...}}], ...}}}.
 *       Gear is matched by name, {@code <material>_<type>} (iron_pickaxe, refined_obsidian_helmet). Each material
 *       lists what it upgrades into and what the bench takes to do it; an empty list ends a line. Files merge in pack
 *       order and a later file's entry for a material replaces an earlier one.</li>
 *   <li>{@code gear_types/*.json}: {@code {"tools": {"paxel": 7, ...}, "armor": {"helmet": 5, ...}}}: the type
 *       names gear is matched by, each with how much material to charge when the new item has no crafting recipe to
 *       go by. Merged the same way.</li>
 * </ul>
 */
public final class UpgradePaths extends SimpleJsonResourceReloadListener {
    public record Step(String to, Ingredient ingredient) {}

    public record Data(Map<String, List<Step>> materials, Map<String, Integer> tools, Map<String, Integer> armor) {
        public static final Data EMPTY = new Data(Map.of(), Map.of(), Map.of());

        /** The fallback cost for a type ("pickaxe"), 3 when it isn't listed. */
        public int cost(String type) {
            Integer c = tools.get(type);
            if (c == null) {
                c = armor.get(type);
            }
            return c == null ? 3 : Math.max(1, Math.min(64, c));
        }
    }

    private static volatile Data current = Data.EMPTY;

    public static Data current() {
        return current;
    }

    public UpgradePaths() {
        super(new Gson(), WorkbenchWisdom.MODID);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller profiler) {
        Map<String, List<Step>> materials = new LinkedHashMap<>();
        Map<String, Integer> tools = new LinkedHashMap<>();
        Map<String, Integer> armor = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> f : files.entrySet()) {
            String path = f.getKey().getPath();
            try {
                JsonObject root = f.getValue().getAsJsonObject();
                if (path.startsWith("upgrade_paths/")) {
                    for (Map.Entry<String, JsonElement> m : root.getAsJsonObject("materials").entrySet()) {
                        materials.put(m.getKey(), steps(f.getKey(), m.getKey(), m.getValue()));
                    }
                } else if (path.startsWith("gear_types/")) {
                    counts(root, "tools", tools);
                    counts(root, "armor", armor);
                }
            } catch (RuntimeException e) {
                WorkbenchWisdom.LOG.error("Couldn't read {}: {}", f.getKey(), e.toString());
            }
        }
        current = new Data(Map.copyOf(materials), Map.copyOf(tools), Map.copyOf(armor));
        WorkbenchWisdom.LOG.info("Upgrade paths: {} materials, {} gear types", materials.size(), tools.size() + armor.size());
    }

    private static List<Step> steps(ResourceLocation file, String material, JsonElement value) {
        List<Step> out = new ArrayList<>();
        for (JsonElement e : value.getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject();
            String to = o.get("to").getAsString();
            // an ingredient naming an item from a mod that isn't installed just doesn't apply
            Ingredient.CODEC_NONEMPTY.parse(JsonOps.INSTANCE, o.get("ingredient")).resultOrPartial(
                    err -> WorkbenchWisdom.LOG.debug("{}: {} -> {} skipped: {}", file, material, to, err))
                    .ifPresent(i -> out.add(new Step(to, i)));
        }
        return List.copyOf(out);
    }

    private static void counts(JsonObject root, String key, Map<String, Integer> into) {
        if (root.has(key)) {
            root.getAsJsonObject(key).entrySet().forEach(e -> into.put(e.getKey(), e.getValue().getAsInt()));
        }
    }
}
