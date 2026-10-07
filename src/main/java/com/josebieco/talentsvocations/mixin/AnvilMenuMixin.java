package com.josebieco.talentsvocations.mixin;

import com.josebieco.talentsvocations.effect.hooks.AnvilHooks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bigorna do Artífice: ajusta resultado e custo depois que a vanilla montou o resultado. Estende ItemCombinerMenu só
 * para ler o campo protegido {@code player} (o construtor nunca é mesclado).
 * <p>
 * {@code TAIL} = o último RETURN de {@code createResult}, alcançado pelo caminho vanilla completo (depois do
 * {@code broadcastChanges}) e pelo ramo de entrada vazia. Os retornos antecipados (saída do {@code AnvilUpdateEvent}
 * via {@code ForgeHooks.onAnvilChange}, reparo sem dano, combinação inválida, encantamentos incompatíveis) são outros
 * RETURN e não passam aqui — de propósito: saídas de outros mods ficam intocadas.
 * ATENÇÃO: reconferir com {@code javap -c AnvilMenu} a cada atualização (hoje o último RETURN é o alvo do goto depois
 * de {@code broadcastChanges}).
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow private boolean onlyRenaming;

    protected AnvilMenuMixin(MenuType<?> menuType, int containerId, Inventory inventory, ContainerLevelAccess access,
                             ItemCombinerMenuSlotDefinition slots) {
        super(menuType, containerId, inventory, access, slots);
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void talentsvocations$afterCreateResult(CallbackInfo ci) {
        AnvilHooks.afterCreateResult((AnvilMenu) (Object) this, this.player, this.onlyRenaming);
    }
}
