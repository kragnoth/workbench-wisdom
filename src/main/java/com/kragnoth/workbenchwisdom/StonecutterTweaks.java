package com.kragnoth.workbenchwisdom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;

/**
 * Stonecutter changes: every recipe that makes more than one item makes exactly one (so nothing can be duplicated by
 * cutting back and forth), and each recipe gets a reverse, {@code <id>_reversed}, turning the result back into what it
 * was cut from. A recipe whose input is a tag is reversed into the tag's first item.
 *
 * <p>No reverse is added where the crafting table already makes more of the result than it takes of the input
 * (three stone make six slabs there): cutting each slab back into a block would double the stone.
 */
public final class StonecutterTweaks {
    private StonecutterTweaks() {}

    static boolean skipped(ResourceLocation id) {
        for (String s : Config.STONECUTTER_SKIP.get()) {
            if (s.equals(id.toString()) || (s.endsWith(":*") && s.substring(0, s.length() - 2).equals(id.getNamespace()))) {
                return true;
            }
        }
        return false;
    }

    /** @return the recipes with the stonecutter ones changed and the reverses added */
    static List<RecipeHolder<?>> apply(List<RecipeHolder<?>> recipes) {
        boolean reverse = Config.STONECUTTER_REVERSE.get();
        boolean oneToOne = Config.STONECUTTER_ONE_TO_ONE.get();
        if (!reverse && !oneToOne) {
            return recipes;
        }
        List<RecipeHolder<?>> out = new ArrayList<>(recipes.size());
        Set<ResourceLocation> ids = new HashSet<>();
        Set<String> pairs = new HashSet<>(); // "from>to" item pairs a stonecutter recipe already covers
        List<RecipeHolder<StonecutterRecipe>> cuts = new ArrayList<>();
        for (RecipeHolder<?> h : recipes) {
            ids.add(h.id());
            if (h.value().getType() == RecipeType.STONECUTTING && h.value() instanceof StonecutterRecipe r && !skipped(h.id())) {
                @SuppressWarnings("unchecked")
                RecipeHolder<StonecutterRecipe> cut = (RecipeHolder<StonecutterRecipe>) h;
                cuts.add(cut);
                Item to = r.getResultItem(RegistryAccess.EMPTY).getItem();
                for (ItemStack from : r.getIngredients().get(0).getItems()) {
                    pairs.add(key(from.getItem(), to));
                }
            }
        }
        int changed = 0;
        int reversed = 0;
        for (RecipeHolder<?> h : recipes) {
            if (oneToOne && h.value().getType() == RecipeType.STONECUTTING && h.value() instanceof StonecutterRecipe r && !skipped(h.id())) {
                ItemStack result = r.getResultItem(RegistryAccess.EMPTY);
                if (result.getCount() > 1) {
                    out.add(new RecipeHolder<>(h.id(), new StonecutterRecipe(r.getGroup(), r.getIngredients().get(0), result.copyWithCount(1))));
                    changed++;
                    continue;
                }
            }
            out.add(h);
        }
        int unsafe = 0;
        if (reverse) {
            for (RecipeHolder<StonecutterRecipe> h : cuts) {
                StonecutterRecipe r = h.value();
                ItemStack result = r.getResultItem(RegistryAccess.EMPTY);
                ItemStack[] from = r.getIngredients().get(0).getItems();
                if (from.length == 0 || result.isEmpty()) {
                    continue; // an empty tag (nothing to cut back into)
                }
                ItemStack back = from[0].copyWithCount(1);
                if (back.is(result.getItem()) || pairs.contains(key(result.getItem(), back.getItem()))) {
                    continue; // already a recipe that way round (or a duplicate reverse)
                }
                if (craftsMoreThanItTakes(recipes, back, result.getItem())) {
                    unsafe++;
                    continue;
                }
                pairs.add(key(result.getItem(), back.getItem()));
                ResourceLocation id = h.id().withSuffix("_reversed");
                if (!ids.add(id)) {
                    continue;
                }
                out.add(new RecipeHolder<>(id, new StonecutterRecipe(r.getGroup(), Ingredient.of(result.getItem()), back)));
                reversed++;
            }
        }
        WorkbenchWisdom.LOG.info("Stonecutter: {} recipes made one-to-one, {} reverse recipes added, {} not reversed (the crafting"
                + " table makes more of them than it takes, so cutting back would duplicate)", changed, reversed, unsafe);
        return out;
    }

    /**
     * Whether some crafting recipe makes {@code made} out of {@code from} alone at more than one per item put in (three
     * blocks into six slabs).
     */
    private static boolean craftsMoreThanItTakes(List<RecipeHolder<?>> recipes, ItemStack from, Item made) {
        for (RecipeHolder<?> h : recipes) {
            if (h.value().getType() != RecipeType.CRAFTING) {
                continue;
            }
            ItemStack result;
            try {
                result = h.value().getResultItem(RegistryAccess.EMPTY);
            } catch (RuntimeException e) {
                continue; // a recipe that needs the real registries for its result: not one of these
            }
            if (result.isEmpty() || !result.is(made)) {
                continue;
            }
            int in = 0;
            boolean onlyFrom = true;
            for (Ingredient ing : h.value().getIngredients()) {
                if (ing.isEmpty()) {
                    continue;
                }
                in++;
                if (!ing.test(from)) {
                    onlyFrom = false;
                    break;
                }
            }
            if (onlyFrom && in > 0 && result.getCount() > in) {
                return true;
            }
        }
        return false;
    }

    private static String key(Item from, Item to) {
        return BuiltInRegistries.ITEM.getKey(from) + ">" + BuiltInRegistries.ITEM.getKey(to);
    }
}
