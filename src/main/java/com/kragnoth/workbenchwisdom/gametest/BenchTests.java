package com.kragnoth.workbenchwisdom.gametest;

import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import com.kragnoth.workbenchwisdom.bench.BenchAction;
import com.kragnoth.workbenchwisdom.bench.ReforgingMenu;
import com.kragnoth.workbenchwisdom.rules.BenchRules;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** {@code ./gradlew runGameTestServer}: the bench's rules and the block-family recipes against vanilla's own items. */
@GameTestHolder(WorkbenchWisdom.MODID)
@PrefixGameTestTemplate(false)
public final class BenchTests {
    private BenchTests() {}

    private static BenchAction bench(GameTestHelper h, ItemStack base, ItemStack add) {
        return BenchAction.of(base, add, BenchRules.of(h.getLevel()));
    }

    private static void expect(GameTestHelper h, String what, ItemStack got, Item item, int count) {
        if (!got.is(item) || got.getCount() != count) {
            h.fail(what + ": expected " + count + " " + item + ", got " + got);
        }
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) {
            h.fail(what);
        }
    }

    @GameTest(template = "empty")
    public static void salvageGear(GameTestHelper h) {
        expect(h, "diamond pickaxe", bench(h, new ItemStack(Items.DIAMOND_PICKAXE), ItemStack.EMPTY).result(), Items.DIAMOND, 2);
        expect(h, "iron chestplate", bench(h, new ItemStack(Items.IRON_CHESTPLATE), ItemStack.EMPTY).result(), Items.IRON_INGOT, 5);
        ItemStack worn = new ItemStack(Items.IRON_CHESTPLATE);
        worn.setDamageValue(worn.getMaxDamage() / 2);
        expect(h, "half-worn iron chestplate", bench(h, worn, ItemStack.EMPTY).result(), Items.IRON_INGOT, 3);
        expect(h, "netherite sword", bench(h, new ItemStack(Items.NETHERITE_SWORD), ItemStack.EMPTY).result(), Items.NETHERITE_INGOT, 1);
        ItemStack broken = new ItemStack(Items.NETHERITE_SWORD);
        broken.setDamageValue(broken.getMaxDamage() * 3 / 4);
        check(h, bench(h, broken, ItemStack.EMPTY).mode() == BenchAction.Mode.NONE, "a three-quarters-worn netherite sword gives nothing");
        check(h, bench(h, new ItemStack(Items.GOLDEN_SHOVEL), ItemStack.EMPTY).mode() == BenchAction.Mode.NONE, "a one-ingot shovel gives nothing");
        ItemStack enchanted = new ItemStack(Items.DIAMOND_SWORD);
        enchanted.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS), 5);
        check(h, bench(h, enchanted, ItemStack.EMPTY).xp() > 0, "enchanted gear gives experience back");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void salvageBlocks(GameTestHelper h) {
        BenchAction slabs = bench(h, new ItemStack(Items.STONE_SLAB, 64), ItemStack.EMPTY);
        expect(h, "64 stone slabs", slabs.result(), Items.STONE, 21);
        check(h, slabs.baseUse() == 63, "64 stone slabs use 63, not " + slabs.baseUse());
        expect(h, "64 stone bricks", bench(h, new ItemStack(Items.STONE_BRICKS, 64), ItemStack.EMPTY).result(), Items.STONE, 42);
        expect(h, "3 polished andesite", bench(h, new ItemStack(Items.POLISHED_ANDESITE, 3), ItemStack.EMPTY).result(), Items.ANDESITE, 2);
        check(h, bench(h, new ItemStack(Items.STONE_SLAB, 1), ItemStack.EMPTY).mode() == BenchAction.Mode.NONE, "one slab is too little");
        check(h, bench(h, new ItemStack(Items.IRON_BLOCK), ItemStack.EMPTY).mode() == BenchAction.Mode.NONE, "iron blocks already uncraft");
        check(h, bench(h, new ItemStack(Items.IRON_INGOT, 64), ItemStack.EMPTY).mode() == BenchAction.Mode.NONE, "ingots already uncraft");
        check(h, bench(h, new ItemStack(Items.ANDESITE, 64), ItemStack.EMPTY).mode() == BenchAction.Mode.NONE, "andesite is two materials");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void upgradeAndTransfer(GameTestHelper h) {
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        pick.enchant(h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FORTUNE), 3);
        BenchAction short2 = bench(h, pick, new ItemStack(Items.DIAMOND, 2));
        check(h, short2.mode() == BenchAction.Mode.UPGRADE && short2.missing() == 1, "2 diamonds: needs 1 more, got " + short2);
        BenchAction up = bench(h, pick, new ItemStack(Items.DIAMOND, 5));
        expect(h, "iron pickaxe + diamonds", up.result(), Items.DIAMOND_PICKAXE, 1);
        check(h, up.addUse() == 3 && up.result().isEnchanted(), "uses 3 diamonds and keeps Fortune");
        check(h, bench(h, new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.NETHERITE_INGOT)).mode() == BenchAction.Mode.NONE,
                "diamond to netherite stays the smithing table's");
        BenchAction transfer = bench(h, new ItemStack(Items.DIAMOND_PICKAXE), pick);
        check(h, transfer.mode() == BenchAction.Mode.TRANSFER && transfer.result().isEnchanted() && transfer.levels() > 0, "transfer: " + transfer);
        check(h, bench(h, new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.IRON_HELMET)).mode() == BenchAction.Mode.NONE,
                "tool and armour don't pair");

        // armour ranked by the game: total protection, toughness, knockback resistance, durability
        expect(h, "leather helmet + gold", bench(h, new ItemStack(Items.LEATHER_HELMET), new ItemStack(Items.GOLD_INGOT, 5)).result(),
                Items.GOLDEN_HELMET, 1);
        expect(h, "gold chestplate + iron", bench(h, new ItemStack(Items.GOLDEN_CHESTPLATE), new ItemStack(Items.IRON_INGOT, 8)).result(),
                Items.CHAINMAIL_CHESTPLATE, 1);
        expect(h, "chainmail boots + iron", bench(h, new ItemStack(Items.CHAINMAIL_BOOTS), new ItemStack(Items.IRON_INGOT, 4)).result(),
                Items.IRON_BOOTS, 1);
        expect(h, "wooden sword + cobblestone", bench(h, new ItemStack(Items.WOODEN_SWORD), new ItemStack(Items.COBBLESTONE, 2)).result(),
                Items.STONE_SWORD, 1);

        // and through the menu, as a player would
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        ReforgingMenu menu = new ReforgingMenu(1, player.getInventory());
        menu.getSlot(ReforgingMenu.BASE).set(pick.copy());
        menu.getSlot(ReforgingMenu.ADDITION).set(new ItemStack(Items.DIAMOND, 5));
        ItemStack taken = menu.getSlot(ReforgingMenu.RESULT).remove(1);
        menu.getSlot(ReforgingMenu.RESULT).onTake(player, taken);
        expect(h, "menu result", taken, Items.DIAMOND_PICKAXE, 1);
        expect(h, "diamonds left", menu.getSlot(ReforgingMenu.ADDITION).getItem(), Items.DIAMOND, 2);
        check(h, menu.getSlot(ReforgingMenu.BASE).getItem().isEmpty(), "the iron pickaxe is used up");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void blockFamilies(GameTestHelper h) {
        var rm = h.getLevel().getRecipeManager();
        var fromPlanks = rm.getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(new ItemStack(Items.OAK_PLANKS)), h.getLevel());
        check(h, fromPlanks.stream().anyMatch(r -> r.value().getResultItem(h.getLevel().registryAccess()).is(Items.OAK_FENCE)), "oak planks cut into fences");
        check(h, fromPlanks.stream().anyMatch(r -> r.value().getResultItem(h.getLevel().registryAccess()).is(Items.OAK_DOOR)), "oak planks cut into doors");
        var fromStone = rm.getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(new ItemStack(Items.STONE)), h.getLevel());
        long slabs = fromStone.stream().filter(r -> r.value().getResultItem(h.getLevel().registryAccess()).is(Items.STONE_SLAB)).count();
        check(h, slabs == 1, "stone to stone slab listed once, not " + slabs);

        expect(h, "two stone slabs side by side", craft(h, 2, 1, Items.STONE_SLAB), Items.STONE, 1);
        expect(h, "two stone brick slabs stacked", craft(h, 1, 2, Items.STONE_BRICK_SLAB), Items.CHISELED_STONE_BRICKS, 1);
        expect(h, "two stone brick slabs side by side", craft(h, 2, 1, Items.STONE_BRICK_SLAB), Items.STONE_BRICKS, 1);
        expect(h, "four oak stairs", craft(h, 2, 2, Items.OAK_STAIRS), Items.OAK_PLANKS, 3);
        h.succeed();
    }

    private static ItemStack craft(GameTestHelper h, int w, int ht, Item item) {
        List<ItemStack> grid = new java.util.ArrayList<>();
        for (int i = 0; i < w * ht; i++) {
            grid.add(new ItemStack(item));
        }
        CraftingInput in = CraftingInput.of(w, ht, grid);
        return h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, in, h.getLevel())
                .map(r -> r.value().assemble(in, h.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
    }
}
