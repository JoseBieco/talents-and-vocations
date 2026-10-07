package com.seunome.vanillatalents.client;

import com.mojang.math.Transformation;
import com.seunome.vanillatalents.core.formula.OreHighlight;
import com.seunome.vanillatalents.data.TalentRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Faro Mineral: contorno colorido, visível através das paredes, em cada minério encontrado. Usa BlockDisplays
 * criados só no cliente (o servidor e os outros jogadores não os veem), um pouco menores que o bloco para que
 * o minério real os cubra quando estiver à vista.
 */
public final class OreHighlights {

    private static final float SCALE = 0.98F;
    private static final int MAX_HIGHLIGHTS = 256;
    private static final int DEFAULT_TICKS = 60;

    private static final List<Entity> ACTIVE = new ArrayList<>();
    private static ClientLevel activeLevel;
    private static int ticksLeft;
    private static int nextId = -1_000_000;

    private OreHighlights() {}

    static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> tick());
    }

    static void show(List<BlockPos> positions) {
        clear();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        for (BlockPos pos : OreHighlight.capped(positions, MAX_HIGHLIGHTS)) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(Tags.Blocks.ORES)) continue;
            HighlightEntity entity = new HighlightEntity(level);
            entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag(pos, state)));
            entity.setId(nextId--);
            level.addEntity(entity);
            ACTIVE.add(entity);
        }
        activeLevel = level;
        ticksLeft = TalentRegistries.client().get("miner_prospector")
                .map(n -> n.values().getOrDefault("highlight_ticks", (double) DEFAULT_TICKS).intValue())
                .orElse(DEFAULT_TICKS);
    }

    private static CompoundTag tag(BlockPos pos, BlockState state) {
        CompoundTag tag = new CompoundTag();
        Vec3.CODEC.encodeStart(NbtOps.INSTANCE, Vec3.atLowerCornerOf(pos)).result().ifPresent(t -> tag.put("Pos", t));
        tag.put("block_state", NbtUtils.writeBlockState(state));
        float inset = OreHighlight.insetTranslation(SCALE);
        Transformation transformation = new Transformation(new Vector3f(inset, inset, inset), null,
                new Vector3f(SCALE, SCALE, SCALE), null);
        Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, transformation).result()
                .ifPresent(t -> tag.put("transformation", t));
        tag.putInt("glow_color_override", OreHighlight.colorFor(commonTagPaths(state)));
        return tag;
    }

    private static List<String> commonTagPaths(BlockState state) {
        return state.getBlock().builtInRegistryHolder().tags()
                .filter(t -> t.location().getNamespace().equals("c"))
                .map(t -> t.location().getPath())
                .toList();
    }

    private static void tick() {
        if (ticksLeft <= 0) return;
        if (Minecraft.getInstance().level != activeLevel) {
            // Saiu do mundo ou trocou de dimensão: as entidades foram junto com o nível antigo.
            ACTIVE.clear();
            ticksLeft = 0;
            return;
        }
        if (--ticksLeft == 0) clear();
    }

    private static void clear() {
        if (activeLevel != null && activeLevel == Minecraft.getInstance().level) {
            for (Entity entity : ACTIVE) activeLevel.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        }
        ACTIVE.clear();
        ticksLeft = 0;
    }

    /** BlockDisplay sempre brilhando: no cliente, setGlowingTag não liga o flag de brilho. */
    private static final class HighlightEntity extends Display.BlockDisplay {
        HighlightEntity(Level level) {
            super(EntityTypes.BLOCK_DISPLAY, level);
        }

        @Override
        public boolean isCurrentlyGlowing() {
            return true;
        }
    }
}
