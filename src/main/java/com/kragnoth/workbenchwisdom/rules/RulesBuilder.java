package com.kragnoth.workbenchwisdom.rules;

import com.kragnoth.workbenchwisdom.Config;
import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import com.kragnoth.workbenchwisdom.gear.Gear;
import com.kragnoth.workbenchwisdom.gear.GearTiers;
import com.kragnoth.workbenchwisdom.recipe.SalvageRecipe;
import com.kragnoth.workbenchwisdom.recipe.UpgradeRecipe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

/**
 * Works out the {@link BenchRules} from what's loaded.
 *
 * <p><b>Upgrades</b>, first match wins for each item: datapack {@code workbenchwisdom:upgrade} recipes; then any
 * upgrade paths a pack added ({@link UpgradePaths}), by item name; then, for everything else, the next tier up of its
 * kind as the game itself ranks it ({@link GearTiers}), for that tier's repair material. Each upgrade costs as much material as crafting the new
 * item takes, in what it takes (3 diamonds for a pickaxe; ingredients as loaded, KubeJS changes included), or the
 * new tier's repair material at vanilla's counts when the new item has no crafting recipe. Gear crafted from other
 * gear (waxed copper armour) is a variant, never a step up.
 * Upgrades the smithing table already does (diamond to netherite) stay the smithing table's.
 *
 * <p><b>Salvage</b>: datapack {@code workbenchwisdom:salvage} recipes; then gear, into its repair material, as much as
 * crafting it took times {@code salvage.returns} (netherite gear into one of what the smithing table added); then
 * blocks made from a single other block (see {@link #blockSalvage}).
 */
public final class RulesBuilder {
    private RulesBuilder() {}

    static BenchRules build(Level level) {
        long start = System.nanoTime();
        RecipeIndex idx = RecipeIndex.of(level);
        UpgradePaths.Data data = UpgradePaths.current();
        List<GearTiers.Entry> tiers = GearTiers.scan();

        List<BenchRules.Upgrade> upgrades = new ArrayList<>();
        Set<Item> upgradeDone = new HashSet<>();
        for (RecipeHolder<UpgradeRecipe> h : level.getRecipeManager().getAllRecipesFor(WorkbenchWisdom.UPGRADE.get())) {
            UpgradeRecipe r = h.value();
            for (ItemStack base : r.base().getItems()) {
                upgrades.add(new BenchRules.Upgrade(base.getItem(), r.result().getItem(), r.addition(), r.count()));
                upgradeDone.add(base.getItem());
            }
        }
        int fromPaths = 0;
        int automatic = 0;
        if (Config.UPGRADES.get()) {
            Set<Item> named = new HashSet<>();
            List<BenchRules.Upgrade> paths = pathUpgrades(idx, data, named);
            for (BenchRules.Upgrade u : paths) {
                if (!upgradeDone.contains(u.from())) {
                    upgrades.add(u);
                    fromPaths++;
                }
            }
            upgradeDone.addAll(named);
            if (Config.AUTO_UPGRADES.get()) {
                for (Map.Entry<GearTiers.Entry, List<GearTiers.Entry>> e : GearTiers.next(tiers, upgradeDone, i -> idx.isSmithed(i) || idx.isMadeFromGear(i)).entrySet()) {
                    for (GearTiers.Entry to : e.getValue()) {
                        Item from = e.getKey().item();
                        if (from != to.item() && !idx.smithingMakes(from, to.item())) {
                            RecipeIndex.Material m = idx.material(to.item(), to.material());
                            Ingredient pay = m != null ? m.ingredient() : to.material();
                            int cost = m != null ? m.count() : data.cost(to.type());
                            upgrades.add(new BenchRules.Upgrade(from, to.item(), pay, cost));
                            automatic++;
                        }
                    }
                }
            }
        }
        sortTargets(upgrades);

        List<BenchRules.Salvage> salvage = new ArrayList<>();
        Set<Item> salvageDone = new HashSet<>();
        for (RecipeHolder<SalvageRecipe> h : level.getRecipeManager().getAllRecipesFor(WorkbenchWisdom.SALVAGE.get())) {
            SalvageRecipe r = h.value();
            for (ItemStack in : r.input().getItems()) {
                if (salvageDone.add(in.getItem())) {
                    boolean gear = Gear.isGear(in);
                    salvage.add(new BenchRules.Salvage(in.getItem(), r.result().copyWithCount(1), r.result().getCount(), gear));
                }
            }
        }
        double returns = Config.SALVAGE_RETURNS.get();
        int gearCount = 0;
        if (Config.SALVAGE_GEAR.get()) {
            for (GearTiers.Entry e : tiers) {
                BenchRules.Salvage s = gearSalvage(idx, data, e, returns);
                if (s != null && salvageDone.add(e.item())) {
                    salvage.add(s);
                    gearCount++;
                }
            }
        }
        int blockCount = 0;
        if (Config.SALVAGE_BLOCKS.get()) {
            for (Item item : idx.madeItems()) {
                if (salvageDone.contains(item)) {
                    continue;
                }
                BenchRules.Salvage s = blockSalvage(idx, item, returns);
                if (s != null) {
                    salvage.add(s);
                    salvageDone.add(item);
                    blockCount++;
                }
            }
        }
        WorkbenchWisdom.LOG.info("Reforging Bench: {} upgrades ({} from upgrade paths, {} by tier), {} salvages ({} gear, {} blocks) in {} ms",
                upgrades.size(), fromPaths, automatic, salvage.size(), gearCount, blockCount, (System.nanoTime() - start) / 1_000_000);
        writeReport(upgrades, salvage);
        return new BenchRules(upgrades, salvage);
    }

