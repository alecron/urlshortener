package es.unizar.urlshortener.rabbitmq

import es.unizar.urlshortener.core.QRCode
import es.unizar.urlshortener.core.QRCode2
import es.unizar.urlshortener.core.QRCodeRepositoryService
import es.unizar.urlshortener.core.QRService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.lang.Thread.sleep

@Component
class RabbitConsumerQR(
        private val qrService: QRService,
        private val qrCodeRepository: QRCodeRepositoryService
){
    /**
     * It consumes the QR queue and generates a QR with the given data. The generated QR is
     * stored in the QR code repository
     */
    @RabbitListener(queues = ["QR_queue"])
    fun consumeMessageFromQueue(qrCode: QRCode2) {
        println("Message recieved from queue : ${qrCode.hash}")
        val qr = qrService.generateQR("http://localhost:8080/tiny-${qrCode.hash}", qrCode.format)
        //sleep(60000) //1minuto -> Prueba para ver si genera el codigo QR en el momento cuando no está
        //Añadir en db
        qrCodeRepository.save(QRCode(qrCode.hash, qrCode.format, qr))
        println("QR code saved at repository: ${qrCode.hash}")
    }
}