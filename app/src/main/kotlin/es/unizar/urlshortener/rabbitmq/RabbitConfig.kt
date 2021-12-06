package es.unizar.urlshortener.rabbitmq

import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class RabbitConfig {

    //Based on: https://www.youtube.com/watch?v=o4qCdBR4gUM&ab_channel=JavaTechie
    // Copied on the CsvPublisherController
    val QUEUE = "csvqueue"
    val EXCHANGE = "csvexchange"
    val ROUTING_KEY = "csvqueue_routingKey"

    @Bean
    fun queue() = Queue(QUEUE)

    @Bean
    fun exchange() = TopicExchange(EXCHANGE)

    @Bean
    fun binding() = BindingBuilder.bind(queue()).to(exchange()).with(ROUTING_KEY)

    @Bean
    fun converter() = Jackson2JsonMessageConverter()

    @Bean
    fun template(connectionFactory: ConnectionFactory): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = converter()
        return rabbitTemplate
    }
}