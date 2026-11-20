package com.kragnoth.workbenchwisdom.bench;

import com.kragnoth.workbenchwisdom.Config;
import com.kragnoth.workbenchwisdom.gear.Gear;
import com.kragnoth.workbenchwisdom.rules.BenchRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * What the Reforging Bench will do with what's on it, worked out the same way on the server and (from the rules the
 * server sent) on the client.
 *
 * @param result    what comes out (empty when it can't, e.g. an upgrade short of material)
 * @param baseUse   how many of the left item it takes
 * @param addUse    how many of the right item it takes
 * @param levels    experience levels a transfer costs
 * @param missing   how much more material an upgrade needs
 * @param xp        experience points salvage gives back
 * @param material  an upgrade's material, for the screen to name
 */
public record BenchAction(Mode mode, ItemStack result, int baseUse, int addUse, int levels, int missing, int xp, Ingredient material) {
    public enum Mode { NONE, UPGRADE, TRANSFER, SALVAGE }

    public static final BenchAction NONE = new BenchAction(Mode.NONE, ItemStack.EMPTY, 0, 0, 0, 0, 0, Ingredient.EMPTY);

    public static BenchAction of(ItemStack base, ItemStack add, BenchRules rules) {
        if (base.isEmpty()) {
            return NONE;
        }
        if (add.isEmpty()) {
            return salvage(base, rules);
        }
        if (Config.UPGRADES.get() && base.getCount() == 1) {
            for (BenchRules.Upgrade u : rules.upgrades(base.getItem())) {
                if (u.material().test(add)) {
                    if (add.getCount() < u.count()) {
                        return new BenchAction(Mode.UPGRADE, ItemStack.EMPTY, 0, 0, 0, u.count() - add.getCount(), 0, u.material());
                    }
                    return new BenchAction(Mode.UPGRADE, upgraded(base, u), 1, u.count(), 0, 0, 0, u.material());
                }
            }
        }
        if (Config.TRANSFER.get() && EnchantTransfer.pairs(base, add)) {
            return new BenchAction(Mode.TRANSFER, EnchantTransfer.result(base, add), 1, 1, EnchantTransfer.cost(base, add), 0, 0, Ingredient.EMPTY);
        }
        return NONE;
    }

    /** The upgraded item: everything on the old one (enchantments, name, wear) on the new item. */
    private static ItemStack upgraded(ItemStack base, BenchRules.Upgrade u) {
        ItemStack out = base.transmuteCopy(u.to(), 1);
        if (out.isDamageableItem() && out.getDamageValue() >= out.getMaxDamage()) {
            out.setDamageValue(out.getMaxDamage() - 1);
        }
        return out;
    }

    private static BenchAction salvage(ItemStack base, BenchRules rules) {
        BenchRules.Salvage s = rules.salvage(base.getItem());
        if (s == null) {
            return NONE;
        }
        ItemStack material = s.material();
        if (s.gear()) {
            if (base.getCount() != 1) {
                return NONE;
            }
            double whole = base.isDamageableItem() ? (base.getMaxDamage() - base.getDamageValue()) / (double) base.getMaxDamage() : 1.0;
            int amount = Math.min(material.getMaxStackSize(), (int) Math.round(s.perItem() * whole));
            return amount <= 0 ? NONE
                    : new BenchAction(Mode.SALVAGE, material.copyWithCount(amount), 1, 0, 0, 0, enchantmentXp(base), Ingredient.EMPTY);
        }
        int amount = Math.min(material.getMaxStackSize(), (int) Math.floor(base.getCount() * s.perItem() + 1e-9));
        if (amount <= 0) {
            return NONE;
        }
        // only as many as that amount needs: 64 slabs make 21 stone from 63 of them, and one stays
        int used = Math.min(base.getCount(), (int) Math.ceil(amount / s.perItem() - 1e-9));
        return new BenchAction(Mode.SALVAGE, material.copyWithCount(amount), used, 0, 0, 0, 0, Ingredient.EMPTY);
    }

    /** Half of what the enchantments would take to put on, like the grindstone without its randomness. Curses: nothing. */
    static int enchantmentXp(ItemStack gear) {
        if (!Config.SALVAGE_XP.get() || !Gear.isGear(gear)) {
            return 0;
        }
        int xp = 0;
        for (var e : gear.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
            if (!e.getKey().is(EnchantmentTags.CURSE)) {
                xp += e.getKey().value().getMinCost(e.getIntValue());
            }
        }
        return xp / 2;
    }
}
