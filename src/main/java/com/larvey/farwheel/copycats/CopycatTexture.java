package com.larvey.farwheel.copycats;

import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockTextureOverrideEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import net.minecraft.util.FastColor;

/** Far LODs give Copycats+ blocks Create's blank copycat texture, they have no texture of their own */
public class CopycatTexture extends DhApiBlockTextureOverrideEvent {
	@Override
	public void onBlockTextureOverridden(DhApiEventParam<EventParam> event) {
		NativeImage image = CopycatsFixes.blankTexture();
		for (int u = 0; u < 16; u++) {
			for (int v = 0; v < 16; v++) {
				int abgr = image.getPixelRGBA(u, v);
				event.value.setColor(u, v, FastColor.ABGR32.alpha(abgr), FastColor.ABGR32.red(abgr), FastColor.ABGR32.green(abgr), FastColor.ABGR32.blue(abgr));
			}
		}
	}
}
