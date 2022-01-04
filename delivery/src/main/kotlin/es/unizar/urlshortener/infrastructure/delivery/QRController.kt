package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.Format
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import es.unizar.urlshortener.core.usecases.QRUrlUseCase


/**
 * The specification of the controller.
 */
interface QRController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, format: Format): ResponseEntity<ByteArray>
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

    @GetMapping("/qr/{id}", produces = [ MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.APPLICATION_JSON_VALUE ])
    override fun redirectTo(@PathVariable id: String, format: Format): ResponseEntity<ByteArray> =
        qrUrlUseCase.generateQR(id, format).let{
            ResponseEntity.status(HttpStatus.OK).body(it)
        }
}