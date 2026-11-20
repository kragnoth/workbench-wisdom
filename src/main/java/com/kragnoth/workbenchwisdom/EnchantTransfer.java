package com.kragnoth.workbenchwisdom;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The Reforging Bench's other job: two tools, or two pieces of armour (from any mods, see {@link Gear}), the second's
 * enchantments (and name, trim,
 * colour and the rest of its data) moved onto the first. The second is used up. Costs experience, as the anvil does:
 * each enchantment's anvil cost times its level, plus what the items have been worked before, and the result gets the
 * usual higher price for the next rework. There's no "too expensive": whatever it costs, a player with the levels can
 * do it.
 */
public final class EnchantTransfer {
    private EnchantTransfer() {}

    public static final TagKey<Item> TOOLS = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "tools"));
    public static final TagKey<Item> ARMOR = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "armors"));

    public static boolean isGear(ItemStack s) {
        return Gear.kind(s) != Gear.Kind.NONE;
    }

    /** Whether these two can trade: both tools or both armour, and the second has something to give. */
    public static boolean pairs(ItemStack base, ItemStack donor) {
        if (base.isEmpty() || donor.isEmpty() || base.getCount() != 1 || donor.getCount() != 1) {
            return false;
        }
        boolean kind = Gear.sameKind(base, donor);
        // something worth moving: enchantments, a name, a trim... (not just wear)
        return kind && !donor.getComponentsPatch().forget(t -> t == DataComponents.DAMAGE || t == DataComponents.MAX_DAMAGE
                || t == DataComponents.UNBREAKABLE || t == DataComponents.REPAIR_COST).isEmpty();
    }

    public static ItemStack result(ItemStack base, ItemStack donor) {
        ItemStack out = base.copyWithCount(1);
        DataComponentPatch patch = donor.getComponentsPatch();
        if (!Config.TRANSFER_DAMAGE.get()) {
            // durability stays the item's own: its wear, how much it has, and whether it can break at all
            patch = patch.forget(t -> t == DataComponents.DAMAGE || t == DataComponents.MAX_DAMAGE || t == DataComponents.UNBREAKABLE);
        }
        patch = patch.forget(t -> t == DataComponents.REPAIR_COST);
        out.applyComponents(patch);
        int prior = Math.max(base.getOrDefault(DataComponents.REPAIR_COST, 0), donor.getOrDefault(DataComponents.REPAIR_COST, 0));
        out.set(DataComponents.REPAIR_COST, AnvilMenu.calculateIncreasedRepairCost(prior));
        return out;
    }

    /** Levels it costs (0 when the config turns the cost off). */
    public static int cost(ItemStack base, ItemStack donor) {
        if (!Config.TRANSFER_XP.get()) {
            return 0;
        }
        int cost = base.getOrDefault(DataComponents.REPAIR_COST, 0) + donor.getOrDefault(DataComponents.REPAIR_COST, 0);
        ItemEnchantments ench = donor.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var e : ench.entrySet()) {
            Holder<Enchantment> h = e.getKey();
            cost += h.value().getAnvilCost() * e.getIntValue();
        }
        return Math.max(1, cost);
    }
}
