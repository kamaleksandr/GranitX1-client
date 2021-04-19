package common;

import common.custom.EventListener;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Abstract class, provides common functionality of OSI application layer.
 *
 * @author kamyshev.a
 */
public abstract class AbstractChannel extends TCPChannel {

    protected ByteBuffer buffer;

    public final LinkedList<EventListener> OnPacket;
    public AbstractTask task;

    public AbstractChannel(SocketAddress address) throws Exception {
        super(address);
        OnPacket = new LinkedList<>();
        OnRead.add(this::OnTCPRead);
    }

    public ByteBuffer getInputBuffer() {
        return buffer;
    }

    protected void Packet() {
        OnPacket.forEach((cl) -> {
            cl.Occurred(this);
        });
    }

    /**
     * Sub-classes should override this to handle tcp read event.
     *
     * @param object
     */
    protected abstract void OnTCPRead(Object object);

}
