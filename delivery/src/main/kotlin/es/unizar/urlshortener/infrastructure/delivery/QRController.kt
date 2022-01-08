package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.Format
import es.unizar.urlshortener.core.QRCode2
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.QRUrlUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
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
    fun redirectTo(id: String): ResponseEntity<ByteArray>
}

/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
@Tag(name = "QR", description = "Controller to access a QR" )
class QRControllerImpl(
    val qrUrlUseCase : QRUrlUseCase
) : QRController {

    @GetMapping("/qr/{id}", produces = [ MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE, MediaType.APPLICATION_JSON_VALUE ])
    @Operation(
            summary = "Access a generated QR",
            description = "Access the generated QR for the Short URL with the hash given on the id parameter",
            tags = ["QR"],
            responses = [
                ApiResponse(
                        description = "Success",
                        responseCode = "200",
                        content = [Content(mediaType = "image/png"), Content(mediaType = "image/jpeg"), Content(mediaType = "application/json")]
                ),
                ApiResponse(description = "Not found", responseCode = "404"),
                ApiResponse(description = "Internal error", responseCode = "500")
            ]
    )
    override fun redirectTo(@PathVariable
                                @Parameter(description = "The Id assigned to the Short URL related to the QR")
                                id: String): ResponseEntity<ByteArray> =
        qrUrlUseCase.generateQR(id).let{
            ResponseEntity.status(HttpStatus.OK).body(it)
        }
}
