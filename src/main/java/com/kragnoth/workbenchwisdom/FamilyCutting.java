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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;

/**
 * Fills in the stonecutter for every mod's block families: a block cuts into the stairs, slab and wall named after it
 * ({@code x} into {@code x_stairs}; {@code x_bricks} into {@code x_brick_stairs}), and planks into the whole wood set
 * ({@code oak_planks} into {@code oak_stairs}, slab, fence, fence gate, door, trapdoor, button, pressure plate, sign).
 * Only where the stonecutter doesn't already have that cut. Then {@link StonecutterTweaks} makes them one-to-one and
 * reverses them like any other.
 */
public final class FamilyCutting {
    private FamilyCutting() {}

    private static final String[] SHAPES = {"_stairs", "_slab", "_wall"};
    private static final String[] WOOD = {"_stairs", "_slab", "_fence", "_fence_gate", "_door", "_trapdoor", "_button", "_pressure_plate", "_sign"};

    static List<RecipeHolder<?>> generate(List<RecipeHolder<?>> recipes) {
        Set<String> have = new HashSet<>(); // "from>to" the stonecutter already does
        Set<ResourceLocation> ids = new HashSet<>();
        for (RecipeHolder<?> h : recipes) {
            ids.add(h.id());
            if (h.value().getType() == RecipeType.STONECUTTING && h.value() instanceof StonecutterRecipe r) {
                Item to = r.getResultItem(RegistryAccess.EMPTY).getItem();
                for (ItemStack from : r.getIngredients().get(0).getItems()) {
                    have.add(BuiltInRegistries.ITEM.getKey(from.getItem()) + ">" + BuiltInRegistries.ITEM.getKey(to));
                }
            }
        }
        List<RecipeHolder<?>> out = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            String ns = id.getNamespace();
            String path = id.getPath();
            if (StonecutterTweaks.skipped(id)) {
                continue;
            }
            List<String> stems = new ArrayList<>();
            String[] shapes = SHAPES;
            if (path.endsWith("_planks")) {
                stems.add(path.substring(0, path.length() - "_planks".length()));
                shapes = WOOD;
            } else {
                stems.add(path);
                if (path.endsWith("s")) {
                    stems.add(path.substring(0, path.length() - 1)); // stone_bricks -> stone_brick_stairs
                }
                if (path.endsWith("_block")) {
                    stems.add(path.substring(0, path.length() - "_block".length()));
                }
            }
            for (String shape : shapes) {
                for (String stem : stems) {
                    ResourceLocation targetId = ResourceLocation.fromNamespaceAndPath(ns, stem + shape);
                    Item target = BuiltInRegistries.ITEM.get(targetId);
                    if (target == Items.AIR || target == item || !have.add(id + ">" + targetId)) {
                        continue;
                    }
                    ResourceLocation rid = ResourceLocation.fromNamespaceAndPath(WorkbenchWisdom.MODID,
                            "cutting/" + ns + "/" + stem + shape + "_from_" + path);
                    if (ids.add(rid)) {
                        out.add(new RecipeHolder<>(rid, new StonecutterRecipe("", Ingredient.of(item), new ItemStack(target))));
                    }
                    break;
                }
            }
        }
        WorkbenchWisdom.LOG.info("Stonecutter: {} cuts added for block families the stonecutter didn't cover", out.size());
        return out;
    }
}
