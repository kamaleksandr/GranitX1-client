package granitX1.client;

import EAV.proto.Protobuf;
import EAV.Entity;
import com.google.protobuf.ByteString;
import common.AbstractTask;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.*;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.internal.wire.*;

/**
 *
 * @author kamyshev.a
 */
public class Task extends AbstractTask {

    private ByteBuffer request_data;
    public String name;
    public int mqtt_type;
    public Protobuf.Task.Commands command;
    public LinkedList<Entity> entities;
    public int messID;

    private Task(MqttWireMessage message) throws MqttException {
        setRequestData(message);
    }

    /**
     * @return the request_data
     */
    public ByteBuffer getRequestData() {
        SetRequested();
        return request_data;
    }

    private void setRequestData(MqttWireMessage message) throws MqttException {
        byte[] header = message.getHeader();
        byte[] pl = message.getPayload();
        request_data = ByteBuffer.allocate(header.length + pl.length);
        request_data.put(header);
        request_data.put(pl);
        request_data.flip();
        mqtt_type = message.getType();
    }

    public void setLoaded() {
        status = StatusEnum.done;
    }

    public static Task createConnect(String clientID,
            String user, String pass) throws Exception {
        MqttWireMessage message = new MqttConnect(
                clientID, MQTT_VERSION_3_1, true, 300,
                user, pass.toCharArray(), null, null);
        Task task = new Task(message);
        task.name = "CONNECT";
        task.mqtt_type = MqttWireMessage.MESSAGE_TYPE_CONNECT;
        return task;
    }

    public static Task createSubscribe(String[] names, int[] qos) throws MqttException {
        Task task = new Task(new MqttSubscribe(names, qos));
        task.name = "SUBSCRIBE";
        task.mqtt_type = MqttWireMessage.MESSAGE_TYPE_SUBSCRIBE;
        return task;
    }

    public static Task createPuback(MqttPublish publish) throws MqttException {
        Task task = new Task(new MqttPubAck(publish));
        task.name = "PUBACK";
        task.mqtt_type = MqttWireMessage.MESSAGE_TYPE_PUBACK;
        return task;
    }

    public static Task createPing() throws MqttException {
        Task task = new Task(new MqttPingReq());
        task.name = "PINGREQ";
        task.mqtt_type = MqttWireMessage.MESSAGE_TYPE_PINGREQ;
        return task;
    }

    public static Task createTask(Protobuf.Task.Commands command,
            LinkedList<Entity> entities, int messID) throws Exception {
        Protobuf.Task.Builder task_builder = Protobuf.Task.newBuilder();
        task_builder.setCommand(command);

        entities.forEach((entity) -> {
            Protobuf.Entity.Builder entity_builder = Protobuf.Entity.newBuilder();
            if (entity.num != 0) {
                entity_builder.setNum(entity.num);
            }
            entity_builder.setType(entity.type);
            entity.attributes.forEach((attribute) -> {
                Protobuf.Attribute.Builder attribute_builder
                        = Protobuf.Attribute.newBuilder();
                attribute_builder.setType(attribute.type);
                attribute_builder.setValue(ByteString.copyFrom(
                        attribute.value.getValueAsBuffer()));
                entity_builder.addAttributes(attribute_builder);
            });
            task_builder.addEntities(entity_builder);
        });

        MqttMessage message = new MqttMessage(task_builder.build().toByteArray());
        MqttWireMessage wire_message = new MqttPublish("standart/task", message);
        wire_message.setMessageId(messID);
        Task task = new Task(wire_message);
        task.name = "PUBLISH";
        task.mqtt_type = MqttWireMessage.MESSAGE_TYPE_PUBLISH;
        task.command = command;
        task.entities = entities;
        task.messID = messID;
        return task;
    }
}
