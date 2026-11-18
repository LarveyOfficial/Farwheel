package com.larvey.farwheel.create.fixes;

import com.larvey.farwheel.Quad;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Set;

/** Fluid tanks, Create connects their textures from their neighbors */
public class FluidTank {
	private static final Set<ResourceLocation> CONNECTED_TEXTURES = Set.of(
		ResourceLocation.fromNamespaceAndPath("create", "block/fluid_tank"),
		ResourceLocation.fromNamespaceAndPath("create", "block/fluid_tank_top"),
		ResourceLocation.fromNamespaceAndPath("create", "block/fluid_tank_inner"),
		ResourceLocation.fromNamespaceAndPath("create", "block/creative_fluid_tank"),
		ResourceLocation.fromNamespaceAndPath("create", "block/creative_casing"));

	private static boolean connects(BlockState blockState, Direction side) {
		if (side == Direction.UP) {
			return !blockState.getValue(FluidTankBlock.TOP);
		}
		if (side == Direction.DOWN) {
			return !blockState.getValue(FluidTankBlock.BOTTOM);
		}
		return switch (blockState.getValue(FluidTankBlock.SHAPE)) {
			// Plain blocks don't say, so guess they're inside a bigger tank
			case PLAIN -> true;
			case WINDOW -> false;
			case WINDOW_NW -> side == Direction.EAST || side == Direction.SOUTH;
			case WINDOW_NE -> side == Direction.WEST || side == Direction.SOUTH;
			case WINDOW_SW -> side == Direction.EAST || side == Direction.NORTH;
			case WINDOW_SE -> side == Direction.WEST || side == Direction.NORTH;
		};
	}

	/** Moves the quad onto the right tile of Create's connected sheet */
	@SuppressWarnings("resource")
	public static Quad connectTexture(BlockState blockState, Quad quad) {
		ResourceLocation texture = quad.sprite().contents().name();
		if (!CONNECTED_TEXTURES.contains(texture)) {
			return quad;
		}
		TextureAtlasSprite sheet = Minecraft.getInstance().getModelManager().getAtlas(quad.sprite().atlasLocation()).getSprite(texture.withSuffix("_connected"));
		Vector3f normal = quad.normal();
		int tile = tileIndex(blockState, Direction.getNearest(normal.x, normal.y, normal.z));
		Vector2f[] uvs = new Vector2f[4];
		for (int corner = 0; corner < 4; corner++) {
			uvs[corner] = new Vector2f((tile % 4 + quad.uvs()[corner].x) / 4, (tile / 4 + quad.uvs()[corner].y) / 4);
		}
		return new Quad(quad.corners(), uvs, sheet, quad.color(), quad.cullSide());
	}

	/** Same tile rules as Create's rectangle connected textures */
	private static int tileIndex(BlockState blockState, Direction face) {
		Direction right = face.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.WEST;
		Direction up = face.getAxis().isHorizontal() ? Direction.UP : Direction.NORTH;
		if (face.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
			right = right.getOpposite();
		}
		if (face == Direction.DOWN) {
			up = up.getOpposite();
			right = right.getOpposite();
		}
		boolean joinsLeft = connects(blockState, right.getOpposite());
		boolean joinsRight = connects(blockState, right);
		boolean joinsUp = connects(blockState, up);
		boolean joinsDown = connects(blockState, up.getOpposite());
		int column = joinsLeft && joinsRight ? 2 : joinsLeft ? 3 : joinsRight ? 1 : 0;
		int row = joinsUp && joinsDown ? 1 : joinsUp ? 2 : joinsDown ? 0 : 3;
		return column + row * 4;
	}
}
