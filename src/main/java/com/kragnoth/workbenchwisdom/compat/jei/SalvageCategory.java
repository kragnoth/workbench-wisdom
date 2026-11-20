package com.kragnoth.workbenchwisdom.compat.jei;

import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import com.kragnoth.workbenchwisdom.rules.BenchRules;
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

/**
 * An item on its own -> what it breaks down into. Gear shows one unworn piece; blocks show the smallest stack that
 * gives anything back, and how much a full stack gives.
 */
class SalvageCategory implements IRecipeCategory<BenchRules.Salvage> {
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrow;

    SalvageCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(WorkbenchWisdom.REFORGING_BENCH_ITEM.get()));
        slot = gui.getSlotDrawable();
        arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<BenchRules.Salvage> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder b, BenchRules.Salvage s, IFocusGroup focuses) {
        int batch = s.smallestBatch();
        int back = s.gear() ? (int) Math.round(s.perItem()) : (int) Math.floor(batch * s.perItem() + 1e-9);
        b.addSlot(RecipeIngredientRole.INPUT, 1, 1).addItemStack(new ItemStack(s.item(), batch)).setBackground(slot, -1, -1);
        b.addSlot(RecipeIngredientRole.OUTPUT, 98, 1).addItemStack(s.material().copyWithCount(Math.max(1, back))).setBackground(slot, -1, -1);
    }

    @Override
    public void draw(BenchRules.Salvage s, IRecipeSlotsView view, GuiGraphics g, double mx, double my) {
        arrow.draw(g, 40, 1);
        Component note;
        if (s.gear()) {
            note = Component.translatable("jei.workbenchwisdom.salvage_note");
        } else {
            int max = new ItemStack(s.item()).getMaxStackSize();
            note = Component.translatable("jei.workbenchwisdom.salvage_stack", max, (int) Math.floor(max * s.perItem() + 1e-9));
        }
        g.drawString(Minecraft.getInstance().font, note, 1, 21, 0xFF808080, false);
    }
}
