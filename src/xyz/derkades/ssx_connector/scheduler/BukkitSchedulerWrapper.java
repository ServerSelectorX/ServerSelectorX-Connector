package xyz.derkades.ssx_connector.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class BukkitSchedulerWrapper implements SchedulerWrapper {

    private final Plugin plugin;

    public BukkitSchedulerWrapper(final Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public BukkitScheduledTask runAsync(final Runnable runnable) {
        return new BukkitScheduledTask(Bukkit.getScheduler().runTaskAsynchronously(this.plugin, runnable));
    }

    @Override
    public BukkitScheduledTask runTimer(final Runnable runnable, final int delay, final int interval) {
        return new BukkitScheduledTask(Bukkit.getScheduler().runTaskTimer(this.plugin, runnable, delay, interval));
    }

}
