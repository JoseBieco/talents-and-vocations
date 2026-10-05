package com.seunome.vanillatalents.effect.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.seunome.vanillatalents.VanillaTalents;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * GLM único do mod. O campo {@code kind} escolhe o efeito; cada árvore registra o seu handler com
 * {@link #register}. Um JSON por kind em {@code data/vanillatalents/loot_modifiers/}.
 */
public class TalentLootModifier extends LootModifier {

    public enum Kind {
        MINER_FORTUNE, FARMER_HARVEST, FARMER_FORESTER, FARMER_REPLANT;

        static final Codec<Kind> CODEC = Codec.STRING.xmap(s -> Kind.valueOf(s.toUpperCase(Locale.ROOT)),
                k -> k.name().toLowerCase(Locale.ROOT));
    }

    @FunctionalInterface
    public interface Handler {
        ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> loot, LootContext context);
    }

    public static final MapCodec<TalentLootModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i)
            .and(Kind.CODEC.fieldOf("kind").forGetter(m -> m.kind))
            .apply(i, TalentLootModifier::new));

    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, VanillaTalents.MODID);

    static {
        SERIALIZERS.register("talent", () -> CODEC);
    }

    private static final Map<Kind, Handler> HANDLERS = new EnumMap<>(Kind.class);

    private final Kind kind;

    public TalentLootModifier(LootItemCondition[] conditions, Kind kind) {
        super(conditions);
        this.kind = kind;
    }

    public static void registerSerializer(BusGroup modBusGroup) {
        SERIALIZERS.register(modBusGroup);
    }

    public static void register(Kind kind, Handler handler) {
        HANDLERS.put(kind, handler);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(LootTable table, ObjectArrayList<ItemStack> loot, LootContext context) {
        Handler handler = HANDLERS.get(kind);
        return handler == null ? loot : handler.apply(loot, context);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
