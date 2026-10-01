package com.selfdriveconnect.bookingservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Uber/Rapido lean heavily on event streaming (Kafka, in their case)
 * for cross-service coordination instead of only synchronous
 * request/response calls — a trip status change fans out to
 * matching, notifications, analytics, etc. via events, not a chain of
 * direct HTTP calls. This project adopts the same pattern at a much
 * smaller scale using RabbitMQ (a topic exchange) instead of Kafka —
 * the right tool for this data volume, same underlying idea.
 *
 * One topic exchange, "sdc.events", shared by every service that
 * publishes or consumes domain events. Each service declares its own
 * queue(s) bound to the routing keys it cares about — booking-service
 * cares about payment outcomes, so it binds a queue to "payment.*".
 */
@Configuration
public class RabbitMQConfig {

    public static final String EVENTS_EXCHANGE = "sdc.events";
    public static final String PAYMENT_EVENTS_QUEUE = "booking-service.payment-events";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentEventsQueue() {
        return new Queue(PAYMENT_EVENTS_QUEUE, true);
    }

    @Bean
    public Binding paymentEventsBinding(Queue paymentEventsQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(paymentEventsQueue).to(eventsExchange).with("payment.*");
    }

    @Bean
    public MessageConverter messageConverter() {
        // JSON, not Java serialization — messages must be readable by
        // any service regardless of language/version, same principle
        // as REST DTOs elsewhere in this project.
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
