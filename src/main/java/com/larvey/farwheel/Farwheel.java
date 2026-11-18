package com.larvey.farwheel;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockModelOverrideEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Farwheel.MOD_ID, dist = Dist.CLIENT)
public class Farwheel {
	public static final String MOD_ID = "farwheel";

	public Farwheel(IEventBus modBus, ModContainer container) {
		container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
		container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
		modBus.addListener((ModConfigEvent.Reloading event) -> DhApi.Delayed.renderProxy.clearRenderDataCache());
		DhApi.events.bind(DhApiBlockStateWrapperCreatedEvent.class, new BlockStateRules());
		DhApi.events.bind(DhApiBlockModelOverrideEvent.class, new ModelOverride());
	}
}
