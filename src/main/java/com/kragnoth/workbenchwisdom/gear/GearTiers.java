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
 * Tiered gear, read from the game itself rather than from lists: tools on tiers and armour on armour materials (nearly
 * every mod's) are grouped by kind: pickaxe, axe, shovel, hoe and sword by class, any other tiered tool by the last
 * word of its name (iron_paxel and osmium_paxel are both "paxel"), armour by slot.
 * <ul>
 *   <li>Tools rank by what they can mine: iron ore, diamond ore, obsidian. Wood and gold mine the same, so they're one
 *       tier; so are a mod's copper and stone.</li>
 *   <li>Armour ranks by its material across the whole set (total protection, then toughness, then knockback
 *       resistance, and durability last), so a material is one tier in every slot: leather, gold, chainmail, iron,
 *       diamond.</li>
 * </ul>
 * Each entry's material is its repair ingredient: what the anvil mends it with.
 */
public final class GearTiers {
    private GearTiers() {}

    /** A tiered item: its kind ("tool:pickaxe", "armor:helmet"), rank within the kind, and material. */
    public record Entry(Item item, String kind, double rank, Ingredient material, boolean target) {
        /** "pickaxe", "helmet": the part of the kind after the colon. */
        public String type() {
            return kind.substring(kind.indexOf(':') + 1);
        }
    }

    private static final BlockState[] LEVELS = {
            Blocks.IRON_ORE.defaultBlockState(), Blocks.DIAMOND_ORE.defaultBlockState(), Blocks.OBSIDIAN.defaultBlockState()};

    /** Every tiered tool and armour piece in the game. */
    public static List<Entry> scan() {
        List<Entry> out = new ArrayList<>();
        // armour materials with more than one piece: a lone piece (the turtle shell) is a special item, not a tier to
        // upgrade iron into, though it can still upgrade onwards itself
        Map<Object, Integer> pieces = new java.util.HashMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof ArmorItem a && a.getType() != ArmorItem.Type.BODY) {
                pieces.merge(a.getMaterial(), 1, Integer::sum);
            }
        }
        for (Item item : BuiltInRegistries.ITEM) {
            try {
                Entry e = entry(item);
                if (e != null) {
                    boolean lone = item instanceof ArmorItem a && pieces.getOrDefault(a.getMaterial(), 0) < 2;
                    out.add(lone ? new Entry(e.item(), e.kind(), e.rank(), e.material(), false) : e);
                }
            } catch (RuntimeException ex) {
                // a modded item that doesn't behave like its class: leave it out
            }
        }
        return out;
    }

    private static Entry entry(Item item) {
        if (item instanceof ArmorItem armor) {
            if (armor.getType() == ArmorItem.Type.BODY) {
                return null;
            }
            ArmorMaterial m = armor.getMaterial().value();
            int total = 0;
            for (ArmorItem.Type t : new ArmorItem.Type[] {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS}) {
                total += m.getDefense(t);
            }
            int durability = Math.min(9999, item.getDefaultInstance().getMaxDamage());
            double rank = total * 1000.0 + m.toughness() * 10.0 + m.knockbackResistance() + durability / 1_000_000.0;
            return new Entry(item, "armor:" + armor.getType().getName(), rank, m.repairIngredient().get(), true);
        }
        if (item instanceof TieredItem tiered) {
            String kind = item instanceof PickaxeItem ? "pickaxe" : item instanceof AxeItem ? "axe" : item instanceof ShovelItem ? "shovel"
                    : item instanceof HoeItem ? "hoe" : item instanceof SwordItem ? "sword" : lastWord(item);
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
            return new Entry(item, "tool:" + kind, level, tier.getRepairIngredient(), true);
        }
        return null;
    }

    private static String lastWord(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        int i = path.lastIndexOf('_');
        return i < 0 || i == path.length() - 1 ? null : path.substring(i + 1);
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
        return to.target() && !to.material().isEmpty() && !smithed.test(to.item());
    }
}
