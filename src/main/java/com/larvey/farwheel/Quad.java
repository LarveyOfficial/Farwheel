package com.larvey.farwheel;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiDirection;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Vector2f;
import org.joml.Vector3f;

public record Quad(Vector3f[] corners, Vector2f[] uvs, TextureAtlasSprite sprite, int color, EDhApiDirection cullSide) {
	public Vector3f normal() {
		return new Vector3f(corners[2]).sub(corners[0]).cross(new Vector3f(corners[3]).sub(corners[1]));
	}
}
