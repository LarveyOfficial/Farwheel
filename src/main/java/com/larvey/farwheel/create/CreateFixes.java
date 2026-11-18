package com.larvey.farwheel.create;

import com.larvey.farwheel.Quad;
import com.larvey.farwheel.create.fixes.FluidTank;
import com.larvey.farwheel.create.fixes.Roller;
import com.larvey.farwheel.create.fixes.SawBlade;
import com.larvey.farwheel.create.fixes.SlidingDoor;
import com.larvey.farwheel.create.fixes.SteamEngine;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.trains.track.TrackBlock;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Hands each Create block to the fix it needs */
public class CreateFixes {
	/** Create blocks we take over even without a visual */
	public static boolean takesOver(BlockState blockState) {
		return blockState.getBlock() instanceof RollerBlock || blockState.getBlock() instanceof TrackBlock || blockState.getBlock() instanceof FluidTankBlock || SlidingDoor.isOpen(blockState);
	}

	/** Adds the parts Create draws outside Flywheel */
	public static void addParts(InstancerProvider instancerProvider, BlockState blockState) {
		if (blockState.getBlock() instanceof SawBlock) {
			SawBlade.add(instancerProvider, blockState);
		}
		if (blockState.getBlock() instanceof RollerBlock) {
			Roller.add(instancerProvider, blockState);
		}
		if (blockState.getBlock() instanceof SlidingDoorBlock) {
			SlidingDoor.add(instancerProvider, blockState);
		}
		if (blockState.getBlock() instanceof SteamEngineBlock) {
			SteamEngine.add(instancerProvider, blockState);
		}
	}

	/** Open door halves only keep their own block's part */
	public static boolean keepsQuad(BlockState blockState, Quad quad) {
		return !SlidingDoor.isOpen(blockState) || SlidingDoor.isInOwnBlock(quad);
	}

	/** Swaps in the textures Create connects */
	public static Quad fixQuad(BlockState blockState, Quad quad) {
		return blockState.getBlock() instanceof FluidTankBlock ? FluidTank.connectTexture(blockState, quad) : quad;
	}

	/** Create's angle for a horizontal facing */
	public static float horizontalAngle(Direction facing) {
		if (facing.getAxis().isVertical()) {
			return 0;
		}
		return facing.getAxis() == Direction.Axis.X ? -facing.toYRot() : facing.toYRot();
	}
}
