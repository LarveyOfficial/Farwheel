package com.larvey.farwheel.create.fixes;

import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import java.util.List;

/** Spout nozzle, Create draws it outside Flywheel */
public class SpoutNozzle {
	public static void add(InstancerProvider instancerProvider) {
		for (PartialModel bit : List.of(AllPartialModels.SPOUT_TOP, AllPartialModels.SPOUT_MIDDLE, AllPartialModels.SPOUT_BOTTOM)) {
			instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(bit)).createInstance();
		}
	}
}
