package com.seunome.vanillatalents.mixin.client;

import com.seunome.vanillatalents.client.EnchantInsightClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

/**
 * Visão Arcana: acrescenta a lista completa à dica da opção sob o mouse. {@code extractRenderState} monta a dica da
 * primeira linha {@code i} com {@code isHovering(60, 14 + 19·i, 108, 17) && levelClue[i] >= 0} e chama
 * {@code setComponentTooltipForNextFrame(font, texts, mouseX, mouseY)} uma única vez; o handler recebe os argumentos
 * da chamada e refaz o mesmo teste para achar a linha.
 */
@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {

    private EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @ModifyArg(method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"),
            index = 1)
    private List<Component> vanillatalents$insight(Font font, List<Component> texts, int mouseX, int mouseY) {
        for (int i = 0; i < 3; i++) {
            if (this.isHovering(60, 14 + 19 * i, 108, 17, mouseX, mouseY) && this.menu.levelClue[i] >= 0) {
                return EnchantInsightClient.withInsight(texts, this.menu.containerId, i);
            }
        }
        return texts;
    }
}
