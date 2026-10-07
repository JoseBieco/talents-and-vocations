package com.josebieco.talentsvocations.effect.hooks;

import com.josebieco.talentsvocations.core.formula.TamerFormulas;
import com.josebieco.talentsvocations.effect.Talents;
import com.josebieco.talentsvocations.effect.pet.PetOwnership;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;

/** Comportamento dos pets sem evento no Forge: chamado por CatRelaxOnOwnerGoalMixin. */
public final class PetHooks {

    private PetHooks() {}

    /**
     * tamer_cat_gift: chance do presente ao acordar. Só muda quando o gato é pet do {@code owner} (o dono que dormiu)
     * e ele tem o nó; senão devolve o valor vanilla.
     */
    public static float catGiftChance(Cat cat, Player owner, float vanilla) {
        if (!(owner instanceof ServerPlayer player) || !PetOwnership.isPetOf(cat, player)) return vanilla;
        int level = Talents.level(player, "tamer_cat_gift");
        if (level <= 0) return vanilla;
        return (float) TamerFormulas.catGiftChance(vanilla, level, Talents.value(player, "tamer_cat_gift", "per_level"));
    }
}
