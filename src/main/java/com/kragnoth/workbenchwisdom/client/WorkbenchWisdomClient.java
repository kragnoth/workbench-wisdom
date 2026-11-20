package com.kragnoth.workbenchwisdom.client;

import com.kragnoth.workbenchwisdom.WorkbenchWisdom;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = WorkbenchWisdom.MODID, dist = Dist.CLIENT)
public final class WorkbenchWisdomClient {
    public WorkbenchWisdomClient(IEventBus modBus, ModContainer container) {
        modBus.addListener((RegisterMenuScreensEvent e) -> e.register(WorkbenchWisdom.REFORGING_MENU.get(), ReforgingScreen::new));
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
