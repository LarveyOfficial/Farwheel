package com.larvey.farwheel.copycats;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.NativeImage;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockColorOverrideEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import net.minecraft.util.FastColor;

import java.util.function.Supplier;

/** DH keeps the alpha of a block's own color, Copycats+ blocks have none so they come out nearly clear */
public class CopycatColor extends DhApiBlockColorOverrideEvent {
	// DH asks for this millions of times
	private static final Supplier<Integer> BLANK_COLOR = Suppliers.memoize(CopycatColor::averageColor);

	@Override
	public void onBlockColorOverridden(DhApiEventParam<EventParam> event) {
		int color = BLANK_COLOR.get();
		event.value.setColor(255, FastColor.ARGB32.red(color), FastColor.ARGB32.green(color), FastColor.ARGB32.blue(color));
	}

	/** Average of the solid pixels in Create's blank copycat texture */
	private static int averageColor() {
		NativeImage image = CopycatsFixes.blankTexture();
		long red = 0;
		long green = 0;
		long blue = 0;
		int count = 0;
		for (int u = 0; u < 16; u++) {
			for (int v = 0; v < 16; v++) {
				int abgr = image.getPixelRGBA(u, v);
				if (FastColor.ABGR32.alpha(abgr) > 0) {
					red += FastColor.ABGR32.red(abgr);
					green += FastColor.ABGR32.green(abgr);
					blue += FastColor.ABGR32.blue(abgr);
					count++;
				}
			}
		}
		return FastColor.ARGB32.color(255, (int) (red / count), (int) (green / count), (int) (blue / count));
	}
}
