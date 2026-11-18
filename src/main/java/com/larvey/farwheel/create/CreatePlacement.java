package com.larvey.farwheel.create;

import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.processing.burner.ScrollInstance;
import com.simibubi.create.content.processing.burner.ScrollTransformedInstance;
import dev.engine_room.flywheel.api.instance.Instance;
import org.joml.Matrix4f;
import org.joml.Vector2f;

public class CreatePlacement {
	public static Matrix4f matrix(Instance instance) {
		if (instance instanceof RotatingInstance rotating) {
			return new Matrix4f()
				.translate(rotating.x + 0.5f, rotating.y + 0.5f, rotating.z + 0.5f)
				.rotate(rotating.rotation)
				.translate(-0.5f, -0.5f, -0.5f);
		}
		if (instance instanceof ScrollInstance scroll) {
			return new Matrix4f()
				.translate(scroll.x + 0.5f, scroll.y + 0.5f, scroll.z + 0.5f)
				.rotate(scroll.rotation)
				.translate(-0.5f, -0.5f, -0.5f);
		}
		return null;
	}

	public static Vector2f uvShift(Instance instance) {
		if (instance instanceof ScrollInstance scroll) {
			return new Vector2f(scroll.diffU, scroll.diffV);
		}
		if (instance instanceof ScrollTransformedInstance scroll) {
			return new Vector2f(scroll.diffU, scroll.diffV);
		}
		return new Vector2f();
	}
}
