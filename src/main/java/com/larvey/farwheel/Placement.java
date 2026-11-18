package com.larvey.farwheel;

import com.larvey.farwheel.create.CreatePlacement;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.instance.PosedInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.neoforged.fml.ModList;
import org.joml.Matrix4f;
import org.joml.Vector2f;

public class Placement {
	public static Matrix4f matrix(Instance instance) {
		if (instance instanceof OrientedInstance oriented) {
			return new Matrix4f()
				.translate(oriented.pivotX + oriented.posX, oriented.pivotY + oriented.posY, oriented.pivotZ + oriented.posZ)
				.rotate(oriented.rotation)
				.translate(-oriented.pivotX, -oriented.pivotY, -oriented.pivotZ);
		}
		if (instance instanceof TransformedInstance transformed) {
			return transformed.pose;
		}
		if (instance instanceof PosedInstance posed) {
			return posed.pose;
		}
		if (ModList.get().isLoaded("create")) {
			return CreatePlacement.matrix(instance);
		}
		return null;
	}

	public static Vector2f uvShift(Instance instance) {
		if (ModList.get().isLoaded("create")) {
			return CreatePlacement.uvShift(instance);
		}
		return new Vector2f();
	}
}
