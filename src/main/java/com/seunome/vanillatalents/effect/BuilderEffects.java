package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.formula.BuilderFormulas;
import com.seunome.vanillatalents.core.formula.HookFormulas;
import com.seunome.vanillatalents.effect.loot.TalentLootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Efeitos da árvore do Construtor (alcance e Passo de Gato são atributos: AttributeSync). */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class BuilderEffects {

    public static final TagKey<Block> BUILDING_BLOCKS =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VanillaTalents.MODID, "building_blocks"));

    public static final TagKey<Item> CHEAP_BLOCKS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(VanillaTalents.MODID, "cheap_blocks"));

    /**
     * Jogador → gameTime da última quebra de um bloco de BUILDING_BLOCKS. Gravado em BreakEvent, que em
     * ServerPlayerGameMode.destroyBlock dispara antes de ItemStack.mineBlock (o gasto da ferramenta); só vale no
     * mesmo tick e é apagado por qualquer outra quebra.
     */
    private static final Map<UUID, Long> BUILDING_BREAK = new HashMap<>();

    private BuilderEffects() {}

    public static void registerLoot() {
        TalentLootModifier.register(TalentLootModifier.Kind.BUILDER_GLASS, BuilderEffects::glassLoot);
    }

    public static void forget(UUID player) {
        BUILDING_BREAK.remove(player);
    }

    /** Verdadeiro só durante a quebra (mesmo tick) de um bloco da tag por esse jogador. Usado pela lista R4. */
    public static boolean breakingBuildingBlock(ServerPlayer player) {
        Long time = BUILDING_BREAK.get(player.getUUID());
        return time != null && time == player.level().getGameTime();
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (event.getState().is(BUILDING_BLOCKS) && !event.getResult().isDenied()) {
            BUILDING_BREAK.put(player.getUUID(), player.level().getGameTime());
        } else {
            BUILDING_BREAK.remove(player.getUUID());
        }
    }

    /** builder_dismantle (R5: multiplica, sem teto). Os dois lados, com qualquer ferramenta. */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getState().is(BUILDING_BLOCKS)) return;
        int level = Talents.level(event.getEntity(), "builder_dismantle");
        if (level <= 0) return;
        double multiplier = BuilderFormulas.breakMultiplier(level, Talents.value(event.getEntity(), "builder_dismantle", "per_level"));
        event.setNewSpeed((float) (event.getNewSpeed() * multiplier));
    }

    /**
     * builder_hardhat: redução ambiental (sem teto, fora do R2) de blocos caindo e sufocamento. Combina com a parte de
     * blocos caindo de miner_stoneskin multiplicando (cada listener aplica o seu fator).
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DamageSource source = event.getSource();
        if (!source.is(DamageTypes.FALLING_BLOCK) && !source.is(DamageTypes.FALLING_ANVIL)
                && !source.is(DamageTypes.FALLING_STALACTITE) && !source.is(DamageTypes.IN_WALL)) return;
        int level = Talents.level(player, "builder_hardhat");
        if (level <= 0) return;
        event.setAmount((float) (event.getAmount()
                * HookFormulas.reductionMultiplier(level, Talents.value(player, "builder_hardhat", "per_level"))));
    }

    /** builder_glass: vidro ou painel quebrado sem Toque Suave e sem drops dropa o próprio bloco. */
    static ObjectArrayList<ItemStack> glassLoot(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) return loot;
        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
        if (state == null) return loot;
        if (Talents.level(player, "builder_glass") <= 0) return loot;
        boolean glass = state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES);
        boolean silk = glass && MinerEffects.hasSilkTouch(player, context.getOptional(LootContextParams.TOOL));
        if (!BuilderFormulas.glassDrop(glass, loot.isEmpty(), silk)) return loot;
        ItemStack drop = new ItemStack(state.getBlock().asItem());
        if (!drop.isEmpty()) loot.add(drop);
        return loot;
    }
}
