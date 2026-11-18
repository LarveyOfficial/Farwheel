package com.larvey.farwheel;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiDirection;
import com.seibel.distanthorizons.api.objects.render.DhApiModelQuad;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FastColor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DhQuads {
	private static final RendererReloadCache<TextureAtlas, Map<Integer, List<TextureAtlasSprite>>> SPRITE_GRIDS = new RendererReloadCache<>(DhQuads::buildSpriteGrid);

	public static TextureAtlasSprite findSprite(TextureAtlas atlas, float u, float v) {
		List<TextureAtlasSprite> spritesInCell = SPRITE_GRIDS.get(atlas).getOrDefault(cellKey(cell(u), cell(v)), List.of());
		for (TextureAtlasSprite sprite : spritesInCell) {
			if (u >= sprite.getU0() && u <= sprite.getU1() && v >= sprite.getV0() && v <= sprite.getV1()) {
				return sprite;
			}
		}
		return null;
	}

	private static Map<Integer, List<TextureAtlasSprite>> buildSpriteGrid(TextureAtlas atlas) {
		Map<Integer, List<TextureAtlasSprite>> spriteGrid = new HashMap<>();
		for (TextureAtlasSprite sprite : atlas.getTextures().values()) {
			for (int cellU = cell(sprite.getU0()); cellU <= cell(sprite.getU1()); cellU++) {
				for (int cellV = cell(sprite.getV0()); cellV <= cell(sprite.getV1()); cellV++) {
					spriteGrid.computeIfAbsent(cellKey(cellU, cellV), key -> new ArrayList<>()).add(sprite);
				}
			}
		}
		return spriteGrid;
	}

	private static int cell(float uv) {
		return (int) (uv * 256);
	}

	private static int cellKey(int cellU, int cellV) {
		return cellU * 1000 + cellV;
	}

	public static EDhApiDirection cullSide(Vector3f[] corners) {
		Vector3f normal = new Vector3f(corners[2]).sub(corners[0]).cross(new Vector3f(corners[3]).sub(corners[1]));
		for (EDhApiDirection side : EDhApiDirection.values()) {
			float plane = side.x + side.y + side.z > 0 ? 1 : 0;
			boolean onSide = normal.dot(side.x, side.y, side.z) > 0;
			for (Vector3f corner : corners) {
				onSide &= Math.abs(corner.dot(side.x, side.y, side.z) - plane) < 0.001f
					&& corner.x > -0.001f && corner.x < 1.001f
					&& corner.y > -0.001f && corner.y < 1.001f
					&& corner.z > -0.001f && corner.z < 1.001f;
			}
			if (onSide) {
				return side;
			}
		}
		return null;
	}

	public static DhApiModelQuad modelQuad(Quad quad) {
		float[] x = new float[4];
		float[] y = new float[4];
		float[] z = new float[4];
		float[] u = new float[4];
		float[] v = new float[4];
		for (int corner = 0; corner < 4; corner++) {
			x[corner] = quad.corners()[corner].x;
			y[corner] = quad.corners()[corner].y;
			z[corner] = quad.corners()[corner].z;
			u[corner] = quad.uvs()[corner].x;
			v[corner] = quad.uvs()[corner].y;
		}
		TextureAtlasSprite sprite = quad.sprite();
		float minU = Math.min(Math.min(u[0], u[1]), Math.min(u[2], u[3]));
		float maxU = Math.max(Math.max(u[0], u[1]), Math.max(u[2], u[3]));
		float minV = Math.min(Math.min(v[0], v[1]), Math.min(v[2], v[3]));
		float maxV = Math.max(Math.max(v[0], v[1]), Math.max(v[2], v[3]));
		float[] tileU = new float[4];
		float[] tileV = new float[4];
		for (int corner = 0; corner < 4; corner++) {
			tileU[corner] = maxU > minU ? (u[corner] - minU) / (maxU - minU) : 0;
			tileV[corner] = maxV > minV ? (v[corner] - minV) / (maxV - minV) : 0;
		}
		int[] pixels = new int[256];
		for (int pixelX = 0; pixelX < 16; pixelX++) {
			for (int pixelY = 0; pixelY < 16; pixelY++) {
				float spriteU = minU + (pixelX + 0.5f) / 16 * (maxU - minU);
				float spriteV = minV + (pixelY + 0.5f) / 16 * (maxV - minV);
				int texelX = Math.clamp((int) (spriteU * sprite.contents().width()), 0, sprite.contents().width() - 1);
				int texelY = Math.clamp((int) (spriteV * sprite.contents().height()), 0, sprite.contents().height() - 1);
				int abgr = sprite.contents().getOriginalImage().getPixelRGBA(texelX, texelY);
				pixels[pixelY * 16 + pixelX] = FastColor.ARGB32.multiply(FastColor.ARGB32.color(FastColor.ABGR32.alpha(abgr), FastColor.ABGR32.red(abgr), FastColor.ABGR32.green(abgr), FastColor.ABGR32.blue(abgr)), quad.color());
			}
		}
		return new DhApiModelQuad(x, y, z, tileU, tileV, pixels, quad.cullSide());
	}
}
