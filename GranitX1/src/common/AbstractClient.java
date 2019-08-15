package common;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.ListIterator;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The abstract class, provides common functionality for client-server
 * communications.
 *
 * @author kamyshev.a
 */
public abstract class AbstractClient {

    private final ChangeListener PacketListener = this::OnChannelPacket;
    private final ChangeListener StatusListener = this::OnChannelStatus;
    private final ChangeListener ErrorListener = this::OnChannelError;
    private final Timer timer;

    public final LinkedList<ChangeListener> OnError;
    public final LinkedList<ChangeListener> OnData;

    protected int relevance_timeout = 10;
    protected int read_timeout = 5;
    protected final LinkedList<AbstractChannel> channels;
    protected final LinkedList<AbstractTask> tasks;
    protected final Object lock;

    public AbstractClient() {
        lock = new Object();
        channels = new LinkedList<>();
        OnError = new LinkedList<>();
        OnData = new LinkedList<>();
        TimerHandler timer_handler = new TimerHandler();
        timer = new Timer(1000, timer_handler);
        tasks = new LinkedList<>();
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
     * @param event The event source is instance of AbstractChannel
     */
    protected abstract void OnChannelPacket(ChangeEvent event);

    protected void OnChannelStatus(ChangeEvent event) {
        AbstractChannel channel = (AbstractChannel)event.getSource();
        AbstractChannel.StatusEnum status = channel.getStatus();
        if (status == AbstractChannel.StatusEnum.ready) {
            Maintenance();
        }
    }

    private void OnChannelError(ChangeEvent event) {
        OnError.forEach((cl) -> {
            cl.stateChanged(event);
        });
    }

    protected final void Error(Exception ex) {
        ChangeEvent event = new ChangeEvent(ex);
        OnError.forEach((cl) -> {
            cl.stateChanged(event);
        });
    }

    protected final void Data(Object object) {
        ChangeEvent event = new ChangeEvent(object);
        OnData.forEach((cl) -> {
            cl.stateChanged(event);
        });
    }

    protected abstract AbstractChannel CreateChannel();

    private AbstractChannel NewChannel() {
        AbstractChannel channel = CreateChannel();
        if (channel != null) {
            channel.OnPacket.add(PacketListener);
            channel.OnStatus.add(StatusListener);
            channel.getOnError().add(ErrorListener);
            try {
                channel.Open();
            } catch (Exception ex) {
                Error(ex);
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
            Error(ex);
        }
    }

    protected void Maintenance() {
        synchronized (lock) {
            boolean is_prepared = false;
            ListIterator<AbstractTask> it_task = tasks.listIterator();
            while (it_task.hasNext()) {
                AbstractTask task = it_task.next();
                AbstractTask.StatusEnum status = task.getStatus();
                if (status == AbstractTask.StatusEnum.loaded) {
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

    protected abstract ByteBuffer getRequestData(AbstractTask task)throws Exception;

    protected void SetChannelTask(AbstractChannel channel) {
        if (channel.task == null) {
            ListIterator<AbstractTask> it = tasks.listIterator();
            while (it.hasNext()) {
                AbstractTask task = it.next();
                if (task.getStatus() == AbstractTask.StatusEnum.prepared) {
                    channel.task = task;
                    try {
                        ByteBuffer buf = getRequestData(task);
                        channel.Send(buf);
                        task.SetRequested();
                        break;
                    } catch (Exception ex) {
                        Error(ex);
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
                    Error(ex);
                }
            }
        });
    }

}
