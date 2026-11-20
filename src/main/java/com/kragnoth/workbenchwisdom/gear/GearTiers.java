package com.kragnoth.workbenchwisdom.gear;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
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
 * Tiered gear, from what the items are. Tools on vanilla's tiers and armour on armour materials (nearly every mod's)
 * are grouped by kind: pickaxe, axe, shovel, hoe and sword by class, other tools by their name ending (paxel,
 * hammer...), armour by slot. Tools rank by what they can mine (iron ore, diamond ore, obsidian), armour by protection
 * in its slot then toughness. Each entry's material is its repair ingredient: what the anvil mends it with.
 */
public final class GearTiers {
    private GearTiers() {}

    /** A tiered item: its kind ("tool:pickaxe", "armor:helmet"), rank within the kind, and material. */
    public record Entry(Item item, String kind, double rank, Ingredient material) {
        /** "pickaxe", "helmet": the part of the kind after the colon. */
        public String type() {
            return kind.substring(kind.indexOf(':') + 1);
        }
    }

    private static final BlockState[] LEVELS = {
            Blocks.IRON_ORE.defaultBlockState(), Blocks.DIAMOND_ORE.defaultBlockState(), Blocks.OBSIDIAN.defaultBlockState()};

    /** Every tiered tool and armour piece in the game. {@code toolNames} are the name endings of tools beyond the five vanilla kinds. */
    public static List<Entry> scan(List<String> toolNames) {
        List<String> names = new ArrayList<>(toolNames);
        names.sort((a, b) -> b.length() - a.length()); // longest first: "battleaxe" before "axe"
        List<Entry> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            try {
                Entry e = entry(item, names);
                if (e != null) {
                    out.add(e);
                }
            } catch (RuntimeException ex) {
                // a modded item that doesn't behave like its class: leave it out
            }
        }
        return out;
    }

    private static Entry entry(Item item, List<String> toolNames) {
        if (item instanceof ArmorItem armor) {
            if (armor.getType() == ArmorItem.Type.BODY) {
                return null;
            }
            ArmorMaterial m = armor.getMaterial().value();
            double rank = m.getDefense(armor.getType()) * 100.0 + m.toughness() * 10.0;
            return new Entry(item, "armor:" + armor.getType().getName(), rank, m.repairIngredient().get());
        }
        if (item instanceof TieredItem tiered) {
            String kind = item instanceof PickaxeItem ? "pickaxe" : item instanceof AxeItem ? "axe" : item instanceof ShovelItem ? "shovel"
                    : item instanceof HoeItem ? "hoe" : item instanceof SwordItem ? "sword" : named(item, toolNames);
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
            return new Entry(item, "tool:" + kind, level, tier.getRepairIngredient());
        }
        return null;
    }

    private static String named(Item item, List<String> names) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        for (String n : names) {
            if (path.endsWith("_" + n)) {
                return n;
            }
        }
        return null;
    }

    /**
     * For each entry not in {@code skip}: the entries one rank up in its kind (all of them, if several mods have one
     * there). Items nothing can pay for, or that only the smithing table makes ({@code smithed}: netherite), are never
     * targets. Items of the same rank don't upgrade into each other.
     */
    public static Map<Entry, List<Entry>> next(List<Entry> all, Set<Item> skip, Predicate<Item> smithed) {
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
                    if (to.rank() > from.rank() && to.rank() < best && usable(to, smithed)) {
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

    private static boolean usable(Entry to, Predicate<Item> smithed) {
        return !to.material().isEmpty() && !smithed.test(to.item());
    }
}
