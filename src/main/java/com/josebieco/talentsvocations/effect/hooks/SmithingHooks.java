package com.josebieco.talentsvocations.effect.hooks;

import com.josebieco.talentsvocations.core.formula.HookFormulas;
import com.josebieco.talentsvocations.effect.NextTick;
import com.josebieco.talentsvocations.effect.Talents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/** Mesa de ferraria do Artífice (artisan_smithing). */
public final class SmithingHooks {

    private SmithingHooks() {}

    /**
     * artisan_smithing: chamado pelo SmithingMenuMixin no {@code HEAD} de {@code SmithingMenu.onTake}, antes de a vanilla
     * consumir 1 de cada entrada. Só o Molde de Aprimoramento de Netherita conta ("molde de aprimoramento" do nó; os
     * moldes de ornamento não). No sucesso, 1 molde volta no próximo tick do servidor: no slot do molde se a mesa ainda
     * está aberta e o slot está vazio (ou tem o mesmo molde com espaço), senão no inventário, senão aos pés do jogador —
     * mesma regra de devolução do Construtor. {@code onTake} roda nos dois lados; só o servidor rola.
     */
    public static void beforeTake(SmithingMenu menu, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || serverPlayer.hasInfiniteMaterials()) return;
        ItemStack template = menu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        if (template.isEmpty() || !template.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) return;
        int level = Talents.level(serverPlayer, "artisan_smithing");
        if (level <= 0) return;
        double chance = HookFormulas.chance(level, Talents.value(serverPlayer, "artisan_smithing", "per_level"));
        if (serverPlayer.getRandom().nextDouble() >= chance) return;
        giveBack(serverPlayer, menu, template.copyWithCount(1));
    }

    private static void giveBack(ServerPlayer player, SmithingMenu menu, ItemStack one) {
        MinecraftServer server = player.level().getServer();
        UUID id = player.getUUID();
        NextTick.schedule(() -> {
            ServerPlayer current = server.getPlayerList().getPlayer(id);
            if (current == null) return;
            if (current.containerMenu == menu) {
                Slot slot = menu.getSlot(SmithingMenu.TEMPLATE_SLOT);
                ItemStack inSlot = slot.getItem();
                if (inSlot.isEmpty()) {
                    slot.set(one); // setChanged → slotsChanged → novo resultado; o menu sincroniza no tick do jogador
                    return;
                }
                if (ItemStack.isSameItemSameComponents(inSlot, one) && inSlot.getCount() < inSlot.getMaxStackSize()) {
                    inSlot.grow(1);
                    slot.setChanged();
                    return;
                }
            }
            if (!current.getInventory().add(one)) current.spawnAtLocation(current.level(), one);
        });
    }
}
