package com.seunome.vanillatalents;

import com.seunome.vanillatalents.core.CostMode;
import net.minecraftforge.common.ForgeConfigSpec;

public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.EnumValue<CostMode> COST_MODE = BUILDER
            .comment("How 1 Talent Point is paid: LEVELS (whole XP levels) or POINTS (raw XP points)")
            .defineEnum("costMode", CostMode.LEVELS);

    public static final ForgeConfigSpec.IntValue COST_LEVELS = BUILDER
            .comment("XP levels per Talent Point when costMode = LEVELS")
            .defineInRange("costLevels", 5, 1, 1000);

    public static final ForgeConfigSpec.IntValue COST_POINTS = BUILDER
            .comment("XP points per Talent Point when costMode = POINTS")
            .defineInRange("costPoints", 100, 1, 1_000_000);

    public static final ForgeConfigSpec.IntValue RESPEC_FEE_LEVELS = BUILDER
            .comment("XP levels charged to change class (the first class choice is free)")
            .defineInRange("respecFeeLevels", 10, 0, 1000);

    public static final ForgeConfigSpec.IntValue RESPEC_REFUND_PERCENT = BUILDER
            .comment("Percent of the old class's spent points returned on respec (rounded down)")
            .defineInRange("respecRefundPercent", 25, 0, 100);

    public static final ForgeConfigSpec.IntValue AURA_MAX_PER_PULSE = BUILDER
            .comment("Fertile Aura: maximum plants advanced per pulse")
            .defineInRange("auraMaxPerPulse", 32, 0, 4096);

    public static final ForgeConfigSpec.IntValue AURA_IDLE_TICKS = BUILDER
            .comment("Fertile Aura stops if the player has not moved for this many ticks")
            .defineInRange("auraIdleTicks", 1200, 0, 72000);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
