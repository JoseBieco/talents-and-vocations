package com.seunome.vanillatalents;

import com.mojang.logging.LogUtils;
import com.seunome.vanillatalents.client.ClientSetup;
import com.seunome.vanillatalents.data.TalentDataLoader;
import com.seunome.vanillatalents.effect.AnglerEffects;
import com.seunome.vanillatalents.effect.BuilderEffects;
import com.seunome.vanillatalents.effect.FarmerEffects;
import com.seunome.vanillatalents.effect.MinerEffects;
import com.seunome.vanillatalents.effect.loot.TalentLootModifier;
import com.seunome.vanillatalents.network.ModNetwork;
import com.seunome.vanillatalents.server.DebugCommands;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(VanillaTalents.MODID)
public final class VanillaTalents {
    public static final String MODID = "vanillatalents";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VanillaTalents(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        TalentDataLoader.register();
        TalentLootModifier.registerSerializer(context.getModBusGroup());
        ModNetwork.register();
        DebugCommands.register();
        MinerEffects.registerLoot();
        FarmerEffects.registerLoot();
        AnglerEffects.registerLoot();
        BuilderEffects.registerLoot();
        if (FMLEnvironment.dist.isClient()) ClientSetup.init();
    }
}
