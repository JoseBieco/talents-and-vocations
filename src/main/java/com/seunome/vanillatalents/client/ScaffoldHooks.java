package com.seunome.vanillatalents.client;

import com.seunome.vanillatalents.core.formula.BuilderFormulas;
import com.seunome.vanillatalents.effect.Talents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * builder_scaffold (descida), chamado pelo LivingEntityMixin no retorno de {@code LivingEntity.handleOnClimbable}.
 * Descendo o andaime (agachado: o andaime perde a colisão), a vanilla prende o y em −0,15 nesse método antes do move,
 * a cada tick; multiplicar aqui, depois do prende, acelera a descida sem acumular (o próximo tick prende de novo).
 * Roda nos dois lados, mas só age no jogador local ({@link Player#isLocalPlayer()}, sem referência a classes do
 * cliente, para o servidor dedicado carregar a classe sem problema). A subida fica em ClientEffects.onScaffoldTick.
 */
public final class ScaffoldHooks {

    private ScaffoldHooks() {}

    public static Vec3 climbDelta(LivingEntity entity, Vec3 delta) {
        if (delta.y >= 0) return delta;
        if (!(entity instanceof Player player) || !player.isLocalPlayer() || player.getAbilities().flying) return delta;
        if (!player.getInBlockState().isScaffolding(player)) return delta;
        int level = Talents.level(player, "builder_scaffold");
        if (level <= 0) return delta;
        double factor = BuilderFormulas.scaffoldFactor(level, Talents.value(player, "builder_scaffold", "per_level"));
        return new Vec3(delta.x, delta.y * factor, delta.z);
    }
}
