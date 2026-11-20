package com.kragnoth.workbenchwisdom.gear;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BookItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;

/**
 * What counts as gear, whatever mod it's from: something worn (armour) or something used (tools and weapons). Tags
 * first ({@code c:armors}, {@code c:tools}); then what the item is: armour items and anything else worn in an armour
 * slot that wears out or takes enchantments (an elytra...); tools, weapons, shields, shears, fishing rods by class;
 * and anything else that wears out and takes enchantments. Nothing here needs durability left, so gear a "no
 * durability" mod made unbreakable still counts. Pumpkins and heads are worn but neither wear out nor take
 * enchantments; horse and wolf armour isn't worn by players.
 */
public final class Gear {
    private Gear() {}

    public static final TagKey<Item> TOOLS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools"));
    public static final TagKey<Item> ARMOR = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "armors"));

    public enum Kind { NONE, TOOL, ARMOR }

    public static Kind kind(ItemStack s) {
        if (s.isEmpty()) {
            return Kind.NONE;
        }
        Item item = s.getItem();
        if (item instanceof ArmorItem a && a.getType() == ArmorItem.Type.BODY) {
            return Kind.NONE;
        }
        if (s.is(ARMOR) || item instanceof ArmorItem) {
            return Kind.ARMOR;
        }
        if (s.is(TOOLS)) {
            return Kind.TOOL;
        }
        boolean wears = s.isDamageableItem() || item.getDefaultInstance().has(DataComponents.MAX_DAMAGE);
        boolean enchantable = item.getEnchantmentValue(s) > 0 && !(item instanceof BookItem) && !(item instanceof EnchantedBookItem);
        Equipable eq = Equipable.get(s);
        if (eq != null && eq.getEquipmentSlot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR
                && (wears || enchantable || item instanceof ElytraItem)) {
            return Kind.ARMOR;
        }
        if (item instanceof TieredItem || item instanceof ProjectileWeaponItem || item instanceof TridentItem || item instanceof MaceItem
                || item instanceof ShieldItem || item instanceof FishingRodItem || item instanceof ShearsItem
                || item instanceof FlintAndSteelItem || item instanceof BrushItem || s.has(DataComponents.TOOL)) {
            return Kind.TOOL;
        }
        return wears && (enchantable || s.isEnchantable()) ? Kind.TOOL : Kind.NONE;
    }

    public static boolean isGear(ItemStack s) {
        return kind(s) != Kind.NONE;
    }

    /** Both the same kind of gear (tool and tool, armour and armour). */
    public static boolean sameKind(ItemStack a, ItemStack b) {
        Kind k = kind(a);
        return k != Kind.NONE && k == kind(b);
    }
}
