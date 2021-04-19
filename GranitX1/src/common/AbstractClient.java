package common;

import common.custom.EventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.ListIterator;
import javax.swing.Timer;

/**
 * The abstract class, provides common functionality for client-server
 * communications.
 *
 * @author kamyshev.a
 */
public abstract class AbstractClient {

    private final EventListener PacketListener = this::OnChannelPacket;
    private final EventListener StatusListener = this::OnChannelStatus;
    private final EventListener LogListener = this::Log;
    private final Timer timer;

    public final LinkedList<EventListener> OnLog;
    public final LinkedList<EventListener> OnData;
    public final LinkedList<EventListener> OnConnect;

    protected int relevance_timeout = 10;
    protected int read_timeout = 5;
    protected final LinkedList<AbstractChannel> channels;
    protected final LinkedList<AbstractTask> tasks;
    protected final Object lock;
    protected boolean keep_connect;

    public AbstractClient() {
        lock = new Object();
        channels = new LinkedList<>();
        OnLog = new LinkedList<>();
        OnData = new LinkedList<>();
        OnConnect = new LinkedList<>();
        TimerHandler timer_handler = new TimerHandler();
        timer = new Timer(1000, timer_handler);
        tasks = new LinkedList<>();
        keep_connect = false;
    }

    protected void OnMainTimer() {
        Maintenance();
    }

    private class TimerHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ts) {
            OnMainTimer();
        }
    }

    /**
     * Subclasses must override this to handle the received packet.
     *
     * @param object instance of AbstractChannel
     */
    protected abstract void OnChannelPacket(Object object);

    protected void OnChannelStatus(Object object) {
        AbstractChannel channel = (AbstractChannel) object;
        AbstractChannel.StatusEnum status = channel.getStatus();
        if (status == AbstractChannel.StatusEnum.ready) {
            Maintenance();
        }
    }

    protected void Log(Object object) {
        OnLog.forEach((el) -> {
            el.Occurred(object);
        });
    }

    protected void Data(AbstractTask task) {
        OnData.forEach((el) -> {
            el.Occurred(task);
        });
    }

    protected abstract AbstractChannel CreateChannel();

    private AbstractChannel NewChannel() {
        AbstractChannel channel = CreateChannel();
        if (channel != null) {
            channel.OnPacket.add(PacketListener);
            channel.OnStatus.add(StatusListener);
            channel.OnLog.add(LogListener);
            try {
                channel.Open();
            } catch (Exception ex) {
                Log(ex);
                CloseChannel(channel);
                return null;
            }
        }
        return channel;
    }

    private void CloseChannel(AbstractChannel channel) {
        try {
            channel.Close();
        } catch (Exception ex) {
            Log(ex);
        }
    }

    protected void Maintenance() {
        synchronized (lock) {
            boolean is_prepared = keep_connect;
            ListIterator<AbstractTask> it_task = tasks.listIterator();
            while (it_task.hasNext()) {
                AbstractTask task = it_task.next();
                AbstractTask.StatusEnum status = task.getStatus();

                if (status == AbstractTask.StatusEnum.done) {
                    continue;
                }

                if (status == AbstractTask.StatusEnum.prepared) {
                    if (task.IsTimeout(relevance_timeout)) {
                        Data(task);
                    } else {
                        is_prepared = true;
                    }
                }
                if (status == AbstractTask.StatusEnum.requested) {
                    if (task.IsTimeout(read_timeout)) {
                        Data(task);
                    }
                }
            }
            if (!is_prepared) {
                return;
            }
            ChannelsMaintenance();
        }
    }

    private void ChannelsMaintenance() {
        ListIterator<AbstractChannel> it_channel = channels.listIterator();
        while (it_channel.hasNext()) {
            AbstractChannel channel = it_channel.next();
            if (channel == null) {
                it_channel.set(NewChannel());
                break;
            }
            AbstractChannel.StatusEnum channel_status = channel.getStatus();
            if (channel_status == AbstractChannel.StatusEnum.disconnect) {
                CloseChannel(channel);
                it_channel.set(NewChannel());
            } else if (channel_status == AbstractChannel.StatusEnum.error) {
                CloseChannel(channel);
                it_channel.set(null);
                break;
            } else if (channel_status == AbstractChannel.StatusEnum.closed) {
                it_channel.set(NewChannel());
            } else if (channel.getStatus() == AbstractChannel.StatusEnum.ready) {
                SetChannelTask(channel);
            }

        }
    }

    public boolean Connected() {
        ListIterator<AbstractChannel> it_channel = channels.listIterator();
        synchronized (lock) {
            while (it_channel.hasNext()) {
                AbstractChannel channel = it_channel.next();
                if (channel != null) {
                    if (channel.getStatus() == AbstractChannel.StatusEnum.ready){
                        return true;
                    }
                }


            }
        }
        return false;
    }

    protected abstract ByteBuffer getRequestData(AbstractTask task) throws Exception;

    protected void SetChannelTask(AbstractChannel channel) {
        if (channel.task != null) {
            if (channel.task.status == AbstractTask.StatusEnum.exspired) {
                channel.task = null;
            }
        }
        if (channel.task == null) {
            ListIterator<AbstractTask> it = tasks.listIterator();
            while (it.hasNext()) {
                AbstractTask task = it.next();
                if (task.getStatus() == AbstractTask.StatusEnum.prepared) {
                    channel.task = task;
                    try {
                        channel.Send(getRequestData(task));
                        break;
                    } catch (Exception ex) {
                        Log(ex);
                    }
                }
            }
        }
    }

    /**
     * Start maintenance service timer.
     */
    public final void Open() {
        timer.start();
        Maintenance();
    }

    /**
     * Stop maintenance service timer, close channels.
     */
    public final void Close() {
        timer.stop();
        channels.forEach((channel) -> {
            if (channel != null) {
                try {
                    channel.Close();
                } catch (Exception ex) {
                    Log(ex);
                }
            }
        });
    }

}
