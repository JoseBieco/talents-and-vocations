package com.seunome.vanillatalents.data;

import com.seunome.vanillatalents.VanillaTalents;
import com.seunome.vanillatalents.core.TalentNode;
import com.seunome.vanillatalents.core.TalentRegistry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Lê {@code data/<ns>/skills/**.json} a cada reload e publica o registro validado em {@link TalentRegistries}. */
public class TalentDataLoader extends SimpleJsonResourceReloadListener<TalentNode> {

    public TalentDataLoader() {
        super(TalentNodeCodec.CODEC, FileToIdConverter.json("skills"));
    }

    public static void register() {
        AddReloadListenerEvent.BUS.addListener(event -> event.addListener(new TalentDataLoader()));
    }

    @Override
    protected void apply(Map<Identifier, TalentNode> parsed, ResourceManager manager, ProfilerFiller profiler) {
        List<String> errors = new ArrayList<>();
        TalentRegistry registry = TalentRegistry.build(parsed.values(), errors);
        for (String error : errors) {
            VanillaTalents.LOGGER.warn("[vanillatalents] Nó descartado: {}", error);
        }
        TalentRegistries.setServer(registry);
        VanillaTalents.LOGGER.info("Loaded {} talent nodes ({} discarded)", registry.size(), errors.size());
    }
}
