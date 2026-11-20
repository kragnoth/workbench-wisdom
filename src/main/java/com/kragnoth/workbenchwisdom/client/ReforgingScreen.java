package com.kragnoth.workbenchwisdom.client;

import com.kragnoth.workbenchwisdom.ReforgingMenu;
import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** The Reforging Bench: item, plus what's added, gives the result; under it, what it costs (or what's missing). */
public class ReforgingScreen extends ItemCombinerScreen<ReforgingMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(WorkbenchWisdom.MODID, "textures/gui/reforging_bench.png");
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
        if (menu.mode() == ReforgingMenu.TRANSFER) {
            int cost = menu.cost();
            boolean creative = minecraft.player.getAbilities().instabuild;
            if (cost > 0) {
                line = Component.translatable("container.repair.cost", cost);
                colour = creative || minecraft.player.experienceLevel >= cost ? GREEN : RED;
            }
        } else if (menu.mode() == ReforgingMenu.SALVAGE) {
            line = Component.translatable(menu.cost() > 0 ? "container.workbenchwisdom.salvage_xp" : "container.workbenchwisdom.salvage", menu.cost());
        } else if (menu.mode() == ReforgingMenu.UPGRADE) {
            var r = menu.upgrade();
            if (r.isPresent()) {
                ItemStack[] mats = r.get().addition().getItems();
                Component name = mats.length > 0 ? mats[0].getHoverName() : Component.literal("?");
                if (menu.cost() < 0) {
                    line = Component.translatable("container.workbenchwisdom.needs_more", -menu.cost(), name);
                    colour = RED;
                } else {
                    line = Component.translatable("container.workbenchwisdom.uses", r.get().count(), name);
                }
            }
        }
        if (line != null) {
            int w = font.width(line);
            int x = imageWidth - 8 - w;
            g.fill(x - 2, 67, imageWidth - 8, 79, 0x4F000000);
            g.drawString(font, line, x, 69, colour);
        }
    }
}
