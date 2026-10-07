package com.josebieco.talentsvocations.effect.hooks;

import com.josebieco.talentsvocations.core.formula.ArtisanFormulas;
import com.josebieco.talentsvocations.core.formula.HookFormulas;
import com.josebieco.talentsvocations.effect.Talents;
import com.josebieco.talentsvocations.network.ModNetwork;
import com.josebieco.talentsvocations.network.S2CEnchantInsight;
import com.josebieco.talentsvocations.network.S2CEnchantInsight.EnchantLine;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Mesa de encantamento do Artífice: artisan_lapis, artisan_bookshelf e artisan_insight. */
public final class EnchantHooks {

    private static final int ROWS = 3;

    /** Último estado da mesa enviado a cada jogador com Visão Arcana (só thread do servidor). */
    private static final Map<ServerPlayer, Snapshot> SENT = new WeakHashMap<>();

    private record Snapshot(EnchantmentMenu menu, int seed, int[] costs, ItemStack item) {}

    private EnchantHooks() {}

    /**
     * artisan_lapis: chamado pelo EnchantmentMenuMixin no lugar de {@code currency.consume(...)} dentro do lambda de
     * {@code clickMenuButton}. Devolve {@code true} quando o lápis NÃO deve ser consumido (chance {@code lvl·per}).
     * O lambda só roda no servidor ({@code ContainerLevelAccess.NULL} no cliente), então o cliente nunca prevê o
     * consumo: o slot chega pela sincronização normal do menu. O custo em níveis ({@code onEnchantmentPerformed}) já
     * foi cobrado antes e não muda. Criativo: a vanilla já não consome.
     */
    public static boolean keepLapis(LivingEntity owner) {
        if (!(owner instanceof ServerPlayer player) || player.hasInfiniteMaterials()) return false;
        int level = Talents.level(player, "artisan_lapis");
        if (level <= 0) return false;
        double chance = HookFormulas.chance(level, Talents.value(player, "artisan_lapis", "per_level"));
        return player.getRandom().nextDouble() < chance;
    }

    /**
     * artisan_bookshelf: nível da linha {@code row} de {@code EnchantmentLevelSetEvent} recalculado com as estantes de
     * {@link ArtisanFormulas#boostedShelves}. O evento não traz o jogador; ele dispara dentro de
     * {@code slotsChanged → access.execute} (só servidor), com {@code item} = {@code enchantSlots.getItem(0)} — a
     * instância exata do slot 0 daquele menu. Cada menu tem o próprio {@code SimpleContainer}, então o jogador é o único
     * do nível cujo {@code containerMenu} é um EnchantmentMenu com essa mesma instância no slot 0. Por isso, achado esse
     * jogador e ele não tendo o nó, devolver o nível atual (sem procurar outros) é intencional: nenhum outro menu tem
     * essa instância de ItemStack.
     * <p>
     * Recalcula como a vanilla faria com as estantes ampliadas: {@code RandomSource} semeado com
     * {@code enchantmentSeed} (o {@code setSeed} da vanilla) e {@code getEnchantmentCost} chamado para as linhas
     * 0..row em ordem — cada chamada consome números diferentes conforme as estantes, então as linhas anteriores
     * precisam ser refeitas com o mesmo valor. Mesma regra "custo &lt; linha+1 → 0". A própria
     * {@code getEnchantmentCost} limita a 15 estantes (nível máximo 30). Os custos chegam ao cliente pelos DataSlots.
     */
    public static int bookshelfLevel(Level level, ItemStack item, int row, int power, int current) {
        if (level.isClientSide() || item.isEmpty()) return current;
        for (Player candidate : level.players()) {
            if (!(candidate instanceof ServerPlayer player)) continue;
            if (!(player.containerMenu instanceof EnchantmentMenu menu)) continue;
            if (menu.getSlot(0).getItem() != item) continue;
            int lvl = Talents.level(player, "artisan_bookshelf");
            if (lvl <= 0) return current;
            int shelves = ArtisanFormulas.boostedShelves(power, lvl,
                    Talents.value(player, "artisan_bookshelf", "per_level"),
                    (int) Talents.value(player, "artisan_bookshelf", "max_shelves"));
            if (shelves <= power) return current;
            return rowCost(menu.getEnchantmentSeed(), row, shelves, item);
        }
        return current;
    }

    /** Custo da linha como em {@code EnchantmentMenu.slotsChanged}, com {@code shelves} estantes. */
    private static int rowCost(int seed, int row, int shelves, ItemStack item) {
        RandomSource random = RandomSource.create(seed);
        int cost = 0;
        for (int r = 0; r <= row; r++) {
            cost = EnchantmentHelper.getEnchantmentCost(random, r, shelves, item);
        }
        return cost < row + 1 ? 0 : cost;
    }

    /**
     * artisan_insight: no {@code PlayerTickEvent.Post} do servidor, com uma mesa aberta. Quando semente, custos ou item
     * mudam (ou o menu é outro), calcula a lista de cada linha com {@code EnchantmentMenu.getEnchantmentList} (AT) —
     * a MESMA chamada, com os mesmos argumentos (slot 0, linha, {@code costs[linha]}, semente atual), que
     * {@code clickMenuButton} usa para encantar — e manda {@link S2CEnchantInsight}. {@code getEnchantmentList}
     * re-semeia o {@code random} do menu antes de usar e todo uso vanilla dele também re-semeia antes, então a chamada
     * extra não muda nada. {@code broadcastChanges} antes do envio garante que custos/semente/slot chegam ao cliente
     * antes do pacote (o cliente valida as linhas contra esse estado).
     */
    public static void tickInsight(ServerPlayer player) {
        if (!(player.containerMenu instanceof EnchantmentMenu menu) || Talents.level(player, "artisan_insight") <= 0) {
            SENT.remove(player);
            return;
        }
        ItemStack item = menu.getSlot(0).getItem();
        int seed = menu.getEnchantmentSeed();
        Snapshot last = SENT.get(player);
        if (last != null && last.menu() == menu && last.seed() == seed && Arrays.equals(last.costs(), menu.costs)
                && ItemStack.matches(last.item(), item)) {
            return;
        }
        SENT.put(player, new Snapshot(menu, seed, menu.costs.clone(), item.copy()));
        List<List<EnchantLine>> rows = new ArrayList<>(ROWS);
        for (int row = 0; row < ROWS; row++) {
            int cost = menu.costs[row];
            if (cost <= 0 || item.isEmpty()) {
                rows.add(List.of());
                continue;
            }
            rows.add(lines(menu.getEnchantmentList(player.level().registryAccess(), item, row, cost)));
        }
        menu.broadcastChanges();
        ModNetwork.sendTo(player, new S2CEnchantInsight(menu.containerId, rows));
    }

    private static List<EnchantLine> lines(List<EnchantmentInstance> list) {
        List<EnchantLine> out = new ArrayList<>(list.size());
        for (EnchantmentInstance instance : list) {
            Holder<Enchantment> holder = instance.enchantment();
            Optional<ResourceKey<Enchantment>> key = holder.unwrapKey();
            if (out.size() >= S2CEnchantInsight.MAX_LINES) break;
            key.map(k -> k.identifier().toString())
                    .filter(id -> id.length() <= S2CEnchantInsight.MAX_ID_LENGTH)
                    .ifPresent(id -> out.add(new EnchantLine(id, instance.level())));
        }
        return out;
    }
}
