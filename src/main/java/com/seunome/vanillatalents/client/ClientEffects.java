package com.seunome.vanillatalents.client;

import com.seunome.vanillatalents.core.formula.ArcherFormulas;
import com.seunome.vanillatalents.effect.Talents;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.UseEffects;
import net.minecraftforge.client.event.MovementInputUpdateEvent;

/** Efeitos do lado do cliente: Atirar Andando (o Faro Mineral fica em OreHighlights). */
public final class ClientEffects {

    private ClientEffects() {}

    static void register() {
        MovementInputUpdateEvent.BUS.addListener(ClientEffects::onMovementInput);
    }

    /** archer_mobile: devolve parte do input que a vanilla corta enquanto o arco é puxado. */
    private static void onMovementInput(MovementInputUpdateEvent event) {
        Player player = event.getEntity();
        if (!player.isUsingItem() || player.isPassenger()) return;
        ItemStack using = player.getUseItem();
        if (!(using.getItem() instanceof BowItem)) return;
        int level = Talents.level(player, "archer_mobile");
        if (level <= 0) return;
        float vanilla = using.getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT).speedMultiplier();
        if (vanilla <= 0 || vanilla >= 1) return;
        double factor = ArcherFormulas.mobileInputFactor(level, Talents.value(player, "archer_mobile", "per_level"), vanilla);
        ClientInput input = event.getInput();
        input.moveVector = input.moveVector.scale((float) factor);
    }
}
