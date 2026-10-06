package com.seunome.vanillatalents.event;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.capability.PlayerSkillProvider;
import com.seunome.vanillatalents.capability.SkillAccess;
import com.seunome.vanillatalents.core.AttributeBonuses;
import com.seunome.vanillatalents.core.TalentNode;
import com.seunome.vanillatalents.data.TalentRegistries;
import com.seunome.vanillatalents.effect.ActivityTracker;
import com.seunome.vanillatalents.effect.ArcherEffects;
import com.seunome.vanillatalents.effect.AttributeSync;
import com.seunome.vanillatalents.effect.FarmerEffects;
import com.seunome.vanillatalents.effect.StillTracker;
import com.seunome.vanillatalents.effect.WarriorEffects;
import com.seunome.vanillatalents.network.ModNetwork;
import com.seunome.vanillatalents.network.S2CSyncDefinitions;
import com.seunome.vanillatalents.server.TalentActions;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

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

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        refresh(player);
        player.setHealth(AttributeBonuses.respawnHealth(event.isEndConquered(), player.getHealth(), player.getMaxHealth()));
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        AttributeSync.forget(event.getEntity().getUUID());
        StillTracker.forget(event.getEntity().getUUID());
        FarmerEffects.forget(event.getEntity().getUUID());
        WarriorEffects.forget(event.getEntity().getUUID());
        ArcherEffects.forget(event.getEntity().getUUID());
        ActivityTracker.forget(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (event.player() instanceof ServerPlayer player) AttributeSync.tick(player);
    }

    /** Login e /reload: envia as definições de nós e reaplica atributos (maxLevel pode ter mudado). */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        List<TalentNode> nodes = List.copyOf(TalentRegistries.server().all());
        for (ServerPlayer player : event.getPlayers()) {
            ModNetwork.sendTo(player, new S2CSyncDefinitions(nodes));
            AttributeSync.apply(player);
        }
    }

    private static void refresh(ServerPlayer player) {
        AttributeSync.apply(player);
        TalentActions.sync(player);
    }
}
