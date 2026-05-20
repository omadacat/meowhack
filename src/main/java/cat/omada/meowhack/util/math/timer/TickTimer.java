package cat.omada.meowhack.util.math.timer;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;

public class TickTimer implements Timer {

    //
    private long ticks;

    /**
     *
     */
    public TickTimer()
    {
        ticks = 0;
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    /**
     * @param event
     */
    @EventHandler(priority = Integer.MAX_VALUE)
    public void onTick(TickEvent.Pre event)
    {
        ++ticks;
    }

    /**
     * Returns <tt>true</tt> if the time since the last reset has exceeded
     * the param time.
     *
     * @param time The param time
     * @return <tt>true</tt> if the time since the last reset has exceeded
     * the param time
     */
    @Override
    public boolean passed(Number time)
    {
        return ticks >= time.longValue();
    }

    /**
     *
     */
    @Override
    public void reset()
    {
        setElapsedTime(0);
    }

    /**
     * @return
     */
    @Override
    public long getElapsedTime()
    {
        return ticks;
    }

    /**
     * @param time
     */
    @Override
    public void setElapsedTime(Number time)
    {
        ticks = time.longValue();
    }

}
