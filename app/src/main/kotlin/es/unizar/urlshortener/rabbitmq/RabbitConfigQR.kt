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
class RabbitConfigQR {

    //Based on: https://www.youtube.com/watch?v=o4qCdBR4gUM&ab_channel=JavaTechie
    val QUEUE = "QR_queue"
    val EXCHANGE = "QR_exchange"
    val ROUTING_KEY = "QR_routingKey"

    @Bean
    fun qrqueue() = Queue(QUEUE)

    @Bean
    fun qrexchange() = TopicExchange(EXCHANGE)

    @Bean
    fun qrbinding() = BindingBuilder.bind(qrqueue()).to(qrexchange()).with(ROUTING_KEY)

    @Bean
    fun qrconverter() = Jackson2JsonMessageConverter()

    @Bean(name=["qrtemplate"])
    fun qrtemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = qrconverter()
        return rabbitTemplate
    }
}