package com.kragnoth.workbenchwisdom.compat.jei;

import com.kragnoth.workbenchwisdom.ReforgingRecipe;
import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Item + so many of the next tier's material -> the upgraded item (enchantments kept). */
class UpgradeCategory implements IRecipeCategory<RecipeHolder<ReforgingRecipe>> {
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable plus;
    private final IDrawable arrow;

    UpgradeCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(WorkbenchWisdom.REFORGING_BENCH_ITEM.get()));
        slot = gui.getSlotDrawable();
        plus = gui.getRecipePlusSign();
        arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<RecipeHolder<ReforgingRecipe>> getRecipeType() {
        return WorkbenchWisdomJei.UPGRADES;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.workbenchwisdom.upgrade");
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
    public void setRecipe(IRecipeLayoutBuilder b, RecipeHolder<ReforgingRecipe> holder, IFocusGroup focuses) {
        ReforgingRecipe r = holder.value();
        b.addSlot(RecipeIngredientRole.INPUT, 1, 1).addIngredients(r.base()).setBackground(slot, -1, -1);
        List<ItemStack> mats = new ArrayList<>();
        for (ItemStack s : r.addition().getItems()) {
            mats.add(s.copyWithCount(r.count()));
        }
        b.addSlot(RecipeIngredientRole.INPUT, 40, 1).addItemStacks(mats).setBackground(slot, -1, -1);
        b.addSlot(RecipeIngredientRole.OUTPUT, 98, 1).addItemStack(r.result()).setBackground(slot, -1, -1);
    }

    @Override
    public void draw(RecipeHolder<ReforgingRecipe> holder, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        plus.draw(g, 22, 3);
        arrow.draw(g, 64, 1);
        g.drawString(net.minecraft.client.Minecraft.getInstance().font, Component.translatable("jei.workbenchwisdom.keeps"), 1, 21, 0xFF808080, false);
    }
}
