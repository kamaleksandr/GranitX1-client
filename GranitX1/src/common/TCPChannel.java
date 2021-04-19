package common;

/**
 * @author kamyshev.a
 */
import common.custom.EventListener;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.LinkedList;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

public class TCPChannel {

    public enum StatusEnum {
        created, started, ready, disconnect, error, stoped, closed
    };

    public final LinkedList<EventListener> OnRead;
    public final LinkedList<EventListener> OnStatus;
    public final LinkedList<EventListener> OnLog;

    private static final int CONN_TIMEOUT_MS = 5000;
    private static final int HANDSHAKE_TIMEOUT_MS = 5000;
    private StatusEnum status;
    private final Selector selector;
    private final Object lock;
    private final SocketChannel channel;
    private final SocketAddress address;
    private final Thread thread;
    private SSLClient ssl;
    private int ssl_buf_size;
    private ByteBuffer sslBuffer;

    public TCPChannel(SocketAddress address) throws Exception {
        OnRead = new LinkedList<>();
        OnStatus = new LinkedList<>();
        OnLog = new LinkedList<>();
        lock = new Object();
        thread = new Thread(mainTask);
        this.address = address;
        channel = SocketChannel.open();
        channel.configureBlocking(false);
        selector = Selector.open();
        status = StatusEnum.created;
    }

    public StatusEnum getStatus() {
        synchronized (lock) {
            return status;
        }
    }

    protected void setStatus(StatusEnum status) {
        synchronized (lock) {
            this.status = status;
        }
        OnStatus.forEach((el) -> {
            el.Occurred(this);
        });
    }

    /**
     * @param enabled
     * @throws java.lang.Exception
     */
    public final void setSslEnabled(boolean enabled) throws Exception {
        if (enabled == true) {
            ssl = new SSLClient();
            ssl_buf_size = ssl.getBufSize();
            sslBuffer = ByteBuffer.allocate(ssl_buf_size);
        } else {
            ssl = null;
        }
    }

    protected void Log(Object o) {
        OnLog.forEach((el) -> {
            el.Occurred(o);
        });
    }

    protected void Error(Object object) {
        OnLog.forEach((el) -> {
            el.Occurred(object);
        });
        setStatus(StatusEnum.error);
    }

    private boolean Handshake() throws Exception {
        ssl.BeginHandshake();
        ByteBuffer out_data = ByteBuffer.allocate(ssl.getBufSize());
        ByteBuffer in_data = ByteBuffer.allocate(ssl.getBufSize());
        while (status == StatusEnum.started) {
            switch (ssl.getStatus()) {
                case NEED_UNWRAP:
                    if (in_data.position() == 0) {
                        if (selector.select(HANDSHAKE_TIMEOUT_MS) < 1) {
                            throw new Exception("Handshake timeout");
                        }
                        selector.selectedKeys().clear();
                        if (channel.read(in_data) == -1) {
                            setStatus(StatusEnum.disconnect);
                            break;
                        }
                    }
                    in_data.flip();
                    out_data.clear();
                    try {
                        ssl.Unwrap(in_data, out_data);
                    } catch (SSLException ex) {
                        Error(ex);
                        return false;
                    }
                    in_data.compact();
                    break;

                case NEED_WRAP:
                    out_data.clear();
                    try {
                        ssl.Wrap(in_data, out_data);
                    } catch (SSLException ex) {
                        Error(ex);
                        return false;
                    }
                    out_data.flip();
                    while (out_data.hasRemaining()) {
                        channel.write(out_data);
                        break;
                    }
                    break;
                case NEED_TASK:
                    ssl.DelegateTask();
                    break;
                case FINISHED:
                    return true;
                case NOT_HANDSHAKING:
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    private void DoTask() throws Exception {
        synchronized (lock) {
            channel.register(selector, SelectionKey.OP_CONNECT);
            channel.connect(address);
        }
        int res = selector.select(CONN_TIMEOUT_MS);
        if (getStatus() == StatusEnum.stoped) {
            return;
        }
        if (res == 0) {
            throw new Exception("Connect timeout");
        }
        synchronized (lock) {
            channel.finishConnect();
            channel.register(selector, SelectionKey.OP_READ);
            if (ssl != null) {
                if (!Handshake()) {
                    throw new Exception("Handshake failed");
                }
            }
        }
        setStatus(StatusEnum.ready);
        while (true) {
            if (getStatus() != StatusEnum.ready) {
                break;
            }
            if (selector.select(0) > 0) {
                OnRead.forEach((el) -> {
                    el.Occurred(this);
                });
                selector.selectedKeys().clear();
            }
        }
    }

    Runnable mainTask = () -> {
        try {
            DoTask();
        } catch (Exception ex) {
            Error(ex);
        }
    };

    /**
     * Starts the receiving thread.
     *
     * @throws Exception
     */
    public void Open() throws Exception {
        StatusEnum _status = getStatus();
        if (_status != StatusEnum.created) {
            throw new Exception("Status error('" + _status + "'), unable to open");
        }
        setStatus(StatusEnum.started);
        thread.start();
    }

    protected void AfterSSLUnwrap(ByteBuffer data) {
    }

    /**
     * Receive buffer from channel.
     *
     * @param buffer Receiving buffer
     * @throws Exception
     */
    public void Receive(ByteBuffer buffer) throws Exception {
        if (ssl == null) {
            synchronized (lock) {
                    if (channel.read(buffer) < 1) {
                        setStatus(StatusEnum.disconnect);
                    }
            }
            return;
        }
        synchronized (lock) {
                if (channel.read(sslBuffer) < 1) {
                    setStatus(StatusEnum.disconnect);
                }
        }
        int pos = -1;
            while (pos != sslBuffer.position()) {
                sslBuffer.flip();
                pos = sslBuffer.position();
                if (!sslBuffer.hasRemaining()) {
                    break;
                }
                try {
                    ssl.Unwrap(sslBuffer, buffer);
                } catch (Exception ex) {
                    if (ssl.result.getStatus() != SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                        Log("Unwrap " + ex);
                    }
                    sslBuffer.compact();
                    break;
                }
                AfterSSLUnwrap(buffer);
                sslBuffer.compact();
            }
    }

    /**
     * Send data to the channel.
     *
     * @param data Data for send
     * @throws java.lang.Exception
     */
    public void Send(ByteBuffer data) throws Exception {
        if (ssl != null) {
            ByteBuffer buffer = ByteBuffer.allocate(data.limit() + ssl_buf_size);
            ssl.Wrap(data, buffer);
            data = buffer;
            data.flip();
        }
        synchronized (lock) {
            StatusEnum _status = getStatus();
            if (_status != StatusEnum.ready) {
                throw new Exception("Status error('" + _status + "'), unable to send");
            }
            while (data.hasRemaining()) {
                channel.write(data);
            }
        }
    }

    /**
     * Wake up and complete the receiving thread, close the channel.
     *
     * @throws Exception
     */
    public void Close() throws Exception {
        StatusEnum _status = getStatus();
        if (_status == StatusEnum.stoped || _status == StatusEnum.closed) {
            return;
        }
        setStatus(StatusEnum.stoped);
        selector.wakeup();
        thread.join();
        selector.close();
        channel.close();
        setStatus(StatusEnum.closed);
    }

}
