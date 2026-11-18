package com.larvey.farwheel;

import com.larvey.farwheel.copycats.CopycatsFixes;
import com.larvey.farwheel.create.CreateFixes;
import com.seibel.distanthorizons.api.enums.rendering.EDhApiDirection;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBlockModelOverrideEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import com.seibel.distanthorizons.api.objects.render.DhApiModelQuad;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.instance.ColoredLitInstance;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.vertex.FullVertexView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Swaps DH's model for the block model plus its Flywheel parts */
public class ModelOverride extends DhApiBlockModelOverrideEvent {
	/** Builds the DH model for one block state */
	@Override
	public void onBlockModelOverridden(DhApiEventParam<EventParam> event) {
		if (!Config.ENABLED.get()) {
			return;
		}
		BlockState blockState = (BlockState) event.value.getBlockStateWrapper().getWrappedMcObject();
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			return;
		}
		// Speedometer needs a level and pos to get its quads
		BlockEntity blockEntity = ((EntityBlock) blockState.getBlock()).newBlockEntity(new BlockPos(0, 10000, 0), blockState);
		if (blockEntity != null) {
			// Speedometer needs a level and pos to get its quads
			blockEntity.setLevel(level);
		}
		ModelData modelData = blockEntity == null ? ModelData.EMPTY : blockEntity.getModelData();
		// Copycats+ only finishes its model data with a level, like the game does
		if (blockEntity != null && ModList.get().isLoaded("copycats") && CopycatsFixes.takesOver(blockState)) {
			modelData = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState).getModelData(level, blockEntity.getBlockPos(), blockState, modelData);
		}
		VisualRecorder recorder = new VisualRecorder(blockState, blockEntity);

		List<Quad> quads = new ArrayList<>();
		if (blockState.getRenderShape() == RenderShape.MODEL) {
			BakedModel blockModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);
			List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
			// Quads that aren't on a side
			sides.add(null);
			for (Direction side : sides) {
				for (BakedQuad bakedQuad : blockModel.getQuads(blockState, side, RandomSource.create(), modelData, null)) {
					Vector3f[] corners = new Vector3f[4];
					Vector2f[] uvs = new Vector2f[4];
					for (int corner = 0; corner < 4; corner++) {
						// Position is at 0, UV at 4
						int[] vertices = bakedQuad.getVertices();
						corners[corner] = new Vector3f(Float.intBitsToFloat(vertices[corner * 8]), Float.intBitsToFloat(vertices[corner * 8 + 1]), Float.intBitsToFloat(vertices[corner * 8 + 2]));
						uvs[corner] = new Vector2f(bakedQuad.getSprite().getUOffset(Float.intBitsToFloat(vertices[corner * 8 + 4])), bakedQuad.getSprite().getVOffset(Float.intBitsToFloat(vertices[corner * 8 + 5])));
					}
					int tint = bakedQuad.isTinted() ? Minecraft.getInstance().getBlockColors().getColor(blockState, null, null, bakedQuad.getTintIndex()) | 0xFF000000 : -1;
					quads.add(new Quad(corners, uvs, bakedQuad.getSprite(), tint, side == null ? DhQuads.cullSide(corners) : EDhApiDirection.valueOf(side.name())));
				}
			}
		}

		// Fallback when the texture isn't in an atlas
		TextureAtlasSprite particle = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState).getParticleIcon(modelData);
		for (int i = 0; i < recorder.instances.size(); i++) {
			if (!recorder.instances.get(i).handle().isVisible()) {
				continue;
			}
			Matrix4f placement = Placement.matrix(recorder.instances.get(i));
			if (placement == null) {
				continue;
			}
			Vector2f uvShift = Placement.uvShift(recorder.instances.get(i));
			int instanceColor = recorder.instances.get(i) instanceof ColoredLitInstance colored ? FastColor.ARGB32.color(255, Byte.toUnsignedInt(colored.red), Byte.toUnsignedInt(colored.green), Byte.toUnsignedInt(colored.blue)) : -1;
			for (Model.ConfiguredMesh configuredMesh : recorder.models.get(i).meshes()) {
				// Don't load missing textures
				@SuppressWarnings("resource")
				AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(configuredMesh.material().texture(), MissingTextureAtlasSprite.getTexture());
				TextureAtlas atlas = texture instanceof TextureAtlas textureAtlas ? textureAtlas : null;
				int vertexCount = configuredMesh.mesh().vertexCount();
				MemoryBlock memory = MemoryBlock.malloc(vertexCount * FullVertexView.STRIDE);
				try {
					FullVertexView vertices = new FullVertexView();
					vertices.ptr(memory.ptr());
					vertices.vertexCount(vertexCount);
					configuredMesh.mesh().write(vertices);
					for (int quad = 0; quad < vertexCount; quad += 4) {
						// Find the sprite from the middle of the quad
						float centerU = (vertices.u(quad) + vertices.u(quad + 1) + vertices.u(quad + 2) + vertices.u(quad + 3)) / 4 + uvShift.x;
						float centerV = (vertices.v(quad) + vertices.v(quad + 1) + vertices.v(quad + 2) + vertices.v(quad + 3)) / 4 + uvShift.y;
						TextureAtlasSprite sprite = atlas == null ? null : DhQuads.findSprite(atlas, centerU, centerV);
						Vector3f[] corners = new Vector3f[4];
						Vector2f[] uvs = new Vector2f[4];
						for (int corner = 0; corner < 4; corner++) {
							corners[corner] = new Vector3f(vertices.x(quad + corner), vertices.y(quad + corner), vertices.z(quad + corner)).mulPosition(placement);
							uvs[corner] = sprite == null
								? new Vector2f(vertices.u(quad + corner), vertices.v(quad + corner))
								: new Vector2f(sprite.getUOffset(vertices.u(quad + corner) + uvShift.x), sprite.getVOffset(vertices.v(quad + corner) + uvShift.y));
						}
						int vertexColor = FastColor.ARGB32.color(255, (int) (vertices.r(quad) * 255), (int) (vertices.g(quad) * 255), (int) (vertices.b(quad) * 255));
						quads.add(new Quad(corners, uvs, sprite == null ? particle : sprite, FastColor.ARGB32.multiply(vertexColor, instanceColor), DhQuads.cullSide(corners)));
					}
				} finally {
					memory.free();
				}
			}
		}

		if (ModList.get().isLoaded("create")) {
			quads.removeIf(quad -> !CreateFixes.keepsQuad(blockState, quad));
			quads.replaceAll(quad -> CreateFixes.fixQuad(blockState, quad));
		}

		List<DhApiModelQuad> modelQuads = new ArrayList<>();
		for (Quad quad : mergeIfAllowed(blockState, quads)) {
			modelQuads.add(DhQuads.modelQuad(quad));
		}
		// Clear last so DH keeps its model if something breaks
		event.value.clearQuads();
		for (DhApiModelQuad modelQuad : modelQuads) {
			event.value.addQuad(modelQuad);
		}
	}

	/** Never merge vanilla blocks */
	private static List<Quad> mergeIfAllowed(BlockState blockState, List<Quad> quads) {
		boolean vanillaBlock = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).getNamespace().equals("minecraft");
		return vanillaBlock ? quads : QuadMerger.merge(quads, Config.MODEL_MERGING.get());
	}
}
