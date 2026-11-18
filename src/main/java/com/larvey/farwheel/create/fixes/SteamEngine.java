package com.larvey.farwheel.create.fixes;

import com.larvey.farwheel.create.CreateFixes;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Steam engine piston and linkage, Create only draws them when a shaft is attached */
public class SteamEngine {
	/** Adds the parts with the crank held still, placed like Create's SteamEngineVisual */
	public static void add(InstancerProvider instancerProvider, BlockState blockState) {
		Direction facing = SteamEngineBlock.getFacing(blockState);
		// The state doesn't say where the shaft is, so guess it runs across the engine
		Direction.Axis shaftAxis = blockState.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise().getAxis();
		boolean roll90 = facing.getAxis().isHorizontal() && shaftAxis == Direction.Axis.Y || facing.getAxis().isVertical() && shaftAxis == Direction.Axis.Z;
		// Create's crank math at angle 0
		float piston = -Mth.sqrt(Mth.square(14 / 16f) - Mth.square(6 / 16f));
		float linkageAngle = (float) Math.acos(-piston / (14 / 16f));

		part(instancerProvider, AllPartialModels.ENGINE_PISTON, facing, roll90)
			.translate(0, piston + 20 / 16f, 0);
		part(instancerProvider, AllPartialModels.ENGINE_LINKAGE, facing, roll90)
			.center()
			.translate(0, 1, 0)
			.uncenter()
			.translate(0, piston + 20 / 16f, 0)
			.translate(0, 4 / 16f, 8 / 16f)
			.rotateX(linkageAngle)
			.translate(0, -4 / 16f, -8 / 16f);
		part(instancerProvider, AllPartialModels.ENGINE_CONNECTOR, facing, roll90)
			.translate(0, 2, 0)
			.center()
			.rotateX(-Mth.HALF_PI)
			.uncenter();
	}

	/** One part, turned to point out of the tank */
	private static TransformedInstance part(InstancerProvider instancerProvider, PartialModel model, Direction facing, boolean roll90) {
		float verticalAngle = facing == Direction.UP ? -90 : facing == Direction.DOWN ? 90 : 0;
		return instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(model)).createInstance()
			.center()
			.rotateYDegrees(CreateFixes.horizontalAngle(facing))
			.rotateXDegrees(verticalAngle + 90)
			.rotateYDegrees(roll90 ? -90 : 0)
			.uncenter();
	}
}
