package com.larvey.farwheel;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockModelOverrideEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockStateWrapperCreatedEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.objects.render.DhApiModelQuad;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
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
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.vertex.FullVertexView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mod(value = Farwheel.MOD_ID, dist = Dist.CLIENT)
public class Farwheel {
	public static final String MOD_ID = "farwheel";

	public Farwheel() {
		DhApi.events.bind(DhApiBlockStateWrapperCreatedEvent.class, new DhApiBlockStateWrapperCreatedEvent() {
			@Override
			public void blockStateWrapperCreated(DhApiEventParam<EventParam> event) {
				if (event.value.getBlockStateWrapper().getWrappedMcObject() instanceof BlockState blockState
					&& blockState.getBlock() instanceof EntityBlock entityBlock
					&& entityBlock.newBlockEntity(BlockPos.ZERO, blockState) instanceof BlockEntity blockEntity
					&& VisualizerRegistry.getVisualizer(blockEntity.getType()) != null) {
					event.value.setAllowApiModelOverride(true);
				}
			}
		});

		DhApi.events.bind(DhApiBlockModelOverrideEvent.class, new DhApiBlockModelOverrideEvent() {
			@Override
			public void onBlockModelOverridden(DhApiEventParam<EventParam> event) {
				BlockState blockState = (BlockState) event.value.getBlockStateWrapper().getWrappedMcObject();
				BlockEntity blockEntity = ((EntityBlock) blockState.getBlock()).newBlockEntity(BlockPos.ZERO, blockState);
				List<Instance> instances = new ArrayList<>();
				List<Model> models = new ArrayList<>();
				VisualizationContext context = new VisualizationContext() {
					@Override
					public InstancerProvider instancerProvider() {
						return new InstancerProvider() {
							@Override
							public <I extends Instance> Instancer<I> instancer(InstanceType<I> type, Model model, int bias) {
								return new Instancer<I>() {
									@Override
									public I createInstance() {
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

					@Override
					public Vec3i renderOrigin() {
						return Vec3i.ZERO;
					}

					@Override
					public VisualEmbedding createEmbedding(Vec3i renderOrigin) {
						return null;
					}
				};
				((BlockEntityVisualizer<BlockEntity>) VisualizerRegistry.getVisualizer(blockEntity.getType())).createVisual(context, blockEntity, 0);

				event.value.clearQuads();
				int[] blockPixels = new int[256];
				Arrays.fill(blockPixels, 0xFF0000FF);
				BakedModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);
				List<BakedQuad> bakedQuads = new ArrayList<>(blockModel.getQuads(blockState, null, RandomSource.create(), blockEntity.getModelData(), null));
				for (Direction direction : Direction.values()) {
					bakedQuads.addAll(blockModel.getQuads(blockState, direction, RandomSource.create(), blockEntity.getModelData(), null));
				}
				for (BakedQuad bakedQuad : bakedQuads) {
					float[] x = new float[4];
					float[] y = new float[4];
					float[] z = new float[4];
					for (int corner = 0; corner < 4; corner++) {
						x[corner] = Float.intBitsToFloat(bakedQuad.getVertices()[corner * 8]);
						y[corner] = Float.intBitsToFloat(bakedQuad.getVertices()[corner * 8 + 1]);
						z[corner] = Float.intBitsToFloat(bakedQuad.getVertices()[corner * 8 + 2]);
					}
					event.value.addQuad(new DhApiModelQuad(x, y, z, new float[] { 0, 0, 1, 1 }, new float[] { 0, 1, 1, 0 }, blockPixels, null));
				}

				int[] pixels = new int[256];
				Arrays.fill(pixels, 0xFFFF0000);
				for (int i = 0; i < instances.size(); i++) {
					for (Model.ConfiguredMesh configuredMesh : models.get(i).meshes()) {
						int vertexCount = configuredMesh.mesh().vertexCount();
						MemoryBlock memory = MemoryBlock.malloc(vertexCount * FullVertexView.STRIDE);
						FullVertexView vertices = new FullVertexView();
						vertices.ptr(memory.ptr());
						vertices.vertexCount(vertexCount);
						configuredMesh.mesh().write(vertices);
						for (int quad = 0; quad < vertexCount; quad += 4) {
							float[] x = new float[4];
							float[] y = new float[4];
							float[] z = new float[4];
							for (int corner = 0; corner < 4; corner++) {
								Vector3f position = new Vector3f(vertices.x(quad + corner), vertices.y(quad + corner), vertices.z(quad + corner));
								if (instances.get(i) instanceof OrientedInstance oriented) {
									position.sub(oriented.pivotX, oriented.pivotY, oriented.pivotZ)
										.rotate(oriented.rotation)
										.add(oriented.pivotX + oriented.posX, oriented.pivotY + oriented.posY, oriented.pivotZ + oriented.posZ);
								}
								if (instances.get(i) instanceof RotatingInstance rotating) {
									position.sub(0.5f, 0.5f, 0.5f)
										.rotate(rotating.rotation)
										.add(rotating.x + 0.5f, rotating.y + 0.5f, rotating.z + 0.5f);
								}
								x[corner] = position.x;
								y[corner] = position.y;
								z[corner] = position.z;
							}
							event.value.addQuad(new DhApiModelQuad(x, y, z, new float[] { 0, 0, 1, 1 }, new float[] { 0, 1, 1, 0 }, pixels, null));
						}
						memory.free();
					}
				}
			}
		});
	}
}
