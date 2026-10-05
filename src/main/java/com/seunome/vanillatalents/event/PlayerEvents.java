package com.seunome.vanillatalents.event;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.capability.PlayerSkillProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public class PlayerEvents {

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            if (!event.getObject().getCapability(PlayerSkillProvider.PLAYER_SKILL).isPresent()) {
                event.addCapability(new ResourceLocation(VanillaTalents.MODID, "skill_data"), new PlayerSkillProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(PlayerSkillProvider.PLAYER_SKILL).ifPresent(oldStore -> {
                event.getEntity().getCapability(PlayerSkillProvider.PLAYER_SKILL).ifPresent(newStore -> {
                    newStore.deserializeNBT(oldStore.serializeNBT());
                });
            });
        }
    }
}
