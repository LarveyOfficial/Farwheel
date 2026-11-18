package com.larvey.farwheel.create;

import com.larvey.farwheel.Quad;
import com.larvey.farwheel.create.fixes.Roller;
import com.larvey.farwheel.create.fixes.SawBlade;
import com.larvey.farwheel.create.fixes.SlidingDoor;
import com.simibubi.create.content.contraptions.actors.roller.RollerBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Hands each Create block to the fix it needs */
public class CreateFixes {
	/** Create blocks we take over even without a visual */
	public static boolean takesOver(BlockState blockState) {
		return blockState.getBlock() instanceof RollerBlock || SlidingDoor.isOpen(blockState);
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
	}

	/** Open door halves only keep their own block's part */
	public static boolean keepsQuad(BlockState blockState, Quad quad) {
		return !SlidingDoor.isOpen(blockState) || SlidingDoor.isInOwnBlock(quad);
	}

	/** Create's angle for a horizontal facing */
	public static float horizontalAngle(Direction facing) {
		return facing.getAxis() == Direction.Axis.X ? -facing.toYRot() : facing.toYRot();
	}
}
