package com.seunome.vanillatalents.effect.hooks;

import com.seunome.vanillatalents.core.formula.ArtisanFormulas;
import com.seunome.vanillatalents.effect.Talents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Bigorna do Artífice (artisan_repair, artisan_rename, artisan_frugal, artisan_master), chamada pelo AnvilMenuMixin no
 * {@code TAIL} de {@code AnvilMenu.createResult}: só o caminho vanilla chega lá (os retornos antecipados — saída de
 * {@code AnvilUpdateEvent} e combinações inválidas — ficam de fora). Roda nos dois lados (o cliente mostra o custo).
 * <p>
 * Economia: a vanilla já decidiu "Caro demais!" (custo ≥ 40 → resultado vazio); aqui só se mexe em resultado não
 * vazio e o custo só diminui, nunca abaixo de 1. Um resultado vazio nunca vira item.
 */
public final class AnvilHooks {

    private AnvilHooks() {}

    public static void afterCreateResult(AnvilMenu menu, Player player, boolean onlyRenaming) {
        ItemStack result = menu.getSlot(AnvilMenu.RESULT_SLOT).getItem();
        int cost = menu.getCost();
        if (result.isEmpty() || cost <= 0) return;

        int repair = Talents.level(player, "artisan_repair");
        int rename = Talents.level(player, "artisan_rename");
        int frugal = Talents.level(player, "artisan_frugal");
        int master = Talents.level(player, "artisan_master");
        if (repair <= 0 && rename <= 0 && frugal <= 0 && master <= 0) return;

        ItemStack input = menu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack addition = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        int newCost = cost;
        boolean changed = false;

        // (1) Reparo Eficiente: repairItemCountCost > 0 só no ramo vanilla de reparo com material.
        if (repair > 0 && menu.repairItemCountCost > 0 && input.isDamageableItem()) {
            ArtisanFormulas.RepairPlan plan = ArtisanFormulas.repairPlan(input.getDamageValue(), input.getMaxDamage(),
                    addition.getCount(), repair, Talents.value(player, "artisan_repair", "per_level"));
            if (plan.units() > 0 && plan.units() <= menu.repairItemCountCost) {
                int saved = menu.repairItemCountCost - plan.units();
                result.setDamageValue(plan.newDamage());
                menu.repairItemCountCost = plan.units();
                newCost = Math.max(1, newCost - saved);
                changed = true;
            }
        }

        // (2) Nome Barato: só renomear custa o "cost" do JSON (nunca mais que a vanilla).
        if (rename > 0 && onlyRenaming) {
            int renameCost = (int) Talents.value(player, "artisan_rename", "cost");
            newCost = Math.max(1, Math.min(newCost, renameCost));
        }

        // (3) Artesão Frugal.
        if (frugal > 0) {
            newCost = ArtisanFormulas.anvilCost(newCost, true, frugal, Talents.value(player, "artisan_frugal", "per_level"));
        }

        // (4) Mestre Artesão: a penalidade de trabalho guardada não dobra.
        if (master > 0 && !onlyRenaming) {
            int stored = ArtisanFormulas.masterRepairCost(input.getOrDefault(DataComponents.REPAIR_COST, 0),
                    addition.getOrDefault(DataComponents.REPAIR_COST, 0));
            if (stored != result.getOrDefault(DataComponents.REPAIR_COST, 0)) {
                result.set(DataComponents.REPAIR_COST, stored);
                changed = true;
            }
        }

        if (newCost != cost) {
            menu.setMaximumCost(newCost);
            changed = true;
        }
        if (changed) menu.broadcastChanges();
    }
}
