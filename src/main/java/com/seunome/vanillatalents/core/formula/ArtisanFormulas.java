package com.seunome.vanillatalents.core.formula;

/** Fórmulas da árvore do Artífice. */
public final class ArtisanFormulas {

    private ArtisanFormulas() {}

    /**
     * artisan_frugal: custo da bigorna com {@code −lvl·per} (arredonda para baixo, mínimo 1). Sem resultado o custo
     * vanilla fica como está: o desconto nunca transforma um "Caro demais!" (resultado vazio) em item.
     */
    public static int anvilCost(int vanillaCost, boolean hasResult, int frugalLvl, double frugalPer) {
        if (!hasResult) return vanillaCost;
        return Math.max(1, (int) Math.floor(vanillaCost * (1 - frugalLvl * frugalPer)));
    }

    /** Reparo com material: unidades consumidas e dano final do item. */
    public record RepairPlan(int units, int newDamage) {}

    /**
     * artisan_repair: cada unidade restaura {@code floor(maxDamage/4 · (1 + lvl·per))} (vanilla: {@code maxDamage/4});
     * usa o mínimo de unidades que zera o dano, limitado a {@code available}.
     */
    public static RepairPlan repairPlan(int damage, int maxDamage, int available, int level, double perLevel) {
        int perUnit = (int) Math.floor(maxDamage / 4.0 * (1 + level * perLevel));
        if (perUnit <= 0 || damage <= 0 || available <= 0) return new RepairPlan(0, Math.max(0, damage));
        int needed = (damage + perUnit - 1) / perUnit;
        int units = Math.min(available, needed);
        return new RepairPlan(units, Math.max(0, damage - units * perUnit));
    }

    /**
     * artisan_master: penalidade de trabalho guardada no resultado = a maior das duas entradas, sem o
     * {@code dobro + 1} da vanilla. O custo atual continua incluindo a penalidade antiga.
     */
    public static int masterRepairCost(int inputRepairCost, int additionRepairCost) {
        return Math.max(inputRepairCost, additionRepairCost);
    }

    /** artisan_anvil_care: chance de a bigorna se degradar, {@code vanilla · (1 − lvl·per)} (mínimo 0). */
    public static float breakChance(float vanilla, int level, double perLevel) {
        return (float) Math.max(0, vanilla * (1 - level * perLevel));
    }
}
