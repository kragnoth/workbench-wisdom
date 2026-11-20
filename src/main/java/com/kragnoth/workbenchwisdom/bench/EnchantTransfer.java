package com.kragnoth.workbenchwisdom.bench;

import com.kragnoth.workbenchwisdom.Config;
import com.kragnoth.workbenchwisdom.gear.Gear;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Two tools, or two pieces of armour, from any mods: the second's enchantments, name, trim, colour and the rest of
 * its data move onto the first, and the second is used up. Costs experience as the anvil does (each enchantment's
 * anvil cost times its level, plus how often the items have been worked), and counts as a rework for next time.
 * There's no "too expensive": a player with the levels can always do it.
 */
public final class EnchantTransfer {
    private EnchantTransfer() {}

    /** Whether these two can trade: both tools or both armour, and the second has more than wear to give. */
    public static boolean pairs(ItemStack base, ItemStack donor) {
        if (base.getCount() != 1 || donor.getCount() != 1 || !Gear.sameKind(base, donor)) {
            return false;
        }
        return !donor.getComponentsPatch().forget(t -> t == DataComponents.DAMAGE || t == DataComponents.MAX_DAMAGE
                || t == DataComponents.UNBREAKABLE || t == DataComponents.REPAIR_COST).isEmpty();
    }

    public static ItemStack result(ItemStack base, ItemStack donor) {
        ItemStack out = base.copyWithCount(1);
        DataComponentPatch patch = donor.getComponentsPatch().forget(t -> t == DataComponents.REPAIR_COST);
        if (!Config.TRANSFER_DAMAGE.get()) {
            patch = patch.forget(t -> t == DataComponents.DAMAGE || t == DataComponents.MAX_DAMAGE || t == DataComponents.UNBREAKABLE);
        }
        out.applyComponents(patch);
        int prior = Math.max(base.getOrDefault(DataComponents.REPAIR_COST, 0), donor.getOrDefault(DataComponents.REPAIR_COST, 0));
        out.set(DataComponents.REPAIR_COST, AnvilMenu.calculateIncreasedRepairCost(prior));
        return out;
    }

    /** Levels it costs (0 when the config makes transfers free). */
    public static int cost(ItemStack base, ItemStack donor) {
        if (!Config.TRANSFER_XP.get()) {
            return 0;
        }
        int cost = base.getOrDefault(DataComponents.REPAIR_COST, 0) + donor.getOrDefault(DataComponents.REPAIR_COST, 0);
        for (var e : donor.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
            cost += e.getKey().value().getAnvilCost() * e.getIntValue();
        }
        return Math.max(1, cost);
    }
}
