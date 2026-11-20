package com.kragnoth.workbenchwisdom;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** What's in the Reforging Bench: the item being worked on, and what's added to it. */
public record ReforgingInput(ItemStack base, ItemStack addition) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? base : addition;
    }

    @Override
    public int size() {
        return 2;
    }
}
