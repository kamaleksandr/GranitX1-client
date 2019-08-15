package common;

import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import javax.swing.event.ChangeEvent;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 *
 * @author kamyshev.a
 */
public class MQTTChannel extends AbstractChannel {

    private static final int MAX_PACK_SIZE = 65535;
    private static final int REC_BUF_SIZE = 65535;

    private MqttWireMessage message = null;

    public MQTTChannel(SocketAddress address) throws Exception {
        super(address);
        buf = ByteBuffer.allocate(REC_BUF_SIZE);
    }
    
    /**
     * @return the mess
     */
    public MqttWireMessage getMessage() {
        return message;
    }    

    private int RequiredSize(int offset) throws Exception {
        int size = buf.position() - offset;
        if (size > MAX_PACK_SIZE) {
            size = MAX_PACK_SIZE;
        }
        if (size == 0) {
            return 0;
        }
        int first = Byte.toUnsignedInt(buf.get(offset));
        byte type = (byte)(first >> 4);
        if ((type < MqttWireMessage.MESSAGE_TYPE_CONNECT)
                || (type > MqttWireMessage.MESSAGE_TYPE_DISCONNECT)) {
            throw new Exception("MQTT header fail, invalid message type");
        }
        int rem_length = 0, pos = 1;
        for (; pos < 5; pos++) {
            if (size < pos) {
                return -1; // expect more data
            }
            byte val = buf.get(offset + pos);
            rem_length |= (val & 0x7f) << (7 * (pos - 1));
            if ((val & 0x80) == 0) {
                break;
            }
        }
        if (pos++ == 5) { // no terminal flag found
            throw new Exception("MQTT header fail, no terminal flag");
        }
        if (size < rem_length + pos) {
            return size - rem_length - pos;
        }
        return rem_length + pos;
    }

    @Override
    protected void OnTCPRead(ChangeEvent event) {
        try {
            buf = Receive(buf);
        } catch (Exception ex) {
            Error(ex);
            return;
        }
        if (buf.position() == 0) {
            setStatus(StatusEnum.disconnect);
            return;
        }
        int pos = 0, size;
        while (true) {
            try {
                size = RequiredSize(pos);
            } catch (Exception ex) {
                Error(ex);
                break;
            }
            if (size == 0) {
                break;
            }
            if (size < 0) { // not enough data
                if (buf.position() - pos - size > MAX_PACK_SIZE) {
                    Error(new Exception("MQTT header fail, remaining length too large"));
                    break;
                }
                if (pos != 0) {       
                    buf.compact();
                    return;
                }
            }
            byte[] data = new byte[size];
            System.arraycopy(buf.array(), pos, data, 0, size);
            try {
                message = MqttWireMessage.createWireMessage(data);                                                                            
            } catch (MqttException ex) {
                Error(ex);
                break;
            }
            Packet();
            pos += size;
        }
        buf.clear();
    }

}
