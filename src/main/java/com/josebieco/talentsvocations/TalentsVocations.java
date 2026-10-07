package com.josebieco.talentsvocations;

import com.mojang.logging.LogUtils;
import com.josebieco.talentsvocations.client.ClientSetup;
import com.josebieco.talentsvocations.data.TalentDataLoader;
import com.josebieco.talentsvocations.effect.AnglerEffects;
import com.josebieco.talentsvocations.effect.BuilderEffects;
import com.josebieco.talentsvocations.effect.FarmerEffects;
import com.josebieco.talentsvocations.effect.MinerEffects;
import com.josebieco.talentsvocations.effect.loot.TalentLootModifier;
import com.josebieco.talentsvocations.network.ModNetwork;
import com.josebieco.talentsvocations.server.DebugCommands;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(TalentsVocations.MODID)
public final class TalentsVocations {
    public static final String MODID = "talentsvocations";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TalentsVocations(FMLJavaModLoadingContext context) {
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
