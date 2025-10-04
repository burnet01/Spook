package rip.thecraft.spook.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Scheduler compatibility layer for both Folia and Paper.
 * Automatically detects the server type and uses appropriate scheduling methods.
 */
public class SchedulerUtil {
    
    private static final boolean IS_FOLIA = checkFolia();
    private static final Logger LOGGER = Logger.getLogger("HubCore-Scheduler");
    
    private static boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Run a task synchronously on the main thread or appropriate region
     */
    public static void runTask(@NotNull Plugin plugin, @NotNull Runnable task) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * Run a task with delay synchronously
     */
    public static void runTaskLater(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }
    
    /**
     * Run a task asynchronously
     */
    public static void runTaskAsync(@NotNull Plugin plugin, @NotNull Runnable task) {
        if (IS_FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }
    
    /**
     * Run a task asynchronously with delay
     */
    public static void runTaskLaterAsync(@NotNull Plugin plugin, @NotNull Runnable task, long delay, @NotNull TimeUnit timeUnit) {
        if (IS_FOLIA) {
            Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> task.run(), delay, timeUnit);
        } else {
            long delayTicks = timeUnit.toSeconds(delay) * 20; // Convert to ticks
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
    }
    
    /**
     * Run a repeating task
     */
    public static void runTaskTimer(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks, long periodTicks) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, scheduledTask -> task.run(), delayTicks, periodTicks);
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }
    
    /**
     * Run a task for a specific entity (region-aware for Folia)
     */
    public static void runTaskForEntity(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task) {
        if (IS_FOLIA) {
            entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * Run a task for a specific entity with delay (region-aware for Folia)
     */
    public static void runTaskLaterForEntity(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }
    
    /**
     * Run a task for a specific location (region-aware for Folia)
     */
    public static void runTaskAtLocation(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task) {
        if (IS_FOLIA) {
            Bukkit.getRegionScheduler().run(plugin, location, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * Check if we're running on Folia
     */
    public static boolean isFolia() {
        return IS_FOLIA;
    }
    
    /**
     * Get server type as string for logging
     */
    public static String getServerType() {
        return IS_FOLIA ? "Folia" : "Paper/Bukkit";
    }
    
    /**
     * Log scheduler initialization
     */
    public static void logSchedulerInfo() {
        LOGGER.info("Initialized scheduler compatibility layer for " + getServerType());
    }
}