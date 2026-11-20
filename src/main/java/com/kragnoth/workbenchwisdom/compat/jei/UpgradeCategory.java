package com.kragnoth.workbenchwisdom.compat.jei;

import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import com.kragnoth.workbenchwisdom.rules.BenchRules;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Item + so many of the next tier's material -> the upgraded item, enchantments kept. */
class UpgradeCategory implements IRecipeCategory<BenchRules.Upgrade> {
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
    public RecipeType<BenchRules.Upgrade> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder b, BenchRules.Upgrade u, IFocusGroup focuses) {
        b.addSlot(RecipeIngredientRole.INPUT, 1, 1).addItemStack(new ItemStack(u.from())).setBackground(slot, -1, -1);
        List<ItemStack> mats = new ArrayList<>();
        for (ItemStack s : u.material().getItems()) {
            mats.add(s.copyWithCount(u.count()));
        }
        b.addSlot(RecipeIngredientRole.INPUT, 40, 1).addItemStacks(mats).setBackground(slot, -1, -1);
        b.addSlot(RecipeIngredientRole.OUTPUT, 98, 1).addItemStack(new ItemStack(u.to())).setBackground(slot, -1, -1);
    }

    @Override
    public void draw(BenchRules.Upgrade u, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        plus.draw(g, 22, 3);
        arrow.draw(g, 64, 1);
        g.drawString(Minecraft.getInstance().font, Component.translatable("jei.workbenchwisdom.keeps"), 1, 21, 0xFF808080, false);
    }
}
