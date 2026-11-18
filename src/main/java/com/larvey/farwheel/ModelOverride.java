package com.larvey.farwheel;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiDirection;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockModelOverrideEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.objects.render.DhApiModelQuad;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.instance.ColoredLitInstance;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.vertex.FullVertexView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class ModelOverride extends DhApiBlockModelOverrideEvent {
	@Override
	public void onBlockModelOverridden(DhApiEventParam<EventParam> event) {
		BlockState blockState = (BlockState) event.value.getBlockStateWrapper().getWrappedMcObject();
		BlockEntity blockEntity = ((EntityBlock) blockState.getBlock()).newBlockEntity(new BlockPos(0, 10000, 0), blockState);
		blockEntity.setLevel(Minecraft.getInstance().level);
		VisualRecorder recorder = new VisualRecorder(blockEntity);

		List<DhApiModelQuad> modelQuads = new ArrayList<>();
		if (blockState.getRenderShape() == RenderShape.MODEL) {
			BakedModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);
			List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
			sides.add(null);
			for (Direction side : sides) {
				for (BakedQuad bakedQuad : blockModel.getQuads(blockState, side, RandomSource.create(), blockEntity.getModelData(), null)) {
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
					int tint = bakedQuad.isTinted() ? Minecraft.getInstance().getBlockColors().getColor(blockState, null, null, bakedQuad.getTintIndex()) | 0xFF000000 : -1;
					modelQuads.add(DhQuads.modelQuad(x, y, z, u, v, bakedQuad.getSprite(), tint, side == null ? DhQuads.cullSide(x, y, z) : EDhApiDirection.valueOf(side.name())));
				}
			}
		}

		for (int i = 0; i < recorder.instances.size(); i++) {
			Matrix4f placement = Placement.matrix(recorder.instances.get(i));
			if (placement == null) {
				continue;
			}
			Vector2f uvShift = Placement.uvShift(recorder.instances.get(i));
			int instanceColor = recorder.instances.get(i) instanceof ColoredLitInstance colored ? FastColor.ARGB32.color(255, Byte.toUnsignedInt(colored.red), Byte.toUnsignedInt(colored.green), Byte.toUnsignedInt(colored.blue)) : -1;
			for (Model.ConfiguredMesh configuredMesh : recorder.models.get(i).meshes()) {
				TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(configuredMesh.material().texture());
				int vertexCount = configuredMesh.mesh().vertexCount();
				MemoryBlock memory = MemoryBlock.malloc(vertexCount * FullVertexView.STRIDE);
				try {
					FullVertexView vertices = new FullVertexView();
					vertices.ptr(memory.ptr());
					vertices.vertexCount(vertexCount);
					configuredMesh.mesh().write(vertices);
					for (int quad = 0; quad < vertexCount; quad += 4) {
						float centerU = (vertices.u(quad) + vertices.u(quad + 1) + vertices.u(quad + 2) + vertices.u(quad + 3)) / 4 + uvShift.x;
						float centerV = (vertices.v(quad) + vertices.v(quad + 1) + vertices.v(quad + 2) + vertices.v(quad + 3)) / 4 + uvShift.y;
						TextureAtlasSprite sprite = DhQuads.findSprite(atlas, centerU, centerV);
						float[] x = new float[4];
						float[] y = new float[4];
						float[] z = new float[4];
						float[] u = new float[4];
						float[] v = new float[4];
						for (int corner = 0; corner < 4; corner++) {
							Vector3f position = new Vector3f(vertices.x(quad + corner), vertices.y(quad + corner), vertices.z(quad + corner)).mulPosition(placement);
							x[corner] = position.x;
							y[corner] = position.y;
							z[corner] = position.z;
							u[corner] = sprite.getUOffset(vertices.u(quad + corner) + uvShift.x);
							v[corner] = sprite.getVOffset(vertices.v(quad + corner) + uvShift.y);
						}
						int vertexColor = FastColor.ARGB32.color(255, (int) (vertices.r(quad) * 255), (int) (vertices.g(quad) * 255), (int) (vertices.b(quad) * 255));
						modelQuads.add(DhQuads.modelQuad(x, y, z, u, v, sprite, FastColor.ARGB32.multiply(vertexColor, instanceColor), DhQuads.cullSide(x, y, z)));
					}
				} finally {
					memory.free();
				}
			}
		}

		event.value.clearQuads();
		for (DhApiModelQuad modelQuad : modelQuads) {
			event.value.addQuad(modelQuad);
		}
	}
}
