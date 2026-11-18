package com.larvey.farwheel;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiDirection;
import com.seibel.distanthorizons.api.objects.render.DhApiModelQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FastColor;
import org.joml.Vector3f;

public class DhQuads {
	public static TextureAtlasSprite findSprite(TextureAtlas atlas, float u, float v) {
		TextureAtlasSprite sprite = null;
		for (TextureAtlasSprite atlasSprite : atlas.getTextures().values()) {
			if (u >= atlasSprite.getU0() && u <= atlasSprite.getU1() && v >= atlasSprite.getV0() && v <= atlasSprite.getV1()) {
				sprite = atlasSprite;
			}
		}
		return sprite;
	}

	public static EDhApiDirection cullSide(float[] x, float[] y, float[] z) {
		Vector3f normal = new Vector3f(x[2] - x[0], y[2] - y[0], z[2] - z[0]).cross(x[3] - x[1], y[3] - y[1], z[3] - z[1]);
		for (EDhApiDirection side : EDhApiDirection.values()) {
			float plane = side.x + side.y + side.z > 0 ? 1 : 0;
			boolean onSide = normal.dot(side.x, side.y, side.z) > 0;
			for (int corner = 0; corner < 4; corner++) {
				onSide &= Math.abs(x[corner] * side.x + y[corner] * side.y + z[corner] * side.z - plane) < 0.001f
					&& x[corner] > -0.001f && x[corner] < 1.001f
					&& y[corner] > -0.001f && y[corner] < 1.001f
					&& z[corner] > -0.001f && z[corner] < 1.001f;
			}
			if (onSide) {
				return side;
			}
		}
		return null;
	}

	public static DhApiModelQuad modelQuad(float[] x, float[] y, float[] z, float[] u, float[] v, TextureAtlasSprite sprite, int color, EDhApiDirection cullSide) {
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
				pixels[pixelY * 16 + pixelX] = FastColor.ARGB32.multiply(FastColor.ARGB32.color(FastColor.ABGR32.alpha(abgr), FastColor.ABGR32.red(abgr), FastColor.ABGR32.green(abgr), FastColor.ABGR32.blue(abgr)), color);
			}
		}
		return new DhApiModelQuad(x, y, z, tileU, tileV, pixels, cullSide);
	}
}
