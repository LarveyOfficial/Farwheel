package com.larvey.farwheel;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
	public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec.BooleanValue ENABLED = BUILDER.define("enabled", true);
	public static final ModConfigSpec.EnumValue<MergeLevel> MODEL_MERGING = BUILDER.defineEnum("modelMerging", MergeLevel.SAFE);
	public static final ModConfigSpec SPEC = BUILDER.build();
}
