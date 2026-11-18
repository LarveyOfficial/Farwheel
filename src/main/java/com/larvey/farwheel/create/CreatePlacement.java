package com.larvey.farwheel.create;

import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.processing.burner.ScrollInstance;
import com.simibubi.create.content.processing.burner.ScrollTransformedInstance;
import dev.engine_room.flywheel.api.instance.Instance;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class CreatePlacement {
	public static void place(Instance instance, Vector3f position) {
		if (instance instanceof RotatingInstance rotating) {
			position.sub(0.5f, 0.5f, 0.5f)
				.rotate(rotating.rotation)
				.add(rotating.x + 0.5f, rotating.y + 0.5f, rotating.z + 0.5f);
		}
		if (instance instanceof ScrollInstance scroll) {
			position.sub(0.5f, 0.5f, 0.5f)
				.rotate(scroll.rotation)
				.add(scroll.x + 0.5f, scroll.y + 0.5f, scroll.z + 0.5f);
		}
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
