package common;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Abstract class, provides common functionality of OSI application layer.
 *
 * @author kamyshev.a
 */
public abstract class AbstractChannel extends TCPChannel {

    protected ByteBuffer buf;

    public final LinkedList<ChangeListener> OnPacket;
    public AbstractTask task;

    public AbstractChannel(SocketAddress address) throws Exception {
        super(address);
        OnPacket = new LinkedList<>();
        OnRead.add(this::OnTCPRead);
    }

    public ByteBuffer getInputBuffer() {
        return buf;
    }

    protected void Packet() {
        ChangeEvent event = new ChangeEvent(this);
        OnPacket.forEach((cl) -> {
            cl.stateChanged(event);
        });
    }

    protected void Error(Exception ex) {
        ChangeEvent event = new ChangeEvent(ex);
        getOnError().forEach((cl) -> {
            cl.stateChanged(event);
        });
        setStatus(StatusEnum.error);
    }

    /**
     * Sub-classes should override this to handle tcp read event.
     *
     * @param event
     */
    protected abstract void OnTCPRead(ChangeEvent event);

}
