package com.larvey.farwheel;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiDirection;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

/** One quad, plus the originals if it got painted */
public record Quad(Vector3f[] corners, Vector2f[] uvs, TextureAtlasSprite sprite, int color, EDhApiDirection cullSide, List<Quad> paintedFrom) {
	/** Normal quad, not painted */
	public Quad(Vector3f[] corners, Vector2f[] uvs, TextureAtlasSprite sprite, int color, EDhApiDirection cullSide) {
		this(corners, uvs, sprite, color, cullSide, List.of());
	}

	/** Not normalized */
	public Vector3f normal() {
		return new Vector3f(corners[2]).sub(corners[0]).cross(new Vector3f(corners[3]).sub(corners[1]));
	}

	/** Joined across different textures */
	public boolean isPainted() {
		return !paintedFrom.isEmpty();
	}

	/** What this quad was painted from, or itself */
	public List<Quad> originals() {
		return isPainted() ? paintedFrom : List.of(this);
	}
}
