package com.seunome.vanillatalents.effect;

import com.seunome.vanillatalents.VanillaTalents;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Fila de ações para o próximo tick do servidor (replantio, espera de reprodução, etc.). */
@Mod.EventBusSubscriber(modid = VanillaTalents.MODID)
public final class NextTick {

    private static final List<Runnable> TASKS = new ArrayList<>();

    private NextTick() {}

    /** Executa a tarefa no fim do próximo tick do servidor; tarefas agendadas durante a execução rodam no tick seguinte. */
    public static void schedule(Runnable task) {
        TASKS.add(task);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        if (TASKS.isEmpty()) return;
        List<Runnable> tasks = new ArrayList<>(TASKS);
        TASKS.clear();
        for (Runnable task : tasks) {
            try {
                task.run();
            } catch (RuntimeException e) {
                VanillaTalents.LOGGER.error("Falha numa tarefa agendada para o próximo tick", e);
            }
        }
    }

    /** Descarta tarefas pendentes: elas guardam o mundo antigo e não podem rodar no próximo mundo aberto na mesma JVM. */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
    }
}
