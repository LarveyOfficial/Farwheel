package com.larvey.farwheel.create.fixes;

import com.larvey.farwheel.Quad;
import com.larvey.farwheel.create.CreateFixes;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Open Create doors, Create draws them outside the block model */
public class SlidingDoor {
	/** Whether this is either half of an open Create door */
	public static boolean isOpen(BlockState blockState) {
		return blockState.getBlock() instanceof SlidingDoorBlock && blockState.getValue(SlidingDoorBlock.OPEN);
	}

	/** Whether a quad is part of this half's block */
	public static boolean isInOwnBlock(Quad quad) {
		float centerY = (quad.corners()[0].y + quad.corners()[2].y) / 2;
		return centerY > -0.25f && centerY < 1.25f;
	}

	/** Adds this half of the open door, placed like Create's SlidingDoorRenderer when fully open */
	public static void add(InstancerProvider instancerProvider, BlockState blockState) {
		Direction facing = blockState.getValue(SlidingDoorBlock.FACING);
		boolean rightHinge = blockState.getValue(SlidingDoorBlock.HINGE) == DoorHingeSide.RIGHT;
		boolean upperHalf = blockState.getValue(SlidingDoorBlock.HALF) == DoubleBlockHalf.UPPER;
		if (((SlidingDoorBlock) blockState.getBlock()).isFoldingDoor()) {
			addFoldedPanels(instancerProvider, blockState, facing, rightHinge, upperHalf);
		} else {
			addSlidHalf(instancerProvider, blockState, facing, rightHinge, upperHalf);
		}
	}

	/** Andesite and copper doors fold into two panels */
	private static void addFoldedPanels(InstancerProvider instancerProvider, BlockState blockState, Direction facing, boolean rightHinge, boolean upperHalf) {
		ResourceLocation door = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
		float sign = rightHinge ? -1 : 1;
		for (boolean leftPanel : new boolean[] {true, false}) {
			// Create's map for these needs catnip, so grab them by name
			PartialModel panel = PartialModel.of(door.withPrefix("block/").withSuffix(leftPanel != rightHinge ? "/fold_left" : "/fold_right"));
			TransformedInstance instance = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(panel)).createInstance();
			instance.translate(facing.getStepX() / 32f, upperHalf ? -1 - 1 / 512f : -1 / 512f, facing.getStepZ() / 32f)
				.center()
				.rotateYDegrees(CreateFixes.horizontalAngle(facing.getClockWise()))
				.uncenter();
			if (rightHinge) {
				instance.translate(0, 0, 1);
			}
			instance.rotateYDegrees(91 * sign);
			if (!leftPanel) {
				instance.translate(0, 0, sign / 2).rotateYDegrees(-181 * sign);
			}
			if (rightHinge) {
				instance.translate(0, 0, -0.5f);
			}
		}
	}

	/** Train, glass and brass doors slide to the side */
	private static void addSlidHalf(InstancerProvider instancerProvider, BlockState blockState, Direction facing, boolean rightHinge, boolean upperHalf) {
		Direction slide = rightHinge ? facing.getClockWise() : facing.getCounterClockWise();
		// Flywheel skips invisible blocks
		BlockState closed = blockState.setValue(SlidingDoorBlock.OPEN, false).setValue(SlidingDoorBlock.VISIBLE, true);
		TransformedInstance instance = instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.block(closed)).createInstance();
		float y = upperHalf ? -1 / 512f : 0;
		instance.translate(slide.getStepX() * 13 / 16f + facing.getStepX() / 32f, y, slide.getStepZ() * 13 / 16f + facing.getStepZ() / 32f);
	}
}
