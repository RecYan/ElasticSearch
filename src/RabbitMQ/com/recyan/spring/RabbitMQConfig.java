package com.reyan.spring;

import com.reyan.spring.adapter.MessageDelegate;
import com.reyan.spring.convert.ImageMessageConverter;
import com.reyan.spring.convert.PDFMessageConverter;
import com.reyan.spring.convert.TextMessageConverter;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
@ComponentScan({"com.reyan.spring.*"})
public class RabbitMQConfig {

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses("192.168.80.100:5672");
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");
        connectionFactory.setVirtualHost("/");
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        return rabbitAdmin;
    }

    /**
     * 针对消费者配置
     * 1. 设置交换机类型
     * 2. 将队列绑定到交换机
     * FanoutExchange: 将消息分发到所有的绑定队列，无routingkey的概念
     * HeadersExchange ：通过添加属性key-value匹配
     * DirectExchange:按照routingkey分发到指定队列
     * TopicExchange:多关键字匹配
     */
    @Bean
    public TopicExchange exchange001() {
        return new TopicExchange("topic001", true, false);
    }

    @Bean
    public Queue queue001() {
        return new Queue("queue001", true); //队列持久
    }

    @Bean
    public Binding binding001() {
        return BindingBuilder.bind(queue001()).to(exchange001()).with("spring.*");
    }

    @Bean
    public TopicExchange exchange002() {
        return new TopicExchange("topic002", true, false);
    }

    @Bean
    public Queue queue002() {
        return new Queue("queue002", true); //队列持久
    }

    @Bean
    public Binding binding002() {
        return BindingBuilder.bind(queue002()).to(exchange002()).with("rabbit.*");
    }

    @Bean
    public Queue queue003() {
        return new Queue("queue003", true); //队列持久
    }

    @Bean
    public Binding binding003() {
        return BindingBuilder.bind(queue003()).to(exchange001()).with("mq.*");
    }

    @Bean
    public Queue queue_image() {
        return new Queue("image_queue", true); //队列持久
    }

    @Bean
    public Queue queue_pdf() {
        return new Queue("pdf_queue", true); //队列持久
    }

    @Bean
    public Queue test_direct_queue() {
        return new Queue("test_direct_queue", true); //队列持久
    }


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        return rabbitTemplate;
    }

    // 监听容器 -- 适配器 -- 转换器
    @Bean
    public SimpleMessageListenerContainer messageContainer(ConnectionFactory connectionFactory) {

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(queue001(), queue002(), queue003(), queue_image(), queue_pdf(), test_direct_queue()); //设置监听的队列
        container.setConcurrentConsumers(1); //设置当前的消费端数量
        container.setMaxConcurrentConsumers(10); //设置最大的消费端数量
        container.setDefaultRequeueRejected(false); // 是否重回队列
        container.setAcknowledgeMode(AcknowledgeMode.AUTO); // 签收模式 自动签收

        //设置消费端唯一的标签
        container.setConsumerTagStrategy(queue -> queue + "_" + UUID.randomUUID().toString());

        /*// 对监听的队列消息 进行相应处理 -- 监听器
        container.setMessageListener((ChannelAwareMessageListener) (message, channel) -> {
            String msg = new String(message.getBody());
            System.err.println("----------消费者: " + msg);
        });*/

        //用消息适配器 监听消息队列
        /** 1 适配器方式. 默认是有自己的方法名字的：handleMessage
         // 可以自己指定一个方法的名字: consumeMessage
         // 也可以添加一个转换器: 从字节数组转换为String
         */
//        MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
//        adapter.setDefaultListenerMethod("consumeMessage");
//        adapter.setMessageConverter(new TextMessageConverter()); //接受参数为String类型的形参 进行消息监听
//        container.setMessageListener(adapter); //用适配器 去监听队列

        /** 2 适配器方式: 我们的队列名称 和 方法名称 也可以进行一一的匹配
         *
         */
        /*MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
        HashMap<String, String> queueOrTagToMethodName = new HashMap<>();
        queueOrTagToMethodName.put("queue001", "method1"); //队列名称 和 方法名称 进行绑定匹配
        queueOrTagToMethodName.put("queue002", "method2");
        adapter.setQueueOrTagToMethodName(queueOrTagToMethodName);
        adapter.setMessageConverter(new TextMessageConverter()); //接受参数为String类型的形参 进行消息监听
        container.setMessageListener(adapter); //用适配器 去监听队列*/



        // 1.1 支持json格式的转换器
        /**
         MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
         adapter.setDefaultListenerMethod("consumeMessage");

         Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
         adapter.setMessageConverter(jackson2JsonMessageConverter);

         container.setMessageListener(adapter);
         */



        // 1.2 DefaultJackson2JavaTypeMapper & Jackson2JsonMessageConverter 支持java对象转换
        /**
         MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
         adapter.setDefaultListenerMethod("consumeMessage");

         Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();

         DefaultJackson2JavaTypeMapper javaTypeMapper = new DefaultJackson2JavaTypeMapper();
         jackson2JsonMessageConverter.setJavaTypeMapper(javaTypeMapper);

         adapter.setMessageConverter(jackson2JsonMessageConverter);
         container.setMessageListener(adapter);
         */


        //1.3 DefaultJackson2JavaTypeMapper & Jackson2JsonMessageConverter 支持java对象多映射转换
        /**
         MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
         adapter.setDefaultListenerMethod("consumeMessage");
         Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
         DefaultJackson2JavaTypeMapper javaTypeMapper = new DefaultJackson2JavaTypeMapper();

         Map<String, Class<?>> idClassMapping = new HashMap<String, Class<?>>();
         idClassMapping.put("order", com.recyan.spring.entity.Order.class); //order 为order对象的标签
         idClassMapping.put("packaged", com.recyan.spring.entity.Packaged.class);

         javaTypeMapper.setIdClassMapping(idClassMapping);

         jackson2JsonMessageConverter.setJavaTypeMapper(javaTypeMapper);
         adapter.setMessageConverter(jackson2JsonMessageConverter);
         container.setMessageListener(adapter);
         */

        //1.4 ext convert

        MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
        adapter.setDefaultListenerMethod("consumeMessage");

        //全局的转换器:
        ContentTypeDelegatingMessageConverter convert = new ContentTypeDelegatingMessageConverter();

        TextMessageConverter textConvert = new TextMessageConverter();
        convert.addDelegate("text", textConvert);
        convert.addDelegate("html/text", textConvert);
        convert.addDelegate("xml/text", textConvert);
        convert.addDelegate("text/plain", textConvert);

        Jackson2JsonMessageConverter jsonConvert = new Jackson2JsonMessageConverter();
        convert.addDelegate("json", jsonConvert);
        convert.addDelegate("application/json", jsonConvert);

        ImageMessageConverter imageConverter = new ImageMessageConverter();
        convert.addDelegate("image/png", imageConverter);
        convert.addDelegate("image", imageConverter);

        PDFMessageConverter pdfConverter = new PDFMessageConverter();
        convert.addDelegate("application/pdf", pdfConverter);


        adapter.setMessageConverter(convert);
        container.setMessageListener(adapter);


        return container;
    }



}