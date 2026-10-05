package com.seunome.vanillatalents.event;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.capability.PlayerSkillProvider;
import com.seunome.vanillatalents.capability.SkillAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public class PlayerEvents {

    private static final Identifier SKILL_DATA = Identifier.fromNamespaceAndPath(VanillaTalents.MODID, "skill_data");

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent.Entities event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerSkillProvider.PLAYER_SKILL).isPresent()) {
                event.addCapability(SKILL_DATA, new PlayerSkillProvider());
            }
        }
    }

    /** Copia sempre: morte e saída do End criam um novo ServerPlayer. */
    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        SkillAccess.get(original).ifPresent(oldStore ->
                SkillAccess.get(event.getEntity()).ifPresent(newStore -> newStore.copyFrom(oldStore)));
        original.invalidateCaps();
    }
}
