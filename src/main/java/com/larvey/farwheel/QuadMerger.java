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
	private record Group(TextureAtlasSprite sprite, int color, EDhApiDirection cullSide, int normalX, int normalY, int normalZ, int distance) {}

	private record Corner(Vector3f position, Vector2f uv) {}

	public static List<Quad> merge(List<Quad> quads) {
		Map<Group, List<Quad>> groups = new LinkedHashMap<>();
		for (Quad quad : quads) {
			groups.computeIfAbsent(groupOf(quad), group -> new ArrayList<>()).add(quad);
		}
		List<Quad> merged = new ArrayList<>();
		for (List<Quad> group : groups.values()) {
			merged.addAll(mergeGroup(group));
		}
		return merged;
	}

	private static Group groupOf(Quad quad) {
		Vector3f normal = quad.normal().normalize();
		float distance = normal.dot(quad.corners()[0]);
		return new Group(quad.sprite(), quad.color(), quad.cullSide(), Math.round(normal.x * 1000), Math.round(normal.y * 1000), Math.round(normal.z * 1000), Math.round(distance * 1000));
	}

	private static List<Quad> mergeGroup(List<Quad> group) {
		List<Quad> quads = new ArrayList<>(group);
		boolean joinedAny = true;
		while (joinedAny) {
			joinedAny = false;
			for (int i = 0; i < quads.size() && !joinedAny; i++) {
				for (int j = i + 1; j < quads.size() && !joinedAny; j++) {
					Quad joined = join(quads.get(i), quads.get(j));
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

	private static Quad join(Quad first, Quad second) {
		List<Corner> firstCorners = distinctCorners(first);
		List<Corner> secondCorners = distinctCorners(second);
		if (firstCorners.size() < 3 || secondCorners.size() < 3) {
			return null;
		}
		Quad joined = joinWhereTextureContinues(first, firstCorners, second, secondCorners);
		if (joined == null && firstCorners.size() == 3 && secondCorners.size() == 3) {
			joined = joinTrianglesAcrossDiagonal(first, firstCorners, secondCorners);
		}
		return joined != null && fitsOneTile(joined) ? joined : null;
	}

	private static Quad joinWhereTextureContinues(Quad first, List<Corner> firstCorners, Quad second, List<Corner> secondCorners) {
		for (int i = 0; i < firstCorners.size(); i++) {
			Corner start = firstCorners.get(i);
			Corner end = firstCorners.get((i + 1) % firstCorners.size());
			for (int j = 0; j < secondCorners.size(); j++) {
				if (sameCorner(secondCorners.get(j), end) && sameCorner(secondCorners.get((j + 1) % secondCorners.size()), start)) {
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
					Quad joined = toQuad(outline, first);
					return textureContinues(joined, first) && textureContinues(joined, second) ? joined : null;
				}
			}
		}
		return null;
	}

	private static Quad joinTrianglesAcrossDiagonal(Quad first, List<Corner> firstCorners, List<Corner> secondCorners) {
		for (int i = 0; i < 3; i++) {
			Corner start = firstCorners.get(i);
			Corner end = firstCorners.get((i + 1) % 3);
			Corner opposite = firstCorners.get((i + 2) % 3);
			for (int j = 0; j < 3; j++) {
				if (sameCorner(secondCorners.get(j), end) && sameCorner(secondCorners.get((j + 1) % 3), start)) {
					Corner otherOpposite = secondCorners.get((j + 2) % 3);
					return toQuad(List.of(end, opposite, start, otherOpposite), first);
				}
			}
		}
		return null;
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

	private static Quad toQuad(List<Corner> outline, Quad style) {
		Vector3f[] corners = new Vector3f[4];
		Vector2f[] uvs = new Vector2f[4];
		for (int corner = 0; corner < 4; corner++) {
			Corner source = outline.get(Math.min(corner, outline.size() - 1));
			corners[corner] = source.position();
			uvs[corner] = source.uv();
		}
		return new Quad(corners, uvs, style.sprite(), style.color(), style.cullSide());
	}

	private static boolean sameCorner(Corner first, Corner second) {
		return first.position().distanceSquared(second.position()) < 1e-10f && first.uv().distanceSquared(second.uv()) < 1e-10f;
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
}
