package granitX1.client;

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

    private Task(MqttWireMessage message) throws MqttException {
       setRequestData(message); 
    }

    /**
     * @return the request_data
     */
    public ByteBuffer getRequestData() {
        return request_data;
    }
    
    private void setRequestData(MqttWireMessage message) throws MqttException{
        byte[] header = message.getHeader();
        byte[] pl = message.getPayload();
        request_data = ByteBuffer.allocate(header.length + pl.length);
        request_data.put(header);
        request_data.put(pl);
        request_data.flip();
    }

    public static Task createConnect(
            String clientID, String user, String pass) throws Exception {
        MqttWireMessage message = new MqttConnect(
                clientID, MQTT_VERSION_3_1, true, 300,
                user, pass.toCharArray(), null, null);
        return new Task(message);
    }
    
    public static Task createPuback(MqttPublish publish) throws MqttException{
      return new Task(new MqttPubAck(publish));
    }
    
    public static Task createPing() throws MqttException{
      return new Task(new MqttPingReq());
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
                if (attribute.num != 0) {
                    attribute_builder.setNum(attribute.num);
                }
                attribute_builder.setType(attribute.type);
                attribute_builder.setValue(ByteString.copyFrom(attribute.getValueAsBuffer()));
                entity_builder.addAttributes(attribute_builder);
            });
            task_builder.addEntities(entity_builder);
        });
        
        MqttMessage message = new MqttMessage(task_builder.build().toByteArray());
        MqttWireMessage wire_message = new MqttPublish("standart/task", message);
        wire_message.setMessageId(messID);
        return new Task(wire_message);
    }

}
