package com.larvey.farwheel;

import com.seibel.distanthorizons.api.objects.render.DhApiModelQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FastColor;

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

	public static DhApiModelQuad modelQuad(float[] x, float[] y, float[] z, float[] u, float[] v, TextureAtlasSprite sprite) {
		float minU = 0;
		float maxU = 1;
		float minV = 0;
		float maxV = 1;
		if (sprite.contents().width() > 16 || sprite.contents().height() > 16) {
			minU = Math.min(Math.min(u[0], u[1]), Math.min(u[2], u[3]));
			maxU = Math.max(Math.max(u[0], u[1]), Math.max(u[2], u[3]));
			minV = Math.min(Math.min(v[0], v[1]), Math.min(v[2], v[3]));
			maxV = Math.max(Math.max(v[0], v[1]), Math.max(v[2], v[3]));
		}
		float[] tileU = new float[4];
		float[] tileV = new float[4];
		for (int corner = 0; corner < 4; corner++) {
			tileU[corner] = (u[corner] - minU) / (maxU - minU);
			tileV[corner] = (v[corner] - minV) / (maxV - minV);
		}
		int[] pixels = new int[256];
		for (int pixelX = 0; pixelX < 16; pixelX++) {
			for (int pixelY = 0; pixelY < 16; pixelY++) {
				float spriteU = minU + (pixelX + 0.5f) / 16 * (maxU - minU);
				float spriteV = minV + (pixelY + 0.5f) / 16 * (maxV - minV);
				int texelX = Math.clamp((int) (spriteU * sprite.contents().width()), 0, sprite.contents().width() - 1);
				int texelY = Math.clamp((int) (spriteV * sprite.contents().height()), 0, sprite.contents().height() - 1);
				int abgr = sprite.contents().getOriginalImage().getPixelRGBA(texelX, texelY);
				pixels[pixelY * 16 + pixelX] = FastColor.ARGB32.color(FastColor.ABGR32.alpha(abgr), FastColor.ABGR32.red(abgr), FastColor.ABGR32.green(abgr), FastColor.ABGR32.blue(abgr));
			}
		}
		return new DhApiModelQuad(x, y, z, tileU, tileV, pixels, null);
	}
}
