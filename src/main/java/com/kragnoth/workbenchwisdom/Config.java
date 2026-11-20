package com.kragnoth.workbenchwisdom;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code config/workbenchwisdom-common.toml}: switches and balance numbers. Which materials upgrade into which, and
 * the gear types the bench recognises by name, are datapack data ({@code data/<ns>/workbenchwisdom/upgrade_paths} and
 * {@code gear_types}); extra upgrades and salvage are datapack recipes ({@code workbenchwisdom:upgrade},
 * {@code workbenchwisdom:salvage}).
 */
public final class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    static {
        B.push("transfer");
    }

    public static final ModConfigSpec.BooleanValue TRANSFER = B.comment(
            "Reforging Bench: two tools (or two pieces of armour); the first gets the second's enchantments, name and",
            "other data, and the second is used up.").define("enabled", true);
    public static final ModConfigSpec.BooleanValue TRANSFER_XP = B.comment(
            "Transfers cost experience, worked out like the anvil's but never \"too expensive\" (off: free).")
            .define("costsExperience", true);
    public static final ModConfigSpec.BooleanValue TRANSFER_DAMAGE = B.comment(
            "Also carry over how worn the second item is (off: the first keeps its own durability).")
            .define("carryDamage", false);

    static {
        B.pop().push("upgrades");
    }

    public static final ModConfigSpec.BooleanValue UPGRADES = B.comment(
            "Reforging Bench: tools and armour upgrade to the next tier, keeping their enchantments, for as much of the",
            "next tier's material as crafting the new item takes.").define("enabled", true);
    public static final ModConfigSpec.BooleanValue AUTO_UPGRADES = B.comment(
            "Also work out upgrades for gear the upgrade paths don't name, from what the items are: each tool or armour",
            "piece into the next tier up of its kind, for that tier's repair material.").define("automatic", true);

    static {
        B.pop().push("salvage");
    }

    public static final ModConfigSpec.BooleanValue SALVAGE_GEAR = B.comment(
            "Reforging Bench: a tool or armour piece on its own breaks down into its material.").define("gear", true);
    public static final ModConfigSpec.BooleanValue SALVAGE_BLOCKS = B.comment(
            "Reforging Bench: blocks made from a single other block (anything the stonecutter cuts from a block, and blocks",
            "crafted from one kind of block: slabs, stairs, walls, panes, catwalks...) break back down into it.")
            .define("blocks", true);
    public static final ModConfigSpec.DoubleValue SALVAGE_RETURNS = B.comment(
            "How much of what an item took to make salvage gives back. Worn gear gives less in proportion.")
            .defineInRange("returns", 0.67, 0.0, 1.0);
    public static final ModConfigSpec.BooleanValue SALVAGE_XP = B.comment(
            "Salvaging enchanted gear gives back half of what its enchantments are worth in experience (curses nothing).")
            .define("experience", true);
    public static final ModConfigSpec.BooleanValue SALVAGE_SMITHED = B.comment(
            "Gear made at the smithing table (netherite) salvages to one of what the smithing table added (a netherite",
            "ingot) while it's at least half whole. The template and the gear underneath stay lost.")
            .define("smithedGear", true);

    static {
        B.pop().push("blocks");
    }

    public static final ModConfigSpec.BooleanValue FAMILY_CUTTING = B.comment(
            "The stonecutter cuts every mod's blocks into the stairs, slabs and walls named after them, and planks into",
            "their whole wood set. Recipes the stonecutter already has aren't doubled.").define("familyCutting", true);
    public static final ModConfigSpec.BooleanValue BACK_TO_BLOCKS = B.comment(
            "Crafting: two slabs side by side make their block back, four stairs in a square make three blocks.",
            "Never where another recipe already uses that arrangement.").define("slabsAndStairsToBlocks", true);

    static {
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    private Config() {}
}
