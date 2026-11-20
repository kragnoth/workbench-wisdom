package com.kragnoth.workbenchwisdom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Upgrades for gear the configured chain doesn't name, worked out from what the items are. Tools built on vanilla's
 * tiers and armour built on armour materials (nearly every mod's) are grouped by kind: pickaxe, axe, shovel, hoe, sword
 * by class, other tools by the configured name endings (paxel, hammer...), armour by slot. Within a kind, each item
 * upgrades into the next tier up: the items with the lowest rank above its own (all of them, if several mods have
 * one there, so the player picks). Tools rank by what they can mine (wood level, stone, iron, diamond: gold and wood
 * are one tier, diamond and netherite another, which the smithing table already joins); armour by protection in its
 * slot, then toughness. Items of the same rank don't upgrade into each other: that's what an enchantment transfer is for.
 * The cost is the new tier's repair material (what the anvil mends it with), as many as {@code costs} says for the kind.
 */
final class AutoTiers {
    private AutoTiers() {}

    /** An item in a kind, with its rank and what upgrading into it costs. */
    record Entry(Item item, String kind, double rank, Ingredient material) {}

    private static final BlockState[] LEVELS = {
            Blocks.IRON_ORE.defaultBlockState(), Blocks.DIAMOND_ORE.defaultBlockState(), Blocks.OBSIDIAN.defaultBlockState()};

    static List<Entry> scan(List<String> namedTypes) {
        List<Entry> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            try {
                Entry e = entry(item, namedTypes);
                if (e != null) {
                    out.add(e);
                }
            } catch (RuntimeException ex) {
                // a modded item that doesn't behave like its class: leave it out
            }
        }
        return out;
    }

    private static Entry entry(Item item, List<String> namedTypes) {
        if (item instanceof ArmorItem armor) {
            if (armor.getType() == ArmorItem.Type.BODY) {
                return null; // horse and wolf armour
            }
            Holder<ArmorMaterial> m = armor.getMaterial();
            Ingredient repair = m.value().repairIngredient().get();
            double rank = m.value().getDefense(armor.getType()) * 100.0 + m.value().toughness() * 10.0;
            return new Entry(item, "armor:" + armor.getType().getName(), rank, repair);
        }
        if (item instanceof TieredItem tiered) {
            String kind = item instanceof PickaxeItem ? "pickaxe" : item instanceof AxeItem ? "axe" : item instanceof ShovelItem ? "shovel"
                    : item instanceof HoeItem ? "hoe" : item instanceof SwordItem ? "sword" : named(item, namedTypes);
            if (kind == null) {
                return null;
            }
            Tier tier = tiered.getTier();
            int level = 0;
            for (BlockState s : LEVELS) {
                if (!s.is(tier.getIncorrectBlocksForDrops())) {
                    level++;
                }
            }
            double rank = level;
            return new Entry(item, "tool:" + kind, rank, tier.getRepairIngredient());
        }
        return null;
    }

    private static String named(Item item, List<String> types) {
        String name = BuiltInRegistries.ITEM.getKey(item).getPath();
        for (String t : types) {
            if (name.endsWith("_" + t)) {
                return t;
            }
        }
        return null;
    }

    /** For each item not in {@code skip}: the items it upgrades into (the next rank up in its kind, with a material). */
    static Map<Entry, List<Entry>> next(List<Entry> all, Set<Item> skip, Set<Item> smithed) {
        Map<String, List<Entry>> byKind = new LinkedHashMap<>();
        for (Entry e : all) {
            byKind.computeIfAbsent(e.kind(), k -> new ArrayList<>()).add(e);
        }
        Map<Entry, List<Entry>> out = new LinkedHashMap<>();
        for (List<Entry> kind : byKind.values()) {
            for (Entry from : kind) {
                if (skip.contains(from.item())) {
                    continue;
                }
                double best = Double.MAX_VALUE;
                for (Entry to : kind) {
                    if (to.rank() > from.rank() && usable(to, smithed) && to.rank() < best) {
                        best = to.rank();
                    }
                }
                List<Entry> targets = new ArrayList<>();
                for (Entry to : kind) {
                    if (to.rank() == best && usable(to, smithed)) {
                        targets.add(to);
                    }
                }
                if (!targets.isEmpty()) {
                    out.put(from, targets);
                }
            }
        }
        return out;
    }

    /**
     * Whether an item can be upgraded into: it has a material to pay with, and it isn't made at the smithing table
     * (netherite gear: past diamond only by the smithing table's own way, template and all).
     */
    private static boolean usable(Entry to, Set<Item> smithed) {
        return !to.material().isEmpty() && !smithed.contains(to.item());
    }

    /** The kind name an entry's cost is looked up by ("pickaxe", "chestplate"). */
    static String costKey(Entry e) {
        String k = e.kind().substring(e.kind().indexOf(':') + 1);
        return k;
    }
}
