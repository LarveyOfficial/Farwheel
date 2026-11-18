package com.larvey.farwheel;

import com.larvey.farwheel.create.CreateFixes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.List;

/** Fakes a Flywheel context so we can grab a visual's instances */
public class VisualRecorder {
	public final List<Instance> instances = new ArrayList<>();
	public final List<Model> models = new ArrayList<>();

	/** Builds the visual and saves every instance and its model */
	public VisualRecorder(BlockEntity blockEntity) {
		VisualizationContext context = new VisualizationContext() {
			@Override
			public InstancerProvider instancerProvider() {
				return new InstancerProvider() {
					@Override
					public <I extends Instance> Instancer<I> instancer(InstanceType<I> type, Model model, int bias) {
						return new Instancer<I>() {
							@Override
							public I createInstance() {
								// Static models never update, so the handle does nothing
								I instance = type.create(new InstanceHandle() {
									@Override public void setChanged() {}
									@Override public void setDeleted() {}
									@Override public void setVisible(boolean visible) {}
									@Override public boolean isVisible() { return true; }
								});
								instances.add(instance);
								models.add(model);
								return instance;
							}

							@Override
							public void stealInstance(I instance) {}
						};
					}
				};
			}

			// Keeps positions relative to the block
			@Override
			public Vec3i renderOrigin() {
				return blockEntity.getBlockPos();
			}

			// Only contraptions use this
			@Override
			public VisualEmbedding createEmbedding(Vec3i renderOrigin) {
				return null;
			}
		};
		@SuppressWarnings("unchecked")
		BlockEntityVisualizer<BlockEntity> visualizer = (BlockEntityVisualizer<BlockEntity>) VisualizerRegistry.getVisualizer(blockEntity.getType());
		if (visualizer == null) {
			return;
		}
		// DH builds on multiple threads, so one visual at a time
		synchronized (VisualRecorder.class) {
			visualizer.createVisual(context, blockEntity, 0);
		}
		// Keeps Create optional
		if (ModList.get().isLoaded("create")) {
			CreateFixes.addParts(context.instancerProvider(), blockEntity.getBlockState());
		}
	}
}
