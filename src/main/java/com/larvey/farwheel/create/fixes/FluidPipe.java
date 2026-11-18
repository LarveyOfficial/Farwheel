package com.larvey.farwheel.create.fixes;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes.ComponentPartials;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Fluid pipe arms and casing, Create builds them from the blocks around the pipe */
public class FluidPipe {
	public static void add(InstancerProvider instancerProvider, BlockState blockState) {
		Direction.Axis straightAxis = FluidPropagator.getStraightPipeAxis(blockState);
		for (Direction side : Direction.values()) {
			if (FluidPipeBlock.isOpenAt(blockState, side)) {
				ComponentPartials arm = side.getAxis() == straightAxis ? ComponentPartials.CONNECTION : ComponentPartials.RIM_CONNECTOR;
				addPart(instancerProvider, AllPartialModels.PIPE_ATTACHMENTS.get(arm).get(side));
			}
		}
		// Create only looks at the state for this
		if (FluidPipeBlock.shouldDrawCasing(null, null, blockState)) {
			addPart(instancerProvider, AllPartialModels.FLUID_PIPE_CASING);
		}
	}

	private static void addPart(InstancerProvider instancerProvider, PartialModel model) {
		instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(model)).createInstance();
	}
}
