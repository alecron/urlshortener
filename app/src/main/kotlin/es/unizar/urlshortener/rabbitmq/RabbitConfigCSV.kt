package es.unizar.urlshortener.rabbitmq

import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory


@Configuration
class RabbitConfigCSV : RabbitListenerConfigurer {

    //Based on: https://www.youtube.com/watch?v=o4qCdBR4gUM&ab_channel=JavaTechie
    // Copied on the CsvPublisherController
    val QUEUE = "csvqueue"
    val EXCHANGE = "csvexchange"
    val ROUTING_KEY = "csvqueue_routingKey"

    @Bean
    fun csvqueue() = Queue(QUEUE)

    @Bean
    fun csvexchange() = TopicExchange(EXCHANGE)

    @Bean
    fun csvbinding() = BindingBuilder.bind(csvqueue()).to(csvexchange()).with(ROUTING_KEY)

    @Bean
    fun csvconverter() = Jackson2JsonMessageConverter()

    @Bean(name=["csvtemplate"])
    fun csvtemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = csvconverter()
        return rabbitTemplate
    }

    override fun configureRabbitListeners(registrar: RabbitListenerEndpointRegistrar?) {
        registrar?.setMessageHandlerMethodFactory(messageHandlerMethodFactory())
    }

    @Bean
    fun messageHandlerMethodFactory() : MessageHandlerMethodFactory {
        val messageHandlerMethodFactory = DefaultMessageHandlerMethodFactory()
        messageHandlerMethodFactory.setMessageConverter(consumerConverter())
        return messageHandlerMethodFactory
    }

    @Bean
    fun consumerConverter() = MappingJackson2MessageConverter()

}