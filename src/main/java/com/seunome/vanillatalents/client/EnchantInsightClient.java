package com.seunome.vanillatalents.client;

import com.seunome.vanillatalents.network.S2CEnchantInsight;
import com.seunome.vanillatalents.network.S2CEnchantInsight.EnchantLine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Visão Arcana no cliente: guarda o último {@link S2CEnchantInsight} do menu aberto e monta as linhas extras da dica
 * da mesa (o EnchantmentScreenMixin chama {@link #withInsight}).
 * <p>
 * Coerência: o servidor faz {@code broadcastChanges} antes de mandar o pacote, então custos, semente e slot 0 já estão
 * atualizados no menu do cliente quando o pacote chega. Guardamos esse estado na chegada e só mostramos as linhas
 * enquanto o menu continua nele — se custos/semente/item mudarem, as linhas somem até o próximo pacote (que o
 * servidor manda no tick em que percebe a mudança). O cache é limpo ao fechar a mesa ou trocar de menu.
 */
public final class EnchantInsightClient {

    private static int containerId = -1;
    private static List<List<EnchantLine>> rows = List.of();
    private static int seed;
    private static int[] costs = new int[0];
    private static ItemStack item = ItemStack.EMPTY;

    private EnchantInsightClient() {}

    static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> tick());
    }

    static void receive(S2CEnchantInsight msg) {
        EnchantmentMenu menu = openMenu(msg.containerId());
        if (menu == null) {
            clear();
            return;
        }
        containerId = msg.containerId();
        rows = msg.rows();
        seed = menu.getEnchantmentSeed();
        costs = menu.costs.clone();
        item = menu.getSlot(0).getItem().copy();
    }

    /** Linhas traduzidas (nome + nível) da opção {@code row}; vazio se não há dados válidos para esse menu. */
    public static List<Component> lines(int containerId, int row) {
        EnchantmentMenu menu = openMenu(containerId);
        if (menu == null || containerId != EnchantInsightClient.containerId || row < 0 || row >= rows.size()) return List.of();
        if (menu.getEnchantmentSeed() != seed || !Arrays.equals(menu.costs, costs)
                || !ItemStack.matches(menu.getSlot(0).getItem(), item)) {
            return List.of();
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return List.of();
        var registry = minecraft.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<Component> out = new ArrayList<>();
        for (EnchantLine line : rows.get(row)) {
            Identifier id = Identifier.tryParse(line.enchantId());
            if (id == null) continue;
            Optional<? extends Holder<Enchantment>> holder = registry.get(ResourceKey.create(Registries.ENCHANTMENT, id));
            holder.ifPresent(h -> out.add(Component.literal("  ").append(Enchantment.getFullname(h, line.level()))));
        }
        return out;
    }

    /**
     * Dica da opção {@code row} com a lista completa logo depois da linha de pista ("... ?"), antes dos custos.
     * Sem dados (jogador sem o nó, pacote ainda não chegou) devolve a dica original.
     */
    public static List<Component> withInsight(List<Component> tooltip, int containerId, int row) {
        List<Component> extra = lines(containerId, row);
        if (extra.isEmpty() || tooltip.isEmpty()) return tooltip;
        List<Component> out = new ArrayList<>(tooltip.size() + extra.size() + 1);
        out.add(tooltip.get(0));
        out.add(Component.translatable("gui.vanillatalents.enchant.insight").withStyle(ChatFormatting.DARK_PURPLE));
        out.addAll(extra);
        out.addAll(tooltip.subList(1, tooltip.size()));
        return out;
    }

    private static EnchantmentMenu openMenu(int id) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;
        return player.containerMenu instanceof EnchantmentMenu menu && menu.containerId == id ? menu : null;
    }

    private static void tick() {
        if (containerId != -1 && openMenu(containerId) == null) clear();
    }

    private static void clear() {
        containerId = -1;
        rows = List.of();
        costs = new int[0];
        item = ItemStack.EMPTY;
    }
}
