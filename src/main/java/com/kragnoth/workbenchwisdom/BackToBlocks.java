package com.kragnoth.workbenchwisdom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;

/**
 * Slabs and stairs back into their block at the crafting table, for every family whose slab or stairs are crafted
 * from one block: two slabs side by side make the block back, four stairs in a square make three. Neither can make
 * more than went in (three blocks make six slabs; six make four stairs). Skipped where another recipe already takes
 * that arrangement (two slabs stacked is often a chiseled block, so they go side by side).
 */
public final class BackToBlocks {
    private BackToBlocks() {}

    static List<RecipeHolder<?>> generate(List<RecipeHolder<?>> recipes) {
        List<CraftingRecipe> crafting = new ArrayList<>();
        Set<ResourceLocation> ids = new HashSet<>();
        for (RecipeHolder<?> h : recipes) {
            ids.add(h.id());
            if (h.value().getType() == RecipeType.CRAFTING && h.value() instanceof CraftingRecipe c) {
                crafting.add(c);
            }
        }
        List<RecipeHolder<?>> out = new ArrayList<>();
        Set<Item> done = new HashSet<>();
        for (CraftingRecipe c : crafting) {
            ItemStack result;
            try {
                result = c.getResultItem(RegistryAccess.EMPTY);
            } catch (RuntimeException e) {
                continue;
            }
            if (!(result.getItem() instanceof BlockItem bi) || done.contains(result.getItem())) {
                continue;
            }
            boolean slab = bi.getBlock() instanceof SlabBlock;
            boolean stairs = bi.getBlock() instanceof StairBlock;
            if (!slab && !stairs) {
                continue;
            }
            Item base = onlyIngredient(c.getIngredients());
            if (base == null || base == result.getItem()) {
                continue;
            }
            // the reverse may give back at most what crafting it took: 1 block per 2 slabs, 3 per 4 stairs
            int pieces = slab ? 2 : 4;
            int blocks = slab ? 1 : 3;
            String[] pattern = slab ? new String[] {"SS"} : new String[] {"SS", "SS"};
            CraftingInput test = CraftingInput.of(slab ? 2 : 2, slab ? 1 : 2,
                    java.util.Collections.nCopies(pieces, new ItemStack(result.getItem())));
            if (taken(crafting, test)) {
                continue;
            }
            ResourceLocation rid = ResourceLocation.fromNamespaceAndPath(WorkbenchWisdom.MODID,
                    "back_to_block/" + BuiltInRegistries.ITEM.getKey(result.getItem()).toString().replace(':', '/'));
            if (!ids.add(rid)) {
                continue;
            }
            ShapedRecipePattern p = ShapedRecipePattern.of(java.util.Map.of('S', Ingredient.of(result.getItem())), pattern);
            out.add(new RecipeHolder<>(rid, new ShapedRecipe("", CraftingBookCategory.BUILDING, p, new ItemStack(base, blocks))));
            done.add(result.getItem());
        }
        WorkbenchWisdom.LOG.info("Crafting: {} slab and stair recipes back into their blocks added", out.size());
        return out;
    }

    /** The one item every ingredient of a recipe is (three stone for stone slabs), or null if it's mixed. */
    private static Item onlyIngredient(NonNullList<Ingredient> ings) {
        Item only = null;
        for (Ingredient ing : ings) {
            if (ing.isEmpty()) {
                continue;
            }
            ItemStack[] items = ing.getItems();
            if (items.length != 1) {
                return null;
            }
            if (only == null) {
                only = items[0].getItem();
            } else if (only != items[0].getItem()) {
                return null;
            }
        }
        return only;
    }

    private static boolean taken(List<CraftingRecipe> crafting, CraftingInput input) {
        for (CraftingRecipe c : crafting) {
            try {
                if (c.matches(input, null)) {
                    return true;
                }
            } catch (RuntimeException e) {
                // a recipe that needs a level to match: assume it doesn't take this
            }
        }
        return false;
    }
}
