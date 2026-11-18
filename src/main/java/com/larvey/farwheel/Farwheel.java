package com.larvey.farwheel;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockModelOverrideEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = Farwheel.MOD_ID, dist = Dist.CLIENT)
public class Farwheel {
	public static final String MOD_ID = "farwheel";

	public Farwheel() {
		DhApi.events.bind(DhApiBlockStateWrapperCreatedEvent.class, new BlockStateRules());
		DhApi.events.bind(DhApiBlockModelOverrideEvent.class, new ModelOverride());
	}
}
