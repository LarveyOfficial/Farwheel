package com.larvey.farwheel.create;

import com.larvey.farwheel.create.fixes.SawBlade;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import net.minecraft.world.level.block.state.BlockState;

/** Hands each Create block to the fix it needs */
public class CreateFixes {
	/** Adds the parts Create draws outside Flywheel */
	public static void addParts(InstancerProvider instancerProvider, BlockState blockState) {
		if (blockState.getBlock() instanceof SawBlock) {
			SawBlade.add(instancerProvider, blockState);
		}
	}
}
