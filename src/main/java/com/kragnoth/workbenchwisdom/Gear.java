package com.kragnoth.workbenchwisdom;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BookItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;

/**
 * What counts as gear, whatever mod it's from: something worn (armour) or something used (tools and weapons). Tags
 * first ({@code c:armors}, {@code c:tools}); then what the item is: armour items and anything else worn in an armour
 * slot that wears out or takes enchantments (an elytra, a modded backpack-plate...); tools, swords, bows, tridents,
 * maces, shields, shears, fishing rods by their class; and anything else that wears out and takes enchantments (every
 * mod's odd weapon). Nothing here needs the item to actually have durability left, so gear made unbreakable by a "no
 * durability" mod still counts. Pumpkins and heads are worn but neither wear out nor take enchantments.
 */
public final class Gear {
    private Gear() {}

    public enum Kind { NONE, TOOL, ARMOR }

    public static Kind kind(ItemStack s) {
        if (s.isEmpty()) {
            return Kind.NONE;
        }
        if (s.getItem() instanceof ArmorItem a && a.getType() == ArmorItem.Type.BODY) {
            return Kind.NONE; // horse and wolf armour: worn, but not by players
        }
        if (s.is(EnchantTransfer.ARMOR) || s.getItem() instanceof ArmorItem) {
            return Kind.ARMOR;
        }
        if (s.is(EnchantTransfer.TOOLS)) {
            return Kind.TOOL;
        }
        // Whether it wears out isn't required on its own: a "no durability" mod takes that away from everything.
        boolean wears = s.isDamageableItem() || s.getItem().getDefaultInstance().has(DataComponents.MAX_DAMAGE);
        boolean enchantable = s.getItem().getEnchantmentValue(s) > 0 && !(s.getItem() instanceof BookItem)
                && !(s.getItem() instanceof EnchantedBookItem);
        Equipable eq = Equipable.get(s);
        if (eq != null && eq.getEquipmentSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR
                && (wears || enchantable || s.getItem() instanceof ElytraItem)) {
            return Kind.ARMOR;
        }
        Item item = s.getItem();
        if (item instanceof TieredItem || item instanceof ProjectileWeaponItem || item instanceof TridentItem || item instanceof MaceItem
                || item instanceof ShieldItem || item instanceof FishingRodItem || item instanceof ShearsItem
                || item instanceof FlintAndSteelItem || item instanceof BrushItem || s.has(DataComponents.TOOL)) {
            return Kind.TOOL;
        }
        if (wears && (enchantable || s.isEnchantable())) {
            return Kind.TOOL;
        }
        return Kind.NONE;
    }

    /** Both the same kind of gear (tool and tool, armour and armour). */
    public static boolean sameKind(ItemStack a, ItemStack b) {
        Kind k = kind(a);
        return k != Kind.NONE && k == kind(b);
    }
}
