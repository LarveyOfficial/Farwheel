package com.larvey.farwheel;

import com.seibel.distanthorizons.api.enums.rendering.EDhApiDirection;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuadMerger {
	public enum Level {
		ORIGINAL,
		SAFE,
		AGGRESSIVE
	}

	private static final int SAFE_MIN_QUADS = 60;
	private static final int AGGRESSIVE_MIN_QUADS = 15;
	private static final float MAX_PAINTED_SIZE = 4;

	private record Group(TextureAtlasSprite sprite, int color, EDhApiDirection cullSide, int normalX, int normalY, int normalZ, int distance) {}

	private record Corner(Vector3f position, Vector2f uv) {}

	public static List<Quad> merge(List<Quad> quads, Level level) {
		int minQuads = level == Level.AGGRESSIVE ? AGGRESSIVE_MIN_QUADS : SAFE_MIN_QUADS;
		if (level == Level.ORIGINAL || quads.size() <= minQuads) {
			return quads;
		}
		if (level == Level.AGGRESSIVE) {
			List<Quad> merged = mergeWith(quads, true);
			if (merged.size() >= minQuads) {
				return merged;
			}
		}
		List<Quad> exact = mergeWith(quads, false);
		return exact.size() >= minQuads ? exact : quads;
	}

	private static List<Quad> mergeWith(List<Quad> quads, boolean allowPainting) {
		Map<Group, List<Quad>> groups = new LinkedHashMap<>();
		for (Quad quad : quads) {
			groups.computeIfAbsent(groupOf(quad, allowPainting), group -> new ArrayList<>()).add(quad);
		}
		List<Quad> merged = new ArrayList<>();
		for (List<Quad> group : groups.values()) {
			merged.addAll(mergeGroup(group, allowPainting));
		}
		return merged;
	}

	private static Group groupOf(Quad quad, boolean allowPainting) {
		Vector3f normal = quad.normal().normalize();
		float distance = normal.dot(quad.corners()[0]);
		TextureAtlasSprite sprite = allowPainting ? null : quad.sprite();
		int color = allowPainting ? 0 : quad.color();
		return new Group(sprite, color, quad.cullSide(), Math.round(normal.x * 1000), Math.round(normal.y * 1000), Math.round(normal.z * 1000), Math.round(distance * 1000));
	}

	private static List<Quad> mergeGroup(List<Quad> group, boolean allowPainting) {
		List<Quad> quads = new ArrayList<>(group);
		boolean joinedAny = true;
		while (joinedAny) {
			joinedAny = false;
			for (int i = 0; i < quads.size() && !joinedAny; i++) {
				for (int j = i + 1; j < quads.size() && !joinedAny; j++) {
					Quad joined = join(quads.get(i), quads.get(j), allowPainting);
					if (joined != null) {
						quads.set(i, joined);
						quads.remove(j);
						joinedAny = true;
					}
				}
			}
		}
		return quads;
	}

	private static Quad join(Quad first, Quad second, boolean allowPainting) {
		List<Corner> firstCorners = distinctCorners(first);
		List<Corner> secondCorners = distinctCorners(second);
		if (firstCorners.size() < 3 || secondCorners.size() < 3) {
			return null;
		}
		Quad joined = joinAlongEdge(first, firstCorners, second, secondCorners, allowPainting);
		if (joined == null && firstCorners.size() == 3 && secondCorners.size() == 3) {
			joined = joinTrianglesAcrossDiagonal(first, firstCorners, second, secondCorners, allowPainting);
		}
		return joined;
	}

	private static Quad joinAlongEdge(Quad first, List<Corner> firstCorners, Quad second, List<Corner> secondCorners, boolean allowPainting) {
		for (int i = 0; i < firstCorners.size(); i++) {
			Corner start = firstCorners.get(i);
			Corner end = firstCorners.get((i + 1) % firstCorners.size());
			for (int j = 0; j < secondCorners.size(); j++) {
				Corner otherStart = secondCorners.get(j);
				Corner otherEnd = secondCorners.get((j + 1) % secondCorners.size());
				if (samePosition(otherStart, end) && samePosition(otherEnd, start)) {
					List<Corner> outline = new ArrayList<>();
					for (int k = 1; k <= firstCorners.size(); k++) {
						outline.add(firstCorners.get((i + k) % firstCorners.size()));
					}
					for (int k = 2; k < secondCorners.size(); k++) {
						outline.add(secondCorners.get((j + k) % secondCorners.size()));
					}
					removeIfOnStraightEdge(outline, start);
					removeIfOnStraightEdge(outline, end);
					if (outline.size() < 3 || outline.size() > 4 || !isConvex(outline)) {
						return null;
					}
					boolean seamless = sameUv(otherStart, end) && sameUv(otherEnd, start);
					if (canKeepTexture(first, second, seamless)) {
						Quad exact = toQuad(outline, first, List.of());
						if (textureContinues(exact, first) && textureContinues(exact, second) && fitsOneTile(exact)) {
							return exact;
						}
					}
					return paintedIfAllowed(outline, first, second, allowPainting);
				}
			}
		}
		return null;
	}

	private static Quad joinTrianglesAcrossDiagonal(Quad first, List<Corner> firstCorners, Quad second, List<Corner> secondCorners, boolean allowPainting) {
		for (int i = 0; i < 3; i++) {
			Corner start = firstCorners.get(i);
			Corner end = firstCorners.get((i + 1) % 3);
			Corner opposite = firstCorners.get((i + 2) % 3);
			for (int j = 0; j < 3; j++) {
				Corner otherStart = secondCorners.get(j);
				Corner otherEnd = secondCorners.get((j + 1) % 3);
				if (samePosition(otherStart, end) && samePosition(otherEnd, start)) {
					List<Corner> outline = List.of(end, opposite, start, secondCorners.get((j + 2) % 3));
					boolean seamless = sameUv(otherStart, end) && sameUv(otherEnd, start);
					if (canKeepTexture(first, second, seamless)) {
						Quad exact = toQuad(outline, first, List.of());
						if (fitsOneTile(exact)) {
							return exact;
						}
					}
					return paintedIfAllowed(outline, first, second, allowPainting);
				}
			}
		}
		return null;
	}

	private static boolean canKeepTexture(Quad first, Quad second, boolean seamless) {
		return seamless && !first.isPainted() && !second.isPainted() && first.sprite() == second.sprite() && first.color() == second.color();
	}

	private static Quad paintedIfAllowed(List<Corner> outline, Quad first, Quad second, boolean allowPainting) {
		if (!allowPainting) {
			return null;
		}
		List<Quad> originals = new ArrayList<>(first.originals());
		originals.addAll(second.originals());
		Quad painted = toQuad(outline, first, originals);
		return fitsPaintedSize(painted) ? painted : null;
	}

	private static List<Corner> distinctCorners(Quad quad) {
		List<Corner> corners = new ArrayList<>();
		for (int corner = 0; corner < 4; corner++) {
			Vector3f position = quad.corners()[corner];
			if (corners.isEmpty() || corners.getLast().position().distanceSquared(position) > 1e-10f) {
				corners.add(new Corner(position, quad.uvs()[corner]));
			}
		}
		if (corners.size() > 3 && corners.getFirst().position().distanceSquared(corners.getLast().position()) < 1e-10f) {
			corners.removeLast();
		}
		return corners;
	}

	private static Quad toQuad(List<Corner> outline, Quad style, List<Quad> paintedFrom) {
		Vector3f[] corners = new Vector3f[4];
		Vector2f[] uvs = new Vector2f[4];
		for (int corner = 0; corner < 4; corner++) {
			Corner source = outline.get(Math.min(corner, outline.size() - 1));
			corners[corner] = source.position();
			uvs[corner] = source.uv();
		}
		return new Quad(corners, uvs, style.sprite(), style.color(), style.cullSide(), paintedFrom);
	}

	private static boolean samePosition(Corner first, Corner second) {
		return first.position().distanceSquared(second.position()) < 1e-10f;
	}

	private static boolean sameUv(Corner first, Corner second) {
		return first.uv().distanceSquared(second.uv()) < 1e-10f;
	}

	private static void removeIfOnStraightEdge(List<Corner> outline, Corner corner) {
		int index = outline.indexOf(corner);
		Corner previous = outline.get((index + outline.size() - 1) % outline.size());
		Corner next = outline.get((index + 1) % outline.size());
		if (isBetween(previous.position(), corner.position(), next.position())) {
			outline.remove(index);
		}
	}

	private static boolean isBetween(Vector3f from, Vector3f point, Vector3f to) {
		Vector3f line = new Vector3f(to).sub(from);
		Vector3f toPoint = new Vector3f(point).sub(from);
		float along = toPoint.dot(line) / line.lengthSquared();
		return along > 0 && along < 1 && new Vector3f(line).mul(along).distance(toPoint) < 0.0001f;
	}

	private static boolean isConvex(List<Corner> outline) {
		if (outline.size() == 3) {
			return true;
		}
		Vector3f normal = new Vector3f(outline.get(2).position()).sub(outline.get(0).position())
			.cross(new Vector3f(outline.get(3).position()).sub(outline.get(1).position()));
		for (int corner = 0; corner < 4; corner++) {
			Vector3f current = outline.get(corner).position();
			Vector3f toNext = new Vector3f(outline.get((corner + 1) % 4).position()).sub(current);
			Vector3f toPrevious = new Vector3f(outline.get((corner + 3) % 4).position()).sub(current);
			if (toNext.cross(toPrevious).dot(normal) <= 0) {
				return false;
			}
		}
		return true;
	}

	private static boolean textureContinues(Quad joined, Quad part) {
		Vector3f origin = joined.corners()[0];
		Vector3f sideA = new Vector3f(joined.corners()[1]).sub(origin);
		Vector3f sideB = new Vector3f(joined.corners()[3]).sub(origin);
		Vector3f normal = new Vector3f(sideA).cross(sideB);
		Vector2f uvAlongA = new Vector2f(joined.uvs()[1]).sub(joined.uvs()[0]);
		Vector2f uvAlongB = new Vector2f(joined.uvs()[3]).sub(joined.uvs()[0]);
		for (int corner = 0; corner < 4; corner++) {
			Vector3f offset = new Vector3f(part.corners()[corner]).sub(origin);
			float stepsAlongA = new Vector3f(offset).cross(sideB).dot(normal) / normal.lengthSquared();
			float stepsAlongB = new Vector3f(sideA).cross(offset).dot(normal) / normal.lengthSquared();
			Vector2f expectedUv = new Vector2f(joined.uvs()[0])
				.add(new Vector2f(uvAlongA).mul(stepsAlongA))
				.add(new Vector2f(uvAlongB).mul(stepsAlongB));
			if (expectedUv.distance(part.uvs()[corner]) > 0.001f) {
				return false;
			}
		}
		return true;
	}

	private static boolean fitsOneTile(Quad quad) {
		Vector2f min = new Vector2f(quad.uvs()[0]);
		Vector2f max = new Vector2f(quad.uvs()[0]);
		for (Vector2f uv : quad.uvs()) {
			min.min(uv);
			max.max(uv);
		}
		return (max.x - min.x) * quad.sprite().contents().width() <= 16.01f
			&& (max.y - min.y) * quad.sprite().contents().height() <= 16.01f;
	}

	private static boolean fitsPaintedSize(Quad quad) {
		for (int corner = 0; corner < 4; corner++) {
			if (quad.corners()[corner].distance(quad.corners()[(corner + 1) % 4]) > MAX_PAINTED_SIZE + 0.001f) {
				return false;
			}
		}
		return true;
	}
}
