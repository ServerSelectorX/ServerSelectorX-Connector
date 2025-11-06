package nl.rslot.ssx.connector.scheduler;

import org.bukkit.scheduler.BukkitTask;

public class BukkitScheduledTask extends AbstractScheduledTask {

    private final BukkitTask task;

    BukkitScheduledTask(final BukkitTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        this.task.cancel();
    }

}
