package com.kragnoth.workbenchwisdom.client;

import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import com.kragnoth.workbenchwisdom.bench.ReforgingMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The Reforging Bench: item, plus what's added, gives the result; under it, what it costs, takes or gives back. */
public class ReforgingScreen extends ItemCombinerScreen<ReforgingMenu> {
    private static final ResourceLocation TEXTURE = WorkbenchWisdom.id("textures/gui/reforging_bench.png");
    private static final ResourceLocation ERROR = ResourceLocation.withDefaultNamespace("container/smithing/error");
    private static final int GREEN = 0x80FF20;
    private static final int RED = 0xFF6060;

    public ReforgingScreen(ReforgingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, TEXTURE);
    }

    @Override
    protected void renderErrorIcon(GuiGraphics g, int x, int y) {
        if (menu.getSlot(ReforgingMenu.BASE).hasItem() && menu.getSlot(ReforgingMenu.ADDITION).hasItem()
                && !menu.getSlot(ReforgingMenu.RESULT).hasItem()) {
            g.blitSprite(ERROR, x + 98, y + 44, 28, 21);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        Component line = null;
        int colour = GREEN;
        switch (menu.mode()) {
            case TRANSFER -> {
                int cost = menu.number();
                if (cost > 0) {
                    line = Component.translatable("container.repair.cost", cost);
                    boolean ok = minecraft.player.getAbilities().instabuild || minecraft.player.experienceLevel >= cost;
                    colour = ok ? GREEN : RED;
                }
            }
            case UPGRADE -> {
                ItemStack[] mats = menu.action().material().getItems();
                Component name = mats.length > 0 ? mats[0].getHoverName() : Component.literal("?");
                if (menu.number() > 0) {
                    line = Component.translatable("container.workbenchwisdom.needs_more", menu.number(), name);
                    colour = RED;
                } else {
                    line = Component.translatable("container.workbenchwisdom.uses", menu.action().addUse(), name);
                }
            }
            case SALVAGE -> {
                int used = menu.uses();
                line = menu.number() > 0 ? Component.translatable("container.workbenchwisdom.salvage_xp", menu.number())
                        : used > 1 ? Component.translatable("container.workbenchwisdom.salvage_uses", used)
                        : Component.translatable("container.workbenchwisdom.salvage");
            }
            case NONE -> { }
        }
        if (line != null) {
            int w = font.width(line);
            int x = imageWidth - 8 - w;
            g.fill(x - 2, 67, imageWidth - 8, 79, 0x4F000000);
            g.drawString(font, line, x, 69, colour);
        }
    }
}
