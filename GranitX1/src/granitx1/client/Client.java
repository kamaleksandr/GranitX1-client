package granitX1.client;

import EAV.Attribute;
import EAV.Entity;
import common.AbstractChannel;
import common.AbstractClient;
import common.AbstractTask;
import common.MQTTChannel;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import javax.swing.event.ChangeEvent;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPublish;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

/**
 *
 * @author kamyshev.a
 */
public class Client extends AbstractClient {

    private static final int CHANNEL_COUNT = 2;
    private static final int PING_INTERVAL = 10;
    private int ping_timer = 0;
    private int messID;
    private final SocketAddress address;

    public Client(SocketAddress address) {
        this.address = address;
        while (channels.size() < CHANNEL_COUNT) {
            channels.add(null);
        }
    }

    @Override
    protected final void OnMainTimer() {
        if ( ++ping_timer > PING_INTERVAL ){
            ping_timer = 0;
            try{
            tasks.add(Task.createPing());
            } catch(MqttException ex){
                Error(ex);
            }
        }
        Maintenance();
    }

    @Override
    protected final void OnChannelPacket(ChangeEvent event) {
        MQTTChannel channel = (MQTTChannel) event.getSource();
        MqttWireMessage message = channel.getMessage();

        Error(new Exception(message.toString()));

        if (message.getType() == MqttWireMessage.MESSAGE_TYPE_PUBLISH) {
            try {
                tasks.add(Task.createPuback((MqttPublish) message));
                byte[] payload = message.getPayload();
                //Mqtt_message mess = Mqtt_message.parseFrom(payload);

            } catch (MqttException ex) {
                Error(ex);
            }

        }
        channel.task = null;
        SetChannelTask(channel);
    }

    @Override
    protected final void OnChannelStatus(ChangeEvent event) {
        AbstractChannel channel = (AbstractChannel) event.getSource();
        AbstractChannel.StatusEnum status = channel.getStatus();
        if (status == AbstractChannel.StatusEnum.ready) {
            try {
                Task task = Task.createConnect("client_id", "user", "password");
                channel.task = task;
                channel.Send(task.getRequestData());
            } catch (Exception ex) {
                Error(ex);
            }
        }
    }

    @Override
    protected final AbstractChannel CreateChannel() {
        try {
            AbstractChannel channel = new MQTTChannel(address);
            channel.setSslEnabled(true);
            return channel;
        } catch (Exception ex) {
            Error(ex);
            return null;
        }
    }

    public void AddTask() throws Exception {

        Entity entity = new Entity(0, 0);
        entity.attributes.add(new Attribute(0, 0, 500));
        LinkedList<Entity> entities = new LinkedList<>();
        entities.add(entity);
        Task task = Task.createTask(
                Protobuf.Task.Commands.READ_ENTITY, entities, messID++);
        tasks.add(task);
        Maintenance();
    }

    @Override
    protected ByteBuffer getRequestData(AbstractTask task) throws Exception {
        return ((Task) task).getRequestData();
    }

}
