package granitX1.client;

import EAV.*;
import EAV.proto.Protobuf;
import common.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.TreeMap;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.wire.*;

/**
 * Granit X1 client
 *
 * @author kamyshev.a
 */
public class Client extends AbstractClient {

    private static final int CHANNEL_COUNT = 1;
    private static final int PING_INTERVAL = 180;
    private int ping_timer = 0, messID = 0;
    public Integer port, profile_number;
    public String address, client_id, password;
    public TreeMap<Integer, String> EntityDescriptors;
    public TreeMap<Integer, AttributeDescriptor> AttributeDescriptors;
    public final EAV.EntitiesMap users;
    public final EAV.EntitiesMap devices;

    public Client() {
        address = "localhost";
        port = 8883;
        client_id = "test";
        password = "test";
        EntityDescriptors = new TreeMap<>();
        AttributeDescriptors = new TreeMap<>();
        devices = new EntitiesMap();
        users = new EntitiesMap();
        while (channels.size() < CHANNEL_COUNT) {
            channels.add(null);
        }
        keep_connect = true;
    }

    public void AddTask(Task task) {
        ping_timer = 0;
        synchronized (lock) {
            ListIterator<AbstractTask> it_task = tasks.listIterator();
            while (it_task.hasNext()) {
                Task.StatusEnum status = it_task.next().getStatus();
                if (status == Task.StatusEnum.exspired
                        || status == Task.StatusEnum.done) {
                    it_task.remove();
                }
            }
            tasks.add(task);
            Log("Task '" + task.name + "' is queued");
        }
    }

    @Override
    protected final void OnMainTimer() {
        if (++ping_timer > PING_INTERVAL) {
            try {
                AddTask(Task.createPing());
            } catch (MqttException ex) {
                Log(ex);
            }
        }
        Maintenance();
    }

    private void AddEntityDescriptor(Protobuf.Entity pe) throws Exception {
        ListIterator<Protobuf.Attribute> it_pa
                = pe.getAttributesList().listIterator();
        while (it_pa.hasNext()) {
            Protobuf.Attribute pa = it_pa.next();
            if (pa.getNum() == 0 && pa.getType() == 0) {
                EntityDescriptors.put(pe.getNum(), pa.getValue().toStringUtf8());
            }
        }
    }

    private void AddAttributeDescriptor(Protobuf.Entity pe) throws Exception {
        ListIterator<Protobuf.Attribute> it_pa
                = pe.getAttributesList().listIterator();
        String name = null;
        Byte vt = null;
        while (it_pa.hasNext()) {
            Protobuf.Attribute pa = it_pa.next();
            if (pa.getNum() == 0) {
                switch (pa.getType()) {
                    case 0:
                        name = pa.getValue().toStringUtf8();
                        break;
                    case 1:
                        ByteBuffer buf = pa.getValue().asReadOnlyByteBuffer();
                        if (buf.limit() == 1) {
                            vt = buf.get();
                        }
                        break;
                }
            }
        }
        if (name != null && vt != null) {
            AttributeDescriptors.put(
                    pe.getNum(), new AttributeDescriptor(name, vt));
        }
    }

    private void PutEntityToMap(Protobuf.Entity pe, EAV.EntitiesMap map) throws Exception {
        DualKey key = new DualKey(pe.getNum(), pe.getType());
        ListIterator<Protobuf.Attribute> it_pa = pe.getAttributesList().listIterator();
        EAV.AttributesMap attributes = map.get(key);
        if (attributes == null) {
            attributes = new EAV.AttributesMap();
            map.put(key, attributes);
        }
        while (it_pa.hasNext()) {
            Protobuf.Attribute pa = it_pa.next();
            key = new DualKey(pa.getNum(), pa.getType());
            AttributeDescriptor ad = AttributeDescriptors.get(key.type);
            if (ad == null) {
                continue;
            }
            ByteBuffer buf = pa.getValue().asReadOnlyByteBuffer();
            buf.order(ByteOrder.LITTLE_ENDIAN);
            switch (ad.valueType) {
                case i8:
                    if (buf.limit() == 1) {
                        attributes.put(key, new AttributeValue(buf.get()));
                    }
                    break;
                case i16:
                    if (buf.limit() == 2) {
                        attributes.put(key, new AttributeValue(buf.getShort()));
                    }
                    break;
                case i32:
                    if (buf.limit() == 4) {
                        attributes.put(key, new AttributeValue(buf.getInt()));
                    }
                    break;
                case i64:
                    if (buf.limit() == 8) {
                        attributes.put(key, new AttributeValue(buf.getLong()));
                    }
                    break;
                case f4:
                    if (buf.limit() == 4) {
                        attributes.put(key, new AttributeValue(buf.getFloat()));
                    }
                    break;
                case f8:
                    if (buf.limit() == 8) {
                        attributes.put(key, new AttributeValue(buf.getDouble()));
                    }
                    break;
                case s:
                    CharBuffer cb = StandardCharsets.UTF_8.decode(buf);
                    //String s = pa.getValue().toStringUtf8();
                    attributes.put(key, new AttributeValue(cb.toString()));
                    break;
                case b:
                    attributes.put(key, new AttributeValue(buf.duplicate()));
                    break;
            }
        }
    }

