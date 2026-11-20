package com.kragnoth.workbenchwisdom.compat.jei;

import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
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

/** Examples of an enchantment transfer: any tool onto any tool, any armour onto any armour, for experience. */
class TransferCategory implements IRecipeCategory<WorkbenchWisdomJei.TransferExample> {
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable plus;
    private final IDrawable arrow;

    TransferCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK));
        slot = gui.getSlotDrawable();
        plus = gui.getRecipePlusSign();
        arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<WorkbenchWisdomJei.TransferExample> getRecipeType() {
        return WorkbenchWisdomJei.TRANSFERS;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.workbenchwisdom.transfer");
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
        return 40;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder b, WorkbenchWisdomJei.TransferExample e, IFocusGroup focuses) {
        b.addSlot(RecipeIngredientRole.INPUT, 1, 1).addItemStack(e.base()).setBackground(slot, -1, -1);
        b.addSlot(RecipeIngredientRole.INPUT, 40, 1).addItemStack(e.donor()).setBackground(slot, -1, -1);
        b.addSlot(RecipeIngredientRole.OUTPUT, 98, 1).addItemStack(e.result()).setBackground(slot, -1, -1);
    }

    @Override
    public void draw(WorkbenchWisdomJei.TransferExample e, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        plus.draw(g, 22, 3);
        arrow.draw(g, 64, 1);
        var font = Minecraft.getInstance().font;
        g.drawString(font, Component.translatable("container.repair.cost", e.cost()), 1, 21, 0xFF80FF20, true);
        g.drawString(font, Component.translatable("jei.workbenchwisdom.transfer_note"), 1, 31, 0xFF808080, false);
    }
}
