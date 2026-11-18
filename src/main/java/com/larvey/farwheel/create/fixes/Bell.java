package com.larvey.farwheel.create.fixes;

import com.larvey.farwheel.create.CreateFixes;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.equipment.bell.HauntedBellBlock;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;

/** Peculiar and cursed bells, Create draws the bell itself outside Flywheel */
public class Bell {
	public static void add(InstancerProvider instancerProvider, BlockState blockState) {
		PartialModel bell = blockState.getBlock() instanceof HauntedBellBlock ? AllPartialModels.HAUNTED_BELL : AllPartialModels.PECULIAR_BELL;
		BellAttachType attachment = blockState.getValue(BellBlock.ATTACHMENT);
		float angle = CreateFixes.horizontalAngle(blockState.getValue(BellBlock.FACING));
		if (attachment == BellAttachType.SINGLE_WALL || attachment == BellAttachType.DOUBLE_WALL) {
			angle += 90;
		}
		instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(bell)).createInstance()
			.center()
			.rotateYDegrees(angle)
			.uncenter();
	}
}
