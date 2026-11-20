package com.kragnoth.workbenchwisdom.compat.jei;

import com.kragnoth.workbenchwisdom.SalvageRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Gear -> what salvaging it gives back (unworn). */
class SalvageCategory implements IRecipeCategory<RecipeHolder<SalvageRecipe>> {
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    SalvageCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(Items.GRINDSTONE));
        slot = gui.getSlotDrawable();
        arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<RecipeHolder<SalvageRecipe>> getRecipeType() {
        return WorkbenchWisdomJei.SALVAGES;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.workbenchwisdom.salvage");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 116;
    }

    @Override
    public int getHeight() {
        return 30;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<SalvageRecipe> h, IFocusGroup focuses) {
        b.addSlot(RecipeIngredientRole.INPUT, 20, 1).addIngredients(h.value().input()).setBackground(slot, -1, -1);
        b.addSlot(RecipeIngredientRole.OUTPUT, 78, 1).addItemStack(h.value().result()).setBackground(slot, -1, -1);
    }

    @Override
    public void draw(RecipeHolder<SalvageRecipe> h, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        arrow.draw(g, 44, 1);
        g.drawString(Minecraft.getInstance().font, Component.translatable("jei.workbenchwisdom.salvage_note"), 1, 21, 0xFF808080, false);
    }
}
