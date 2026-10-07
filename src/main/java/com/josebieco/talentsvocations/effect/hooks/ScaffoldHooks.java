package com.josebieco.talentsvocations.effect.hooks;

import com.josebieco.talentsvocations.core.formula.BuilderFormulas;
import com.josebieco.talentsvocations.effect.Talents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * builder_scaffold (descida), chamado pelo LivingEntityMixin no retorno de {@code LivingEntity.handleOnClimbable}.
 * Descendo o andaime (agachado: o andaime perde a colisão), a vanilla prende o y em −0,15 nesse método antes do move,
 * a cada tick; multiplicar aqui, depois do prende, acelera a descida sem acumular (o próximo tick prende de novo).
 * {@code handleOnClimbable} roda em todo tick de travel de toda LivingEntity, nos dois lados (o mixin é comum), então
 * os testes baratos vêm primeiro e só o jogador local ({@link Player#isLocalPlayer()}, sem referência a classes do
 * cliente: o servidor dedicado carrega esta classe) subindo/descendo algo escalável passa. A subida fica em
 * ClientEffects.onScaffoldTick.
 */
public final class ScaffoldHooks {

    private ScaffoldHooks() {}

    public static Vec3 climbDelta(LivingEntity entity, Vec3 delta) {
        if (delta.y >= 0) return delta;
        if (!(entity instanceof Player player) || !player.isLocalPlayer() || player.getAbilities().flying) return delta;
        if (!player.onClimbable()) return delta;
        if (!player.getInBlockState().isScaffolding(player)) return delta;
        int level = Talents.level(player, "builder_scaffold");
        if (level <= 0) return delta;
        double factor = BuilderFormulas.scaffoldFactor(level, Talents.value(player, "builder_scaffold", "per_level"));
        return new Vec3(delta.x, delta.y * factor, delta.z);
    }
}