    /** Everything the bench worked out, one per line, for pack makers: {@code config/workbenchwisdom-rules.txt}. */
    private static void writeReport(List<BenchRules.Upgrade> upgrades, List<BenchRules.Salvage> salvage) {
        List<String> out = new ArrayList<>();
        out.add("# What the Reforging Bench does, worked out on the last world load or /reload. Rewritten every time.");
        out.add("# Change it with datapacks: upgrade_paths / gear_types data, workbenchwisdom:upgrade and workbenchwisdom:salvage");
        out.add("# recipes, and the workbenchwisdom:no_salvage item tag.");
        out.add("");
        out.add("## Upgrades: item + count x material -> new item");
        for (BenchRules.Upgrade u : upgrades) {
            ItemStack[] mats = u.material().getItems();
            out.add(key(u.from()) + " + " + u.count() + " x " + (mats.length > 0 ? key(mats[0].getItem()) : "?")
                    + (mats.length > 1 ? " (or similar)" : "") + " -> " + key(u.to()));
        }
        out.add("");
        out.add("## Salvage: item -> material per item");
        for (BenchRules.Salvage s : salvage) {
            out.add(key(s.item()) + " -> " + String.format(java.util.Locale.ROOT, "%.3f", s.perItem()) + " x " + key(s.material().getItem())
                    + (s.gear() ? " (gear)" : ""));
        }
        java.nio.file.Path path = net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("workbenchwisdom-rules.txt");
        try {
            java.nio.file.Files.write(path, out, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            WorkbenchWisdom.LOG.warn("Couldn't write {}", path, e);
        }
    }

    /** Upgrades from the upgrade paths, matching gear by name: {@code <material>_<type>}. Adds every named item to {@code named}. */
    private static List<BenchRules.Upgrade> pathUpgrades(RecipeIndex idx, UpgradePaths.Data data, Set<Item> named) {
        List<String> types = new ArrayList<>(data.typeNames());
        types.sort(Comparator.comparingInt(String::length).reversed()); // "chestplate" before "plate"
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
                    if (data.materials().containsKey(material)) {
                        found.computeIfAbsent(material, k -> new LinkedHashMap<>()).computeIfAbsent(type, k -> new ArrayList<>()).add(item);
                        named.add(item);
                    }
                    break;
                }
            }
        }
        List<BenchRules.Upgrade> out = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<Item>>> m : found.entrySet()) {
            for (UpgradePaths.Step step : data.materials().get(m.getKey())) {
                Map<String, List<Item>> next = found.get(step.to());
                if (next == null) {
                    continue;
                }
                for (Map.Entry<String, List<Item>> t : m.getValue().entrySet()) {
                    for (Item from : t.getValue()) {
                        for (Item to : next.getOrDefault(t.getKey(), List.of())) {
                            if (from != to && !idx.smithingMakes(from, to)) {
                                out.add(new BenchRules.Upgrade(from, to, step.ingredient(), cost(idx, data, to, step.ingredient(), t.getKey())));
                            }
                        }
                    }
                }
            }
        }
        return out;
    }

    /** How much of {@code material} upgrading into {@code to} takes: what crafting it takes, else the type's listed cost. */
    private static int cost(RecipeIndex idx, UpgradePaths.Data data, Item to, Ingredient material, String type) {
        OptionalInt made = idx.madeFrom(to, material);
        return made.isPresent() && made.getAsInt() > 0 ? made.getAsInt() : data.cost(type);
    }

    /**
     * Where several mods have gear at the next tier, the bench offers the one closest to home first: same mod as the
     * old item, then vanilla, then the rest by name.
     */
    private static void sortTargets(List<BenchRules.Upgrade> upgrades) {
        upgrades.sort(Comparator.comparingInt((BenchRules.Upgrade u) -> {
            String from = BuiltInRegistries.ITEM.getKey(u.from()).getNamespace();
            String to = BuiltInRegistries.ITEM.getKey(u.to()).getNamespace();
            return to.equals(from) ? 0 : to.equals("minecraft") ? 1 : 2;
        }).thenComparing(u -> BuiltInRegistries.ITEM.getKey(u.to()).toString()));
    }

    private static BenchRules.Salvage gearSalvage(RecipeIndex idx, UpgradePaths.Data data, GearTiers.Entry e, double returns) {
        Ingredient added = idx.smithingAddition(e.item());
        if (added != null) {
            ItemStack[] one = added.getItems();
            return Config.SALVAGE_SMITHED.get() && one.length > 0
                    ? new BenchRules.Salvage(e.item(), one[0].copyWithCount(1), 1, true) : null;
        }
        RecipeIndex.Material m = idx.material(e.item(), e.material());
        Ingredient material = m != null ? m.ingredient() : e.material();
        ItemStack[] mats = material.getItems();
        if (mats.length == 0 || idx.isMadeFromGear(e.item())) {
            return null; // nothing to give back, or a variant of another piece (salvage that one instead)
        }
        int n = m != null ? m.count() : data.cost(e.type());
        int back = (int) Math.floor(n * returns + 1e-9);
        return back > 0 ? new BenchRules.Salvage(e.item(), mats[0].copyWithCount(1), back, true) : null;
    }

    /**
     * A block made of a single other block: anything a stonecutter cuts from a block, and any block crafted from one
     * kind of block (two-by-two bricks, slabs, stairs, walls and panes at the crafting table, a mod's catwalks). It
     * breaks back down into that block at {@code returns} of what the cheapest recipe for it costs, so nothing can come out of
     * salvage ahead: making the item any other way costs at least as much. Left alone:
     * <ul>
     *   <li>gear (salvaged as gear) and anything tagged {@code workbenchwisdom:no_salvage};</li>
     *   <li>items the game already turns back into their material (an iron block into ingots, an ingot into
     *       nuggets): no need, and salvage would only lose some.</li>
     * </ul>
     * Where several single-material recipes make an item, the cheapest decides, a stonecutter recipe winning a tie.
     */
    static BenchRules.Salvage blockSalvage(RecipeIndex idx, Item item, double returns) {
        ItemStack stack = new ItemStack(item);
        if (!isBlock(item) || Gear.isGear(stack) || stack.is(WorkbenchWisdom.NO_SALVAGE)) {
            return null;
        }
        RecipeIndex.Source best = null;
        for (RecipeIndex.Source s : idx.sources(item)) {
            if (idx.isMadeFrom(s.input(), item)) {
                return null; // the game already turns it back into what it's made of
            }
            if (!isBlock(s.input())) {
                continue; // a block from items (bricks from brick, a door from ingots) isn't a cut of anything
            }
            if (best == null || s.cost() < best.cost() - 1e-9 || (Math.abs(s.cost() - best.cost()) < 1e-9 && s.cut() && !best.cut())) {
                best = s;
            }
        }
        if (best == null) {
            return null;
        }
        double per = best.cost() * returns;
        return per > 0 ? new BenchRules.Salvage(item, new ItemStack(best.input()), per, false) : null;
    }

    /** A placeable block in its own right: not seeds and the like, whose item only plants a block. */
    private static boolean isBlock(Item item) {
        return item instanceof net.minecraft.world.item.BlockItem && !(item instanceof net.minecraft.world.item.ItemNameBlockItem);
    }

    static ResourceLocation key(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
