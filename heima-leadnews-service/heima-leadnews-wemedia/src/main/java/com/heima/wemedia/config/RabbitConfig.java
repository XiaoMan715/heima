package com.heima.wemedia.config;

import com.heima.common.constants.RabbitConstants;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Exchange textExchange(){
        return ExchangeBuilder.topicExchange(RabbitConstants.EXCANGE_NAME).durable(true).build();
    }

    @Bean
    public Queue textQueue(){
        return QueueBuilder.durable(RabbitConstants.QUEUE_NAME).build();
    }
    @Bean
    public Binding bingText(@Qualifier("textExchange") Exchange exchange, @Qualifier("textQueue") Queue queue){
        return BindingBuilder.bind(queue).to(exchange).with("text.#").noargs();

    }

}
