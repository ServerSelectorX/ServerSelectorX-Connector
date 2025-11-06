package nl.rslot.ssx.connector.scheduler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FoliaScheduledTask extends AbstractScheduledTask {

    private static final Method cancelMethod;
    static {
        try {
            cancelMethod = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask").getMethod("cancel");
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private final Object foliaTask;
    FoliaScheduledTask(final Object foliaTask) {
        this.foliaTask = foliaTask;
    }

    @Override
    public void cancel() {
        try {
            cancelMethod.invoke(this.foliaTask);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
