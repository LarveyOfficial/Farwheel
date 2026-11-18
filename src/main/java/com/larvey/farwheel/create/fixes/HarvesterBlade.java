package com.larvey.farwheel.create.fixes;

import com.larvey.farwheel.create.CreateFixes;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterBlock;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.world.level.block.state.BlockState;

/** Harvester blade, Create draws it outside Flywheel */
public class HarvesterBlade {
	public static void add(InstancerProvider instancerProvider, BlockState blockState) {
		instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.HARVESTER_BLADE)).createInstance()
			.center()
			.rotateYDegrees(CreateFixes.horizontalAngle(blockState.getValue(HarvesterBlock.FACING)))
			.uncenter();
	}
}
