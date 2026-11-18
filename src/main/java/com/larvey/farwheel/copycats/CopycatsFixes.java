package com.larvey.farwheel.copycats;

import com.copycatsplus.copycats.foundation.copycat.ICopycatBlock;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;

/** Copycats+ blocks only get a model from their block entity's data */
public class CopycatsFixes {
	public static boolean takesOver(BlockState blockState) {
		return blockState.getBlock() instanceof ICopycatBlock;
	}

	/** Create's blank copycat texture, what an empty copycat looks like */
	@SuppressWarnings("resource")
	public static NativeImage blankTexture() {
		return Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS)
			.getSprite(ResourceLocation.fromNamespaceAndPath("create", "block/copycat_base")).contents().getOriginalImage();
	}
}
