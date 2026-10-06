package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.Config;
import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.RecursionGuard;
import com.seunome.vanillatalents.core.formula.FarmerFormulas;
import com.seunome.vanillatalents.core.formula.HookFormulas;
import com.seunome.vanillatalents.effect.loot.TalentLootModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Efeitos da árvore do Produtor (farmer_hoe_care fica em DurabilityHooks). */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class FarmerEffects {

    public static final TagKey<EntityType<?>> LIVESTOCK =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(VanillaTalents.MODID, "livestock"));

    /** Espera vanilla após reproduzir (Animal.spawnChildFromBreeding). */
    private static final int VANILLA_BREED_COOLDOWN = 6000;
    private static final String AREA_GUARD = "farmer_area_harvest";

    private record Movement(Vec3 pos, long lastMoveTick) {}

    private static final Map<UUID, Movement> MOVEMENT = new HashMap<>();

    private FarmerEffects() {}

    public static void registerLoot() {
        TalentLootModifier.register(TalentLootModifier.Kind.FARMER_HARVEST, FarmerEffects::harvestLoot);
        TalentLootModifier.register(TalentLootModifier.Kind.FARMER_FORESTER, FarmerEffects::foresterLoot);
        TalentLootModifier.register(TalentLootModifier.Kind.FARMER_REPLANT, FarmerEffects::replantLoot);
    }

    public static void forget(UUID player) {
        MOVEMENT.remove(player);
    }

    static boolean isMatureCrop(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) return crop.isMaxAge(state);
        if (state.getBlock() instanceof NetherWartBlock) return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        return false;
    }

    // ---- Colheita ----------------------------------------------------------------------------------------------

    /** farmer_area_harvest: colher uma planta madura com enxada colhe as maduras vizinhas. */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!player.getMainHandItem().is(ItemTags.HOES) || !isMatureCrop(event.getState())) return;
        int level = Talents.level(player, "farmer_area_harvest");
        if (level <= 0 || event.getResult().isDenied()) return;
        BlockPos origin = event.getPos();
        RecursionGuard.SERVER.runGuarded(player.getUUID(), AREA_GUARD, () -> {
            for (FarmerFormulas.Offset o : FarmerFormulas.areaOffsets(level)) {
                BlockPos pos = origin.offset(o.dx(), 0, o.dz());
                if (isMatureCrop(player.level().getBlockState(pos))) player.gameMode.destroyBlock(pos);
            }
        });
    }

    /** farmer_light_step: não pisoteia terra arada. Retornar true cancela o evento. */
    @SubscribeEvent
    public static boolean onTrample(BlockEvent.FarmlandTrampleEvent event) {
        return event.getEntity() instanceof ServerPlayer player && Talents.level(player, "farmer_light_step") > 0;
    }

    /** farmer_harvest: +1 do produto principal de uma planta madura. */
    static ObjectArrayList<ItemStack> harvestLoot(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) return loot;
        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
        if (state == null || !isMatureCrop(state) || loot.isEmpty()) return loot;
        int level = Talents.level(player, "farmer_harvest");
        if (level <= 0) return loot;
        if (context.getRandom().nextDouble() >= HookFormulas.chance(level, Talents.value(player, "farmer_harvest", "per_level"))) return loot;
        ItemStack product = mainProduct(loot, seedOf(state, context));
        if (product != null) loot.add(product.copyWithCount(1));
        return loot;
    }

    /** O drop que não é a semente; se só houver a semente (cenoura, batata, verruga), ela mesma. */
    private static ItemStack mainProduct(List<ItemStack> loot, ItemStack seed) {
        for (ItemStack stack : loot) {
            if (!ItemStack.isSameItem(stack, seed)) return stack;
        }
        return loot.isEmpty() ? null : loot.getFirst();
    }

    private static ItemStack seedOf(BlockState state, LootContext context) {
        Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
        BlockPos pos = origin == null ? BlockPos.ZERO : BlockPos.containing(origin);
        return state.getCloneItemStack(context.getLevel(), pos, false);
    }

    /** farmer_replant: colheita com enxada guarda 1 semente e replanta no tick seguinte. */
    static ObjectArrayList<ItemStack> replantLoot(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) return loot;
        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
        Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
        if (state == null || origin == null || !isMatureCrop(state)) return loot;
        if (!player.getMainHandItem().is(ItemTags.HOES) || Talents.level(player, "farmer_replant") <= 0) return loot;

        ItemStack seed = seedOf(state, context);
        for (ItemStack stack : loot) {
            if (ItemStack.isSameItem(stack, seed)) {
                stack.shrink(1);
                loot.removeIf(ItemStack::isEmpty);
                ServerLevel level = context.getLevel();
                BlockPos pos = BlockPos.containing(origin);
                Block block = state.getBlock();
                NextTick.schedule(() -> replant(level, pos, block));
                return loot;
            }
        }
        return loot;
    }

    private static void replant(ServerLevel level, BlockPos pos, Block block) {
        if (!level.getBlockState(pos).isAir()) return;
        BlockState below = level.getBlockState(pos.below());
        if (block instanceof NetherWartBlock) {
            if (below.is(Blocks.SOUL_SAND)) level.setBlockAndUpdate(pos, block.defaultBlockState());
        } else if (block instanceof CropBlock crop && below.is(Blocks.FARMLAND)) {
            level.setBlockAndUpdate(pos, crop.getStateForAge(0));
        }
    }

    // ---- Pecuária ----------------------------------------------------------------------------------------------

    /** farmer_animal_drops: +1 em uma pilha aleatória ao abater animal de criação. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!event.getEntity().is(LIVESTOCK) || event.getDrops().isEmpty()) return;
        int level = Talents.level(player, "farmer_animal_drops");
        if (level <= 0) return;
        RandomSource random = player.getRandom();
        if (random.nextDouble() >= HookFormulas.chance(level, Talents.value(player, "farmer_animal_drops", "per_level"))) return;
        List<ItemEntity> drops = new ArrayList<>(event.getDrops());
        ItemEntity chosen = drops.get(random.nextInt(drops.size()));
        chosen.getItem().grow(1);
    }

    /** farmer_shearing: chance de +1 lã da cor da ovelha. */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof Sheep sheep) || !sheep.readyForShearing()) return;
        if (!player.getItemInHand(event.getHand()).is(Items.SHEARS)) return;
        int level = Talents.level(player, "farmer_shearing");
        if (level <= 0) return;
        if (player.getRandom().nextDouble() >= HookFormulas.chance(level, Talents.value(player, "farmer_shearing", "per_level"))) return;
        Identifier woolId = Identifier.withDefaultNamespace(sheep.getColor().getName() + "_wool");
        sheep.spawnAtLocation(player.level(), new ItemStack(BuiltInRegistries.ITEM.getValue(woolId)));
    }

    /** farmer_breeding (espera menor, aplicada no tick seguinte) e farmer_twins (filhote extra). */
    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer player)) return;
        if (!(event.getParentA() instanceof Animal a) || !(event.getParentB() instanceof Animal b)) return;
        ServerLevel level = player.level();

        int breeding = Talents.level(player, "farmer_breeding");
        if (breeding > 0) {
            int cooldown = FarmerFormulas.breedingCooldown(VANILLA_BREED_COOLDOWN, breeding,
                    Talents.value(player, "farmer_breeding", "per_level"));
            NextTick.schedule(() -> {
                if (a.isAlive() && a.getAge() > cooldown) a.setAge(cooldown);
                if (b.isAlive() && b.getAge() > cooldown) b.setAge(cooldown);
            });
        }

        int twins = Talents.level(player, "farmer_twins");
        if (twins <= 0 || event.getChild() == null) return;
        if (player.getRandom().nextDouble() >= HookFormulas.chance(twins, Talents.value(player, "farmer_twins", "per_level"))) return;
        int limit = (int) Talents.value(player, "farmer_twins", "chunk_limit");
        if (animalsInChunk(level, a.blockPosition()) >= limit) return;
        AgeableMob extra = a.getBreedOffspring(level, b);
        if (extra == null) return;
        extra.setBaby(true);
        extra.snapTo(a.getX(), a.getY(), a.getZ(), 0.0F, 0.0F);
        level.addFreshEntityWithPassengers(extra);
    }

    private static int animalsInChunk(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = ChunkPos.containing(pos);
        AABB box = new AABB(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX() + 1, level.getMaxY() + 1, chunk.getMaxBlockZ() + 1);
        return level.getEntitiesOfClass(Animal.class, box).size();
    }

    // ---- Cultivo -----------------------------------------------------------------------------------------------

    /** farmer_bonemeal: com chance, a farinha de osso não é consumida. */
    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        BlockState state = event.getBlock();
        if (!(state.getBlock() instanceof BonemealableBlock block)
                || !block.isValidBonemealTarget(event.getLevel(), event.getPos(), state, BonemealSource.INTERACTION)) return;
        int level = Talents.level(player, "farmer_bonemeal");
        if (level <= 0) return;
        if (player.getRandom().nextDouble() < HookFormulas.chance(level, Talents.value(player, "farmer_bonemeal", "per_level"))) {
            event.getStack().grow(1); // compensa o shrink(1) que vem depois
        }
    }

    /** farmer_forester: com chance, +1 de cada muda/maçã que as folhas deram. */
    static ObjectArrayList<ItemStack> foresterLoot(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!(context.getOptional(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) return loot;
        BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
        if (state == null || !state.is(BlockTags.LEAVES)) return loot;
        int level = Talents.level(player, "farmer_forester");
        if (level <= 0) return loot;
        if (context.getRandom().nextDouble() >= HookFormulas.chance(level, Talents.value(player, "farmer_forester", "per_level"))) return loot;
        List<ItemStack> extras = new ArrayList<>();
        for (ItemStack stack : loot) {
            if (stack.is(ItemTags.SAPLINGS) || stack.is(Items.APPLE)) extras.add(stack.copyWithCount(1));
        }
        loot.addAll(extras);
        return loot;
    }

    /** farmer_growth_aura: a cada pulso, plantas no raio (Y±1) têm chance de receber um tick aleatório. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        long now = player.level().getGameTime();
        Movement previous = MOVEMENT.get(player.getUUID());
        Vec3 pos = player.position();
        boolean moved = previous == null || previous.pos().distanceToSqr(pos) > 1.0E-4;
        MOVEMENT.put(player.getUUID(), new Movement(pos, moved ? now : previous.lastMoveTick()));

        int level = Talents.level(player, "farmer_growth_aura");
        if (level <= 0) return;
        int interval = (int) Talents.value(player, "farmer_growth_aura", "interval");
        if (player.tickCount % Math.max(1, interval) != 0) return;
        if (!FarmerFormulas.auraAllowed(MOVEMENT.get(player.getUUID()).lastMoveTick(), now, Config.AURA_IDLE_TICKS.get())) return;

        ServerLevel world = player.level();
        int radius = FarmerFormulas.auraRadius(level, (int) Talents.value(player, "farmer_growth_aura", "radius_base"));
        double chance = Talents.value(player, "farmer_growth_aura", "chance");
        boolean forester = Talents.level(player, "farmer_forester") > 0;
        int max = Config.AURA_MAX_PER_PULSE.get();
        int advanced = 0;
        BlockPos center = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
            if (advanced >= max) break;
            BlockState state = world.getBlockState(p);
            if (!isAuraTarget(state, forester) || world.getRandom().nextDouble() >= chance) continue;
            state.randomTick(world, p.immutable(), world.getRandom());
            advanced++;
        }
    }

    private static boolean isAuraTarget(BlockState state, boolean forester) {
        if (state.is(BlockTags.CROPS) || state.getBlock() instanceof NetherWartBlock) return !isMatureCrop(state);
        return forester && (state.is(BlockTags.SAPLINGS) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS)
                || state.is(Blocks.BAMBOO) || state.is(Blocks.BAMBOO_SAPLING));
    }
}
