package com.larvey.farwheel;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockModelOverrideEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.objects.render.DhApiModelQuad;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

import java.util.Arrays;

@Mod(value = Farwheel.MOD_ID, dist = Dist.CLIENT)
public class Farwheel {
	public static final String MOD_ID = "farwheel";

	public Farwheel() {
		DhApi.events.bind(DhApiBlockStateWrapperCreatedEvent.class, new DhApiBlockStateWrapperCreatedEvent() {
			@Override
			public void blockStateWrapperCreated(DhApiEventParam<EventParam> event) {
				if (event.value.getBlockStateWrapper().getWrappedMcObject() instanceof BlockState blockState
					&& BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString().equals("create:mechanical_press")) {
					event.value.setAllowApiModelOverride(true);
				}
			}
		});

		DhApi.events.bind(DhApiBlockModelOverrideEvent.class, new DhApiBlockModelOverrideEvent() {
			@Override
			public void onBlockModelOverridden(DhApiEventParam<EventParam> event) {
				int[] pixels = new int[256];
				Arrays.fill(pixels, 0xFFFF0000);
				event.value.clearQuads();
				event.value.addQuad(new DhApiModelQuad(new float[] { 1, 1, 1, 1 }, new float[] { 1, 0, 0, 1 }, new float[] { 1, 1, 0, 0 }, new float[] { 0, 0, 1, 1 }, new float[] { 0, 1, 1, 0 }, pixels, null));
				event.value.addQuad(new DhApiModelQuad(new float[] { 0, 0, 0, 0 }, new float[] { 1, 0, 0, 1 }, new float[] { 0, 0, 1, 1 }, new float[] { 0, 0, 1, 1 }, new float[] { 0, 1, 1, 0 }, pixels, null));
				event.value.addQuad(new DhApiModelQuad(new float[] { 0, 0, 1, 1 }, new float[] { 1, 1, 1, 1 }, new float[] { 0, 1, 1, 0 }, new float[] { 0, 0, 1, 1 }, new float[] { 0, 1, 1, 0 }, pixels, null));
				event.value.addQuad(new DhApiModelQuad(new float[] { 0, 0, 1, 1 }, new float[] { 0, 0, 0, 0 }, new float[] { 1, 0, 0, 1 }, new float[] { 0, 0, 1, 1 }, new float[] { 0, 1, 1, 0 }, pixels, null));
				event.value.addQuad(new DhApiModelQuad(new float[] { 0, 0, 1, 1 }, new float[] { 1, 0, 0, 1 }, new float[] { 1, 1, 1, 1 }, new float[] { 0, 0, 1, 1 }, new float[] { 0, 1, 1, 0 }, pixels, null));
				event.value.addQuad(new DhApiModelQuad(new float[] { 1, 1, 0, 0 }, new float[] { 1, 0, 0, 1 }, new float[] { 0, 0, 0, 0 }, new float[] { 0, 0, 1, 1 }, new float[] { 0, 1, 1, 0 }, pixels, null));
			}
		});
	}
}
