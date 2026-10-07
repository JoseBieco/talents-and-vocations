package com.seunome.vanillatalents.effect.loot;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.Optional;

/** Saída de fornalha de um item, como a função de loot {@code smelt} da vanilla (respeita receitas de datapack). */
public final class Smelting {

    private Smelting() {}

    /** A pilha fundida com a mesma quantidade (limitada ao tamanho máximo), ou vazio se não houver receita. */
    public static Optional<ItemStack> result(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        SingleRecipeInput input = new SingleRecipeInput(stack);
        return level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level)
                .map(holder -> holder.value().assemble(input))
                .filter(result -> !result.isEmpty())
                .map(result -> result.copyWithCount(Math.min(stack.getCount() * result.getCount(), result.getMaxStackSize())));
    }
}
