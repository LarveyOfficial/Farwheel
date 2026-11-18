package com.larvey.farwheel.create.fixes;

import com.larvey.farwheel.create.CreateFixes;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlock;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Roller frame and drum, Create draws them outside Flywheel */
public class Roller {
	public static void add(InstancerProvider instancerProvider, BlockState blockState) {
		Direction facing = blockState.getValue(RollerBlock.FACING);
		float angle = CreateFixes.horizontalAngle(facing);

		TransformedInstance frame = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.ROLLER_FRAME)).createInstance();
		frame.center().rotateYDegrees(angle + 180).uncenter();

		TransformedInstance drum = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.ROLLER_WHEEL)).createInstance();
		drum.translate(facing.getStepX() * 17 / 16f, -0.25f, facing.getStepZ() * 17 / 16f)
			.center()
			.rotateYDegrees(angle)
			.uncenter()
			.translate(0, -0.5f, 0.5f)
			.rotateYDegrees(90);
	}
}
