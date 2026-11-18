package com.larvey.farwheel;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiDirection;
import com.seibel.distanthorizons.api.objects.render.DhApiModelQuad;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FastColor;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Turns our quads into DH quads */
public class DhQuads {
	/** A point on one of an original quad's triangles */
	private record Spot(Quad original, int[] triangle, float[] weights) {}

	// How DH splits a quad
	private static final int[][] TRIANGLES = {{0, 1, 2}, {2, 3, 0}};
	// Flywheel clears this on reload
	private static final RendererReloadCache<TextureAtlas, Map<Integer, List<TextureAtlasSprite>>> SPRITE_GRIDS = new RendererReloadCache<>(DhQuads::buildSpriteGrid);

	/** Finds the sprite at an atlas UV */
	public static TextureAtlasSprite findSprite(TextureAtlas atlas, float u, float v) {
		List<TextureAtlasSprite> spritesInCell = SPRITE_GRIDS.get(atlas).getOrDefault(cellKey(cell(u), cell(v)), List.of());
		for (TextureAtlasSprite sprite : spritesInCell) {
			if (u >= sprite.getU0() && u <= sprite.getU1() && v >= sprite.getV0() && v <= sprite.getV1()) {
				return sprite;
			}
		}
		return null;
	}

	/** Grid so lookups only check a few sprites */
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

	/** Which grid cell a UV is in */
	private static int cell(float uv) {
		return (int) (uv * 256);
	}

	/** Map key for a cell */
	private static int cellKey(int cellU, int cellV) {
		return cellU * 1000 + cellV;
	}

