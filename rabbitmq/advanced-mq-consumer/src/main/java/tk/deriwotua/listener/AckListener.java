package tk.deriwotua.listener;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

/**
 * Consumer ACK机制：默认就是自动签收
 *  1. 设置手动签收。acknowledge="manual"
 *  2. 自动签收时可以让监听类实现MessageListener接口onMessage()方法
 *      手动签收是调用channel的方法实现手动签收测试就不能实现MessageListener接口onMessage()
 *      这里需要让监听器类实现ChannelAwareMessageListener接口onMessage()方法会附带channel参数
 *  3. 如果消息成功处理，则调用channel的 basicAck()签收
 *  4. 如果消息处理失败，则调用channel的basicNack()拒绝签收，broker重新发送给consumer
 */
@Component
public class AckListener implements ChannelAwareMessageListener {

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            //1.接收转换消息
            System.out.println(new String(message.getBody()));

            //2. 处理业务逻辑
            System.out.println("处理业务逻辑...");
            int i = 3/0;//出现错误
            //3. 手动签收
            channel.basicAck(deliveryTag,true);
        } catch (Exception e) {
            /*
             * 4.拒绝签收
             *  第三个参数：requeue：重回队列。如果设置为true，则消息重新回到queue，broker会重新发送该消息给消费端
             */
            channel.basicNack(deliveryTag,true,true);
            // 单条拒绝签收
            //channel.basicReject(deliveryTag,true);
        }
    }
}