    private void IncomingPublish(MqttPublish publish) throws Exception {
        Protobuf.Task proto = Protobuf.Task.parseFrom(publish.getPayload());
        ListIterator<Protobuf.Entity> it_pe = proto.getEntitiesList().listIterator();
        Protobuf.Entity pe;
        boolean is_descriptors = false;
        boolean is_profiles = false; 
        while (it_pe.hasNext()) {
            pe = it_pe.next();
            switch (pe.getType()) {
                case 0:
                    AddEntityDescriptor(pe);
                    is_descriptors = true;
                    break;
                case 1:
                    AddAttributeDescriptor(pe);
                    is_descriptors = true;
                    break;
                case 2:
                    is_profiles = true;
                    PutEntityToMap(pe, users);
                    break;
                case 3:
                    is_profiles = true;
                    PutEntityToMap(pe, devices);
                    break;
            }

        }
        if (is_descriptors) {
            Log("Entity descriptors received, total " + EntityDescriptors.size());
            Log("Attribute descriptors received, total " + AttributeDescriptors.size());
        }
        if (is_profiles) {
            Log("Profiles received, total users " + users.size() + ", devices " + devices.size());
        }        
    }

    @Override
    protected final void OnChannelPacket(Object object) {
        synchronized (lock) {
            MQTTChannel channel = (MQTTChannel) object;

            //Log(channel.message);

            switch (channel.message.getType()) {
                case MqttWireMessage.MESSAGE_TYPE_CONNACK:
                    MqttConnack connack = (MqttConnack) channel.message;
                    if (connack.getReturnCode() != 0) {
                        Log("CONNACK return code " + connack.getReturnCode());
                        return;
                    }
                    if (channel.task != null) {
                        Task task = (Task) channel.task;
                        if (task.mqtt_type == MqttWireMessage.MESSAGE_TYPE_CONNECT) {
                            task.setLoaded();
                            OnConnect.forEach((el) -> {
                                el.Occurred(task);
                            });
                        }
                    }
                    break;
                case MqttWireMessage.MESSAGE_TYPE_PUBACK: {
                    MqttPubAck puback = (MqttPubAck) channel.message;
                    if (channel.task != null) {
                        Task task = (Task) channel.task;
                        if (task.messID == puback.getMessageId()) {
                            task.setLoaded();
                        }
                    }
                    break;
                }
                case MqttWireMessage.MESSAGE_TYPE_PUBLISH: {
                    MqttPublish publish = (MqttPublish) channel.message;
                    try {
                        if (publish.getMessage().getQos() == 1) {
                            Task puback = Task.createPuback(publish);
                            channel.Send(puback.getRequestData());
                            AddTask(puback);
                        }
                        IncomingPublish(publish);
                    } catch (Exception ex) {
                        Log(ex);
                    }
                    break;
                }
                case MqttWireMessage.MESSAGE_TYPE_PINGRESP: {
                    if (channel.task != null) {
                        Task task = (Task) channel.task;
                        if (task.mqtt_type == MqttWireMessage.MESSAGE_TYPE_PINGREQ) {
                            task.setLoaded();
                        }
                        break;
                    }
                }
            }
            Data(channel.task);
            if (channel.task != null) {
                if (channel.task.getStatus() == Task.StatusEnum.done) {
                    tasks.remove(channel.task);
                }
                channel.task = null;
            }
            SetChannelTask(channel);
        }
    }

    @Override
    protected final void OnChannelStatus(Object object) {
        AbstractChannel channel = (AbstractChannel) object;
        AbstractChannel.StatusEnum status = channel.getStatus();
        if (status == AbstractChannel.StatusEnum.ready) {
            Log("Connection success, authorization, client ID: " + client_id);
            try {
                Task task = Task.createConnect(client_id, "", password);
                channel.task = task;
                channel.Send(task.getRequestData());
                AddTask(task);
            } catch (Exception ex) {
                Log(ex);
            }
        }
    }

    @Override
    protected final AbstractChannel CreateChannel() {
        try {
            Log("Connect to " + address + ":" + port.toString());
            AbstractChannel channel
                    = new MQTTChannel(new InetSocketAddress(address, port));
            channel.setSslEnabled(true);
            return channel;
        } catch (Exception ex) {
            Log(ex);
            return null;
        }
    }

    @Override
    protected ByteBuffer getRequestData(AbstractTask task) throws Exception {
        return ((Task) task).getRequestData();
    }

    public void CreateEntities(LinkedList<Entity> entities, String name) throws Exception {
        Task task = Task.createTask(
                Protobuf.Task.Commands.CREATE_ENTITY, entities, messID++);
        task.name = name;
        AddTask(task);
        Maintenance();
    }

}
