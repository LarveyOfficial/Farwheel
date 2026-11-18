package com.larvey.farwheel;

import com.larvey.farwheel.create.CreatePlacement;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import net.neoforged.fml.ModList;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Placement {
	public static void place(Instance instance, Vector3f position) {
		if (instance instanceof OrientedInstance oriented) {
			position.sub(oriented.pivotX, oriented.pivotY, oriented.pivotZ)
				.rotate(oriented.rotation)
				.add(oriented.pivotX + oriented.posX, oriented.pivotY + oriented.posY, oriented.pivotZ + oriented.posZ);
		}
		if (instance instanceof TransformedInstance transformed) {
			position.mulPosition(transformed.pose);
		}
		if (ModList.get().isLoaded("create")) {
			CreatePlacement.place(instance, position);
		}
	}

	public static Vector2f uvShift(Instance instance) {
		if (ModList.get().isLoaded("create")) {
			return CreatePlacement.uvShift(instance);
		}
		return new Vector2f();
	}
}
