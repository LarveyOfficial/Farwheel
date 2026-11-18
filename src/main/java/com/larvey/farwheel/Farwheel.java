package com.larvey.farwheel;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiBlockMaterial;
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
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.joml.Vector3f;

import java.util.ArrayList;
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
				if (event.value.getBlockStateWrapper().getWrappedMcObject() instanceof BlockState blockState
					&& blockState.getRenderShape() == RenderShape.INVISIBLE
					&& !blockState.hasBlockEntity()
					&& blockState.getFluidState().isEmpty()) {
					event.value.setBlockMaterial(EDhApiBlockMaterial.AIR);
					event.value.setOpacity(0);
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
				BakedModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);
				List<BakedQuad> bakedQuads = new ArrayList<>();
				if (blockState.getRenderShape() == RenderShape.MODEL) {
					bakedQuads.addAll(blockModel.getQuads(blockState, null, RandomSource.create(), blockEntity.getModelData(), null));
					for (Direction direction : Direction.values()) {
						bakedQuads.addAll(blockModel.getQuads(blockState, direction, RandomSource.create(), blockEntity.getModelData(), null));
					}
				}
				for (BakedQuad bakedQuad : bakedQuads) {
					float[] x = new float[4];
					float[] y = new float[4];
					float[] z = new float[4];
					float[] u = new float[4];
					float[] v = new float[4];
					for (int corner = 0; corner < 4; corner++) {
						x[corner] = Float.intBitsToFloat(bakedQuad.getVertices()[corner * 8]);
						y[corner] = Float.intBitsToFloat(bakedQuad.getVertices()[corner * 8 + 1]);
						z[corner] = Float.intBitsToFloat(bakedQuad.getVertices()[corner * 8 + 2]);
						u[corner] = bakedQuad.getSprite().getUOffset(Float.intBitsToFloat(bakedQuad.getVertices()[corner * 8 + 4]));
						v[corner] = bakedQuad.getSprite().getVOffset(Float.intBitsToFloat(bakedQuad.getVertices()[corner * 8 + 5]));
					}
					event.value.addQuad(modelQuad(x, y, z, u, v, bakedQuad.getSprite()));
				}

				for (int i = 0; i < instances.size(); i++) {
					for (Model.ConfiguredMesh configuredMesh : models.get(i).meshes()) {
						TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(configuredMesh.material().texture());
						int vertexCount = configuredMesh.mesh().vertexCount();
						MemoryBlock memory = MemoryBlock.malloc(vertexCount * FullVertexView.STRIDE);
						FullVertexView vertices = new FullVertexView();
						vertices.ptr(memory.ptr());
						vertices.vertexCount(vertexCount);
						configuredMesh.mesh().write(vertices);
						for (int quad = 0; quad < vertexCount; quad += 4) {
							float centerU = (vertices.u(quad) + vertices.u(quad + 1) + vertices.u(quad + 2) + vertices.u(quad + 3)) / 4;
							float centerV = (vertices.v(quad) + vertices.v(quad + 1) + vertices.v(quad + 2) + vertices.v(quad + 3)) / 4;
							TextureAtlasSprite sprite = null;
							for (TextureAtlasSprite atlasSprite : atlas.getTextures().values()) {
								if (centerU >= atlasSprite.getU0() && centerU <= atlasSprite.getU1() && centerV >= atlasSprite.getV0() && centerV <= atlasSprite.getV1()) {
									sprite = atlasSprite;
								}
							}
							float[] x = new float[4];
							float[] y = new float[4];
							float[] z = new float[4];
							float[] u = new float[4];
							float[] v = new float[4];
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
								u[corner] = sprite.getUOffset(vertices.u(quad + corner));
								v[corner] = sprite.getVOffset(vertices.v(quad + corner));
							}
							event.value.addQuad(modelQuad(x, y, z, u, v, sprite));
						}
						memory.free();
					}
				}
			}
		});
	}

	private static DhApiModelQuad modelQuad(float[] x, float[] y, float[] z, float[] u, float[] v, TextureAtlasSprite sprite) {
		boolean cropped = sprite.contents().width() > 16 || sprite.contents().height() > 16;
		int[] pixels = new int[256];
		for (int pixelX = 0; pixelX < 16; pixelX++) {
			for (int pixelY = 0; pixelY < 16; pixelY++) {
				float quadU = (pixelX + 0.5f) / 16;
				float quadV = (pixelY + 0.5f) / 16;
				float spriteU = quadU;
				float spriteV = quadV;
				if (cropped) {
					spriteU = (1 - quadU) * (1 - quadV) * u[0] + (1 - quadU) * quadV * u[1] + quadU * quadV * u[2] + quadU * (1 - quadV) * u[3];
					spriteV = (1 - quadU) * (1 - quadV) * v[0] + (1 - quadU) * quadV * v[1] + quadU * quadV * v[2] + quadU * (1 - quadV) * v[3];
				}
				int texelX = Math.clamp((int) (spriteU * sprite.contents().width()), 0, sprite.contents().width() - 1);
				int texelY = Math.clamp((int) (spriteV * sprite.contents().height()), 0, sprite.contents().height() - 1);
				int abgr = sprite.contents().getOriginalImage().getPixelRGBA(texelX, texelY);
				pixels[pixelY * 16 + pixelX] = FastColor.ARGB32.color(FastColor.ABGR32.alpha(abgr), FastColor.ABGR32.red(abgr), FastColor.ABGR32.green(abgr), FastColor.ABGR32.blue(abgr));
			}
		}
		return new DhApiModelQuad(x, y, z, cropped ? new float[] { 0, 0, 1, 1 } : u, cropped ? new float[] { 0, 1, 1, 0 } : v, pixels, null);
	}
}
