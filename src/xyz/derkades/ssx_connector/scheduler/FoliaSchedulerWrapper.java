package xyz.derkades.ssx_connector.scheduler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

public class FoliaSchedulerWrapper implements SchedulerWrapper {

    private final Plugin plugin;
    private final Object asyncScheduler;
    private final Object globalRegionScheduler;
    private final Method asyncRunNowMethod;
    private final Method runTimerMethod;

    public FoliaSchedulerWrapper(final Plugin plugin) {
        try {
            this.plugin = plugin;
            final Server server = Bukkit.getServer();
            this.asyncScheduler = server.getClass().getMethod("getAsyncScheduler").invoke(server);
            this.asyncRunNowMethod = this.asyncScheduler.getClass().getMethod("runNow", Plugin.class, Runnable.class);
            this.globalRegionScheduler = server.getClass().getMethod("getGlobalRegionScheduler").invoke(server);
            this.runTimerMethod = this.globalRegionScheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Runnable.class, int.class, int.class);
        } catch (final InvocationTargetException | IllegalAccessException | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FoliaScheduledTask runAsync(final Runnable runnable) {
        try {
            return new FoliaScheduledTask(this.asyncRunNowMethod.invoke(this.asyncScheduler, this.plugin, runnable));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FoliaScheduledTask runTimer(final Runnable runnable, final int delay, final int interval) {
        try {
            return new FoliaScheduledTask(this.runTimerMethod.invoke(this.globalRegionScheduler, this.plugin, runnable, delay, interval));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
