package com.kragnoth.workbenchwisdom;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/** {@code config/workbenchwisdom-common.toml}. The upgrade chain itself is {@code config/workbenchwisdom-upgrades.json}. */
public final class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue TRANSFER = B.comment(
            "Reforging Bench: two tools (or two pieces of armour); the first gets the second's enchantments, name and",
            "other data, and the second is used up.").define("transfer.enabled", true);
    public static final ModConfigSpec.BooleanValue TRANSFER_XP = B.comment(
            "Transfers cost experience, worked out like the anvil's (off: free).").define("transfer.costsExperience", true);
    public static final ModConfigSpec.BooleanValue TRANSFER_DAMAGE = B.comment(
            "Also carry over how worn the second item is (off: the first keeps its own durability).")
            .define("transfer.carryDamage", false);

    public static final ModConfigSpec.BooleanValue STONECUTTER_REVERSE = B.comment(
            "Every stonecutter recipe can be cut back the other way (slab back to stone, stairs back to planks...).")
            .define("stonecutter.reverse", true);
    public static final ModConfigSpec.BooleanValue STONECUTTER_ONE_TO_ONE = B.comment(
            "Stonecutter recipes that make more than one item make one, so cutting back and forth can't duplicate",
            "anything. Turn off only with stonecutter.reverse off, or a block can be cut into two slabs and each slab",
            "back into a block.").define("stonecutter.oneToOne", true);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> STONECUTTER_SKIP = B.comment(
            "Recipe ids (or whole namespaces, as \"modid:*\") the stonecutter changes leave alone.")
            .defineListAllowEmpty("stonecutter.skip", List.of(), () -> "", o -> o instanceof String);

    public static final ModConfigSpec.BooleanValue UPGRADES = B.comment(
            "Reforging Bench: upgrades from one tool or armour tier to the next, for as much of the next tier's material",
            "as crafting the item takes (see workbenchwisdom-upgrades.json).").define("upgrades.enabled", true);

    public static final ModConfigSpec.BooleanValue AUTO_UPGRADES = B.comment(
            "Also work out upgrades for gear the chain doesn't name, from what the items are: each tool or armour piece into",
            "the next tier up of its kind, for that tier's repair material (listed in workbenchwisdom-automatic-upgrades.txt).")
            .define("upgrades.automatic", true);

    public static final ModConfigSpec.BooleanValue SALVAGE = B.comment(
            "Reforging Bench: a tool or armour piece on its own breaks down into some of its material.")
            .define("salvage.enabled", true);
    public static final ModConfigSpec.DoubleValue SALVAGE_RETURNS = B.comment(
            "How much of what the item took to make salvage gives back (unworn; worn gear gives less in proportion).")
            .defineInRange("salvage.returns", 0.67, 0.0, 1.0);
    public static final ModConfigSpec.BooleanValue SALVAGE_XP = B.comment(
            "Salvaging enchanted gear gives back some experience, as the grindstone does.")
            .define("salvage.experience", true);
    public static final ModConfigSpec.BooleanValue SALVAGE_SMITHED = B.comment(
            "Whether smithing-table gear (netherite) salvages back to one of what the smithing table added (a netherite ingot).",
            "The template and the diamond gear underneath stay lost. Turn off to keep netherite out of salvage entirely.")
            .define("salvage.smithedGear", true);

    public static final ModConfigSpec.BooleanValue FAMILY_CUTTING = B.comment(
            "The stonecutter cuts every mod's blocks into the stairs, slabs and walls named after them, and planks into",
            "their whole wood set (stairs, slab, fence, gate, door, trapdoor, button, pressure plate, sign).")
            .define("stonecutter.families", true);
    public static final ModConfigSpec.BooleanValue BACK_TO_BLOCKS = B.comment(
            "Crafting: two slabs side by side make their block back, four stairs in a square make three blocks.")
            .define("crafting.slabsAndStairsToBlocks", true);

    public static final ModConfigSpec SPEC = B.build();

    private Config() {}
}
