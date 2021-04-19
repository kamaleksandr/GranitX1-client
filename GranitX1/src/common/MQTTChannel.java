package common;

import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * @author kamyshev.a
 */
public class MQTTChannel extends AbstractChannel {

    private static final int MAX_PACK_SIZE = 65535000;

    public MqttWireMessage message;

    public MQTTChannel(SocketAddress address) throws Exception {
        super(address);
        buffer = ByteBuffer.allocate(MAX_PACK_SIZE);
    }

    private int RequiredSize(ByteBuffer data) throws Exception {
        int size = data.limit();
        if (size > MAX_PACK_SIZE) {
            size = MAX_PACK_SIZE;
        }
        int first = Byte.toUnsignedInt(data.get());
        byte type = (byte) (first >> 4);
        if ((type < MqttWireMessage.MESSAGE_TYPE_CONNECT)
                || (type > MqttWireMessage.MESSAGE_TYPE_DISCONNECT)) {
            //throw new Exception("MQTT header fail, invalid message type");
        }
        int rem_length = 0, pos = 1;
        for (; pos < 5; pos++) {
            if (size < pos) {
                return -1; // expect more data
            }
            byte val = data.get(pos);
            rem_length |= (val & 0x7fL) << (7 * (pos - 1));
            if ((val & 0x80) == 0) {
                break;
            }
        }
        if (pos++ == 5) { // no terminal flag found
            throw new Exception("MQTT header fail, no terminal flag");
        }
        data.position(0);
        if (size < rem_length + pos) {
            return size - rem_length - pos;
        }
        return rem_length + pos;
    }
    
    @Override
    protected void AfterSSLUnwrap(ByteBuffer data){
        int size;
        while (data.position() > 0) {
            data.flip();
            try {
                size = RequiredSize(data);
            } catch (Exception ex) {
                Error("RequiredSize() fail, " + ex);
                buffer.clear();
                break;
            }
            if (size < 0) { // not enough data
                if (data.limit() - size > MAX_PACK_SIZE) {
                    Error(new Exception("MQTT header fail, remaining length too large"));
                    buffer.clear();
                }
                data.compact();
                break;
            }

            try {
                message = MqttWireMessage.createWireMessage(data.array());
            } catch (MqttException ex) {
                Log("MQTT parsing fail " + ex);
                break;
            }
            Packet();
            message = null;
            data.position(size);
            data.compact();
        }
    }

    @Override
    protected void OnTCPRead(Object object) {
        try {
            Receive(buffer);
        } catch (Exception ex) {
            Error(ex);
        }    
    }

}
