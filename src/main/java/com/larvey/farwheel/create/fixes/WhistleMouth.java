package com.larvey.farwheel.create.fixes;

import com.larvey.farwheel.create.CreateFixes;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.world.level.block.state.BlockState;

/** Steam whistle mouth, Create draws it outside Flywheel */
public class WhistleMouth {
	public static void add(InstancerProvider instancerProvider, BlockState blockState) {
		PartialModel mouth = switch (blockState.getValue(WhistleBlock.SIZE)) {
			case SMALL -> AllPartialModels.WHISTLE_MOUTH_SMALL;
			case MEDIUM -> AllPartialModels.WHISTLE_MOUTH_MEDIUM;
			case LARGE -> AllPartialModels.WHISTLE_MOUTH_LARGE;
		};
		instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(mouth)).createInstance()
			.center()
			.rotateYDegrees(CreateFixes.horizontalAngle(blockState.getValue(WhistleBlock.FACING)))
			.uncenter();
	}
}
