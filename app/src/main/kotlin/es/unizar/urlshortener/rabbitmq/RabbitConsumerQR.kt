package es.unizar.urlshortener.rabbitmq

import es.unizar.urlshortener.core.QRCode
import es.unizar.urlshortener.core.QRCode2
import es.unizar.urlshortener.core.QRCodeRepositoryService
import es.unizar.urlshortener.core.QRService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Component
class RabbitConsumerQR(
        private val qrService: QRService,
        private val qrCodeRepository: QRCodeRepositoryService
){
    @RabbitListener(queues = ["QR_queue"])
    fun consumeMessageFromQueue(qrCode: QRCode2) {
        println("Message recieved from queue : ${qrCode.hash}")
        val qr = qrService.generateQR("http://localhost:8080/tiny-${qrCode.hash}", qrCode.format)
        //Añadir directorio
        val path: Path = Paths.get(System.getProperty("user.dir") + "/qrGenerated/tiny-${qrCode.hash}.png")
        println("QR code generated at "+path)
        Files.write(path, qr)
        //Añadir en db
        qrCodeRepository.save(QRCode(qrCode.hash, qrCode.format, qr))
        println("QR code saved at repository: ${qrCode.hash}")
    }
}