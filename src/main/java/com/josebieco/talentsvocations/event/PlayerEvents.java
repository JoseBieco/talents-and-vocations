package com.josebieco.talentsvocations.event;

import com.josebieco.talentsvocations.TalentsVocations;
import com.josebieco.talentsvocations.capability.PlayerSkillProvider;
import com.josebieco.talentsvocations.capability.SkillAccess;
import com.josebieco.talentsvocations.core.AttributeBonuses;
import com.josebieco.talentsvocations.core.TalentNode;
import com.josebieco.talentsvocations.data.TalentRegistries;
import com.josebieco.talentsvocations.effect.ActivityTracker;
import com.josebieco.talentsvocations.effect.ArcherEffects;
import com.josebieco.talentsvocations.effect.AttributeSync;
import com.josebieco.talentsvocations.effect.BuilderEffects;
import com.josebieco.talentsvocations.effect.FarmerEffects;
import com.josebieco.talentsvocations.effect.StillTracker;
import com.josebieco.talentsvocations.effect.WarriorEffects;
import com.josebieco.talentsvocations.effect.pet.GolemBuilders;
import com.josebieco.talentsvocations.effect.pet.PetScan;
import com.josebieco.talentsvocations.effect.pet.PetSync;
import com.josebieco.talentsvocations.network.ModNetwork;
import com.josebieco.talentsvocations.network.S2CSyncDefinitions;
import com.josebieco.talentsvocations.server.TalentActions;
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

@Mod.EventBusSubscriber(modid = TalentsVocations.MODID)
public class PlayerEvents {

    private static final Identifier SKILL_DATA = Identifier.fromNamespaceAndPath(TalentsVocations.MODID, "skill_data");

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
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        refresh(player);
        PetSync.applyAll(player);
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
        GolemBuilders.forget(event.getEntity().getUUID());
        PetScan.forget(event.getEntity().getUUID());
        BuilderEffects.forget(event.getEntity().getUUID());
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
            if (event.getPlayer() == null) PetSync.applyAll(player);
        }
    }

    private static void refresh(ServerPlayer player) {
        AttributeSync.apply(player);
        TalentActions.sync(player);
    }
}
