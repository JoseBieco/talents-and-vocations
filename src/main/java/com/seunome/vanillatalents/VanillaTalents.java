package com.seunome.vanillatalents;

import com.mojang.logging.LogUtils;
import com.seunome.vanillatalents.data.TalentDataLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(VanillaTalents.MODID)
public final class VanillaTalents {
    public static final String MODID = "vanillatalents";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VanillaTalents(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        TalentDataLoader.register();
    }
}
