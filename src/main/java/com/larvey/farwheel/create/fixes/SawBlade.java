package com.larvey.farwheel.create.fixes;

import com.larvey.farwheel.create.CreateFixes;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Saw blade, Create draws it outside Flywheel :( */
public class SawBlade {
	public static void add(InstancerProvider instancerProvider, BlockState blockState) {
		boolean horizontal = SawBlock.isHorizontal(blockState);
		PartialModel model = horizontal ? AllPartialModels.SAW_BLADE_HORIZONTAL_INACTIVE : AllPartialModels.SAW_BLADE_VERTICAL_INACTIVE;
		Direction facing = blockState.getValue(SawBlock.FACING);
		TransformedInstance blade = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(model)).createInstance();
		blade.center();

		if (horizontal) {
			blade.rotateYDegrees(CreateFixes.horizontalAngle(facing));
		} else {
			if (blockState.getValue(SawBlock.AXIS_ALONG_FIRST_COORDINATE)) {
				blade.rotateYDegrees(90);
			}
			blade.rotateXDegrees(facing == Direction.UP ? -90 : 90);
		}
		blade.uncenter();
	}
}
