package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.Format
import es.unizar.urlshortener.core.QRCode2
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.QRUrlUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest


/**
 * The specification of the controller.
 */
interface QRController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, height: Int, width: Int, color: String, background: String,
                   typeImage: String, errorCorrectionLevel: String, request: HttpServletRequest): ResponseEntity<ByteArray>
}

/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
class QRControllerImpl(
    //Add logClick
    val qrUrlUseCase : QRUrlUseCase
) : QRController {

    @Autowired
    private val template: RabbitTemplate? = null

    @GetMapping("/qr/{id}", produces = [ MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.APPLICATION_JSON_VALUE ])
    override fun redirectTo(@PathVariable id: String,
                            @RequestParam(required = false, defaultValue = "500") height: Int,
                            @RequestParam(required = false, defaultValue = "500") width: Int,
                            @RequestParam(required = false, defaultValue = "0xFF000000") color: String,
                            @RequestParam(required = false, defaultValue = "0xFFFFFFFF") background: String,
                            @RequestParam(required = false, defaultValue = "PNG") typeImage: String,
                            @RequestParam(required = false, defaultValue = "L") errorCorrectionLevel: String,
                            request: HttpServletRequest): ResponseEntity<ByteArray> {
        val format = Format(height, width, color, background, typeImage, errorCorrectionLevel)
        template?.convertAndSend("QR_exchange", "QR_routingKey", QRCode2(id, format))
        qrUrlUseCase.generateQR(id, format).let{
            return ResponseEntity.status(HttpStatus.OK).body(it)
        }
    }
}