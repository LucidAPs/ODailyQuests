package com.lucidaps.odailyquests.tools;

import com.lucidaps.odailyquests.ODailyQuests;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public final class TaskScheduler {

    private static final long MILLIS_PER_TICK = 50L;

    private TaskScheduler() {
    }

    public static void runSync(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }

        Bukkit.getScheduler().runTask(ODailyQuests.INSTANCE, task);
    }

    public static void runSyncLater(Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(ODailyQuests.INSTANCE, task, delayTicks);
    }

    public static BukkitTask scheduleSyncLater(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(ODailyQuests.INSTANCE, task, delayTicks);
    }

    public static void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(ODailyQuests.INSTANCE, task);
    }

    public static void runAsyncLater(Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(ODailyQuests.INSTANCE, task, delayTicks);
    }

    public static long ticksFromMillis(long millis) {
        if (millis <= 0) {
            return 0L;
        }

        return Math.max(1L, Math.round((double) millis / MILLIS_PER_TICK));
    }
}
