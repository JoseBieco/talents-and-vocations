package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.formula.ArtisanFormulas;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Efeitos da árvore do Artífice por evento (a bigorna em si fica em effect/hooks/AnvilHooks). */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class ArtisanEffects {

    private ArtisanEffects() {}

    /**
     * artisan_anvil_care: menos chance de a bigorna se degradar. O evento dispara em AnvilMenu.onTake nos dois lados,
     * mas só o servidor degrada o bloco.
     */
    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int level = Talents.level(player, "artisan_anvil_care");
        if (level <= 0) return;
        event.setBreakChance(ArtisanFormulas.breakChance(event.getBreakChance(), level,
                Talents.value(player, "artisan_anvil_care", "per_level")));
    }
}
