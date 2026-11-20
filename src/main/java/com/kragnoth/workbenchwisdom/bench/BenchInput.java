package com.kragnoth.workbenchwisdom.bench;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** What's on the Reforging Bench: the item on the left and what's added on the right. */
public record BenchInput(ItemStack base, ItemStack addition) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? base : addition;
    }

    @Override
    public int size() {
        return 2;
    }
}
