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

    public static final ForgeConfigSpec.DoubleValue FALL_REDUCTION_CAP = BUILDER
            .comment("Maximum fall damage reduction from Landing + Roll (0.6 = never below 40% of the damage)")
            .defineInRange("fallReductionCap", 0.6, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue CROSSBOW_MIN_TICKS = BUILDER
            .comment("Swift Reload never makes the crossbow load faster than this many ticks")
            .defineInRange("crossbowMinTicks", 8, 1, 100);

    public static final ForgeConfigSpec.BooleanValue VEIN_REQUIRES_SNEAK = BUILDER
            .comment("Vein only triggers while crouching")
            .define("veinRequiresSneak", true);

    public static final ForgeConfigSpec.DoubleValue PVP_DAMAGE_MULTIPLIER = BUILDER
            .comment("Scales Thick Hide and Steadfast when both attacker and victim are players (1.0 = full effect)")
            .defineInRange("pvpDamageMultiplier", 1.0, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue MAX_CLASSES = BUILDER
            .comment("Class slots per player: 2 enables a secondary class (after Second Vocation), 1 disables multiclass")
            .defineInRange("maxClasses", 2, 1, 2);

    public static final ForgeConfigSpec.DoubleValue COMBAT_REDUCTION_CAP = BUILDER
            .comment("Maximum combined reduction of combat damage (source is an entity) from all talents")
            .defineInRange("combatReductionCap", 0.5, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue DURABILITY_SAVE_CAP = BUILDER
            .comment("Maximum combined chance of not spending durability from all talents (before Unbreaking)")
            .defineInRange("durabilitySaveCap", 0.5, 0.0, 1.0);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private Config() {}
}
