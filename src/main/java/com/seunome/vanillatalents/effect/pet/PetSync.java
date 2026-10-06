package com.seunome.vanillatalents.effect.pet;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.capability.SkillAccess;
import com.seunome.vanillatalents.core.AttributeBonuses;
import com.seunome.vanillatalents.core.PetBonuses;
import com.seunome.vanillatalents.core.PetKind;
import com.seunome.vanillatalents.data.TalentRegistries;
import com.seunome.vanillatalents.effect.NextTick;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bônus permanentes dos pets (spec §6.3): modificadores {@code vanillatalents:<nodeId>} salvos no próprio pet.
 * Reaplicados ao comprar nó / trocar de classe e no login (applyAll), ao domar e quando o pet entra no mundo com
 * o dono online. Com o dono offline o pet mantém o último valor. Idempotente: remove e recria.
 */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class PetSync {

    private PetSync() {}

    public static void apply(LivingEntity pet, ServerPlayer owner) {
        if (!PetOwnership.isPetOf(pet, owner)) return;
        Optional<PetKind> kind = PetOwnership.kind(pet);
        if (kind.isEmpty()) return;
        SkillAccess.get(owner).ifPresent(data -> {
            for (PetBonuses.Bonus bonus : PetBonuses.compute(data, TalentRegistries.server(), kind.get())) {
                AttributeInstance instance = pet.getAttribute(holder(bonus.attribute()));
                if (instance == null) continue;
                Identifier id = Identifier.fromNamespaceAndPath(VanillaTalents.MODID, bonus.nodeId());
                AttributeModifier.Operation op = operation(bonus.op());
                AttributeModifier existing = instance.getModifier(id);
                if (existing != null && existing.amount() == bonus.amount() && existing.operation() == op) continue;
                if (existing != null) instance.removeModifier(id);
                if (bonus.amount() != 0) {
                    instance.addPermanentModifier(new AttributeModifier(id, bonus.amount(), op));
                }
            }
        });
        float clamped = PetBonuses.clampedHealth(pet.getHealth(), pet.getMaxHealth());
        if (clamped != pet.getHealth()) pet.setHealth(clamped);
    }

    /** Todos os pets carregados de {@code owner}, em todos os níveis do servidor. */
    public static void applyAll(ServerPlayer owner) {
        List<LivingEntity> pets = new ArrayList<>();
        for (ServerLevel level : owner.level().getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && PetOwnership.isPetOf(living, owner)) pets.add(living);
            }
        }
        for (LivingEntity pet : pets) apply(pet, owner);
    }

    /** Pet entrando no mundo (carregado do disco ou novo) com o dono online: corrige os bônus. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity pet)) return;
        PetOwnership.onlineOwner(pet).ifPresent(owner -> apply(pet, owner));
    }

    /** O evento vem antes de o animal virar domado: aplica no tick seguinte. */
    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        Animal animal = event.getAnimal();
        if (animal.level().isClientSide() || !(event.getTamer() instanceof ServerPlayer tamer)) return;
        NextTick.schedule(() -> {
            if (animal.isAlive() && !tamer.hasDisconnected()) apply(animal, tamer);
        });
    }

    /**
     * Nautilus (AbstractNautilus.tryToTame chama tame() direto) não dispara AnimalTameEvent: depois de interagir com
     * um TamableAnimal ainda não domado, confere no tick seguinte se virou pet do jogador.
     */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof TamableAnimal animal) || animal.isTame()) return;
        NextTick.schedule(() -> {
            if (animal.isAlive() && !player.hasDisconnected() && PetOwnership.isPetOf(animal, player)) apply(animal, player);
        });
    }

    private static Holder<Attribute> holder(PetBonuses.Attr attr) {
        return switch (attr) {
            case MAX_HEALTH -> Attributes.MAX_HEALTH;
            case ATTACK_DAMAGE -> Attributes.ATTACK_DAMAGE;
            case JUMP_STRENGTH -> Attributes.JUMP_STRENGTH;
        };
    }

    private static AttributeModifier.Operation operation(AttributeBonuses.Op op) {
        return switch (op) {
            case ADD_VALUE -> AttributeModifier.Operation.ADD_VALUE;
            case ADD_MULTIPLIED_BASE -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_TOTAL -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        };
    }
}
