package com.larvey.farwheel;

import com.larvey.farwheel.copycats.CopycatsFixes;
import com.larvey.farwheel.create.CreateFixes;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;

/** Decides which blocks we take over in DH */
public class BlockStateRules extends DhApiBlockStateWrapperCreatedEvent {
	/** DH calls this once per block state */
	@Override
	public void blockStateWrapperCreated(DhApiEventParam<EventParam> event) {
		if (!Config.ENABLED.get()) {
			return;
		}
		// Anything with a Flywheel visual
		if (event.value.getBlockStateWrapper().getWrappedMcObject() instanceof BlockState blockState
			&& blockState.getBlock() instanceof EntityBlock entityBlock
			&& entityBlock.newBlockEntity(BlockPos.ZERO, blockState) instanceof BlockEntity blockEntity
			&& VisualizerRegistry.getVisualizer(blockEntity.getType()) != null) {
			event.value.setAllowApiModelOverride(true);
		}
		// Create blocks we fix that have no visual
		if (ModList.get().isLoaded("create")
			&& event.value.getBlockStateWrapper().getWrappedMcObject() instanceof BlockState blockState
			&& CreateFixes.takesOver(blockState)) {
			event.value.setAllowApiModelOverride(true);
		}
		// Copycats+ blocks
		if (ModList.get().isLoaded("copycats")
			&& event.value.getBlockStateWrapper().getWrappedMcObject() instanceof BlockState blockState
			&& CopycatsFixes.takesOver(blockState)) {
			event.value.setAllowApiModelOverride(true);
			event.value.setAllowApiTextureOverride(true);
			event.value.setAllowApiColorOverride(true);
		}
		// Invisible helper blocks, like the water wheel's structure blocks
		if (event.value.getBlockStateWrapper().getWrappedMcObject() instanceof BlockState blockState
			&& blockState.getRenderShape() == RenderShape.INVISIBLE
			&& !blockState.hasBlockEntity()
			&& blockState.getFluidState().isEmpty()
			&& !BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).getNamespace().equals("minecraft")
			&& registersVisuals(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).getNamespace())) {
			// Hide them from DH
			event.value.setBlockMaterial(EDhApiBlockMaterial.AIR);
			event.value.setOpacity(0);
		}
	}

	/** Does this mod use Flywheel at all */
	private static boolean registersVisuals(String namespace) {
		for (BlockEntityType<?> type : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
			ResourceLocation key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
			if (key != null && key.getNamespace().equals(namespace) && VisualizerRegistry.getVisualizer(type) != null) {
				return true;
			}
		}
		return false;
	}
}