	/** Which side the quad sits flat on, if any */
	public static EDhApiDirection cullSide(Vector3f[] corners) {
		Vector3f normal = new Vector3f(corners[2]).sub(corners[0]).cross(new Vector3f(corners[3]).sub(corners[1]));
		for (EDhApiDirection side : EDhApiDirection.values()) {
			// Positive sides are at 1
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

	/** Bakes the texture into DH's 16x16 tile */
	public static DhApiModelQuad modelQuad(Quad quad) {
		float[] x = new float[4];
		float[] y = new float[4];
		float[] z = new float[4];
		for (int corner = 0; corner < 4; corner++) {
			x[corner] = quad.corners()[corner].x;
			y[corner] = quad.corners()[corner].y;
			z[corner] = quad.corners()[corner].z;
		}
		// Painted tiles cover the whole quad
		if (quad.isPainted()) {
			float[] tileU = {0, 0, 1, 1};
			float[] tileV = {0, 1, 1, 0};
			return new DhApiModelQuad(x, y, z, tileU, tileV, paintedPixels(quad), quad.cullSide());
		}
		float[] u = new float[4];
		float[] v = new float[4];
		for (int corner = 0; corner < 4; corner++) {
			u[corner] = quad.uvs()[corner].x;
			v[corner] = quad.uvs()[corner].y;
		}
		TextureAtlasSprite sprite = quad.sprite();
		// Crop to just the part of the sprite we use
		float minU = Math.min(Math.min(u[0], u[1]), Math.min(u[2], u[3]));
		float maxU = Math.max(Math.max(u[0], u[1]), Math.max(u[2], u[3]));
		float minV = Math.min(Math.min(v[0], v[1]), Math.min(v[2], v[3]));
		float maxV = Math.max(Math.max(v[0], v[1]), Math.max(v[2], v[3]));
		float[] tileU = new float[4];
		float[] tileV = new float[4];
		for (int corner = 0; corner < 4; corner++) {
			// Don't divide by zero
			tileU[corner] = maxU > minU ? (u[corner] - minU) / (maxU - minU) : 0;
			tileV[corner] = maxV > minV ? (v[corner] - minV) / (maxV - minV) : 0;
		}
		int[] pixels = new int[256];
		for (int pixelX = 0; pixelX < 16; pixelX++) {
			for (int pixelY = 0; pixelY < 16; pixelY++) {
				float spriteU = minU + (pixelX + 0.5f) / 16 * (maxU - minU);
				float spriteV = minV + (pixelY + 0.5f) / 16 * (maxV - minV);
				pixels[pixelY * 16 + pixelX] = texelColor(sprite, spriteU, spriteV, quad.color());
			}
		}
		return new DhApiModelQuad(x, y, z, tileU, tileV, pixels, quad.cullSide());
	}

	/** Reads a texel and tints it */
	private static int texelColor(TextureAtlasSprite sprite, float u, float v, int color) {
		int texelX = Math.clamp((int) (u * sprite.contents().width()), 0, sprite.contents().width() - 1);
		int texelY = Math.clamp((int) (v * sprite.contents().height()), 0, sprite.contents().height() - 1);
		// MC is ABGR, DH wants ARGB
		int abgr = sprite.contents().getOriginalImage().getPixelRGBA(texelX, texelY);
		return FastColor.ARGB32.multiply(FastColor.ARGB32.color(FastColor.ABGR32.alpha(abgr), FastColor.ABGR32.red(abgr), FastColor.ABGR32.green(abgr), FastColor.ABGR32.blue(abgr)), color);
	}

	/** Paints the tile from the original quads */
	private static int[] paintedPixels(Quad quad) {
		int[] pixels = new int[256];
		for (int pixelX = 0; pixelX < 16; pixelX++) {
			for (int pixelY = 0; pixelY < 16; pixelY++) {
				Vector3f point = pointOnTile(quad.corners(), (pixelX + 0.5f) / 16, (pixelY + 0.5f) / 16);
				Spot spot = closestSpot(quad.paintedFrom(), point);
				pixels[pixelY * 16 + pixelX] = spot == null ? 0 : colorAt(spot);
			}
		}
		return pixels;
	}

	/** Tile spot to quad spot, split the same way DH does */
	private static Vector3f pointOnTile(Vector3f[] corners, float tileU, float tileV) {
		if (tileV >= tileU) {
			return new Vector3f(corners[0])
				.add(new Vector3f(corners[1]).sub(corners[0]).mul(tileV))
				.add(new Vector3f(corners[2]).sub(corners[1]).mul(tileU));
		}
		return new Vector3f(corners[0])
			.add(new Vector3f(corners[3]).sub(corners[0]).mul(tileU))
			.add(new Vector3f(corners[2]).sub(corners[3]).mul(tileV));
	}

	/** Which original triangle the point is most inside */
	private static Spot closestSpot(List<Quad> originals, Vector3f point) {
		Spot closest = null;
		float furthestInside = -Float.MAX_VALUE;
		for (Quad original : originals) {
			for (int[] triangle : TRIANGLES) {
				float[] weights = weightsAt(original.corners(), triangle, point);
				if (weights == null) {
					continue;
				}
				float howFarInside = Math.min(weights[0], Math.min(weights[1], weights[2]));
				if (howFarInside > furthestInside) {
					furthestInside = howFarInside;
					closest = new Spot(original, triangle, weights);
				}
			}
		}
		return closest;
	}

	/** How much of each corner the point is */
	private static float[] weightsAt(Vector3f[] corners, int[] triangle, Vector3f point) {
		Vector3f first = corners[triangle[0]];
		Vector3f toSecond = new Vector3f(corners[triangle[1]]).sub(first);
		Vector3f toThird = new Vector3f(corners[triangle[2]]).sub(first);
		Vector3f normal = new Vector3f(toSecond).cross(toThird);
		float normalLengthSquared = normal.lengthSquared();
		if (normalLengthSquared < 1e-14f) {
			return null;
		}
		Vector3f toPoint = new Vector3f(point).sub(first);
		float secondWeight = new Vector3f(toPoint).cross(toThird).dot(normal) / normalLengthSquared;
		float thirdWeight = new Vector3f(toSecond).cross(toPoint).dot(normal) / normalLengthSquared;
		return new float[] {1 - secondWeight - thirdWeight, secondWeight, thirdWeight};
	}

	/** Texture color at that spot */
	private static int colorAt(Spot spot) {
		float u = 0;
		float v = 0;
		float total = 0;
		for (int i = 0; i < 3; i++) {
			float weight = Math.max(0, spot.weights()[i]);
			Vector2f uv = spot.original().uvs()[spot.triangle()[i]];
			u += weight * uv.x;
			v += weight * uv.y;
			total += weight;
		}
		if (total > 0) {
			u /= total;
			v /= total;
		}
		return texelColor(spot.original().sprite(), u, v, spot.original().color());
	}
}
