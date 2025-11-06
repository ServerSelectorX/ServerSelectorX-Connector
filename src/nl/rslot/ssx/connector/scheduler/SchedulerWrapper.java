package nl.rslot.ssx.connector.scheduler;

public interface SchedulerWrapper {

    public AbstractScheduledTask runAsync(Runnable runnable);

    public AbstractScheduledTask runTimer(Runnable runnable, int delay, int interval);

}
