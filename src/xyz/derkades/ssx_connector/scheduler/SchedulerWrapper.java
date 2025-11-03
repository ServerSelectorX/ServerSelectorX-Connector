package xyz.derkades.ssx_connector.scheduler;

public interface SchedulerWrapper {

    public AbstractScheduledTask runAsync(Runnable runnable);

    public AbstractScheduledTask runTimer(Runnable runnable, int delay, int interval);

}
