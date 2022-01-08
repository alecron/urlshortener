package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.Format
import es.unizar.urlshortener.core.ShortUrlProperties
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import es.unizar.urlshortener.core.usecases.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.InputStreamSource
import org.springframework.core.io.Resource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.io.*
import java.lang.System.out
import java.net.URI
import java.nio.file.Files
import java.util.*
import javax.servlet.http.HttpServletRequest

/**
 * The specification of the controller.
 */
interface UrlShortenerController {

    /**
     * Redirects and logs a short url identified by its [id].
     *
     * **Note**: Delivery of use cases [RedirectUseCase] and [LogClickUseCase].
     */
    fun redirectTo(id: String, request: HttpServletRequest): ResponseEntity<Void>
    /**
     * Creates a short url from details provided in [data].
     *
     * **Note**: Delivery of use case [CreateShortUrlUseCase].
     */
    fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut>

    /**
     * Gets info about short url identified by its [id]
     *
     * **Note**: Delivery of use case [InfoShortUrlUseCase]
     */
    fun getInfo(id: String, request: HttpServletRequest): ResponseEntity<List<SimpleClick>>


    //fun csvProcessor(file : MultipartFile, qr : Boolean, request: HttpServletRequest) : ResponseEntity<Resource>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    @field:Schema(description = "URL to be shorten")
    val url: String,
    @field:Schema(description = "Tells if a QR for the URL must be created or not")
    val qr: Boolean? = null,
    @field:Schema(description = "Height of the generated QR")
    val qrHeight: Int? = null,
    @field:Schema(description = "Width of the generated QR")
    val qrWidth: Int? = null,
    @field:Schema(description = "Color of the QR's pattern")
    val qrColor: String? = null,
    @field:Schema(description = "Color of the QR's background")
    val qrBackground: String? = null,
    @field:Schema(description = "Image type of the generated QR", example = "PNG")
    val qrTypeImage: String? = null,
    @field:Schema(description = "Correction level of the QR that has to be generated")
    val qrErrorCorrectionLevel: String? = null,
    @field:Schema(description = "Sponsor of the URL")
    val sponsor: String? = null
)

/**
 * Data returned after the creation of a short url.
 */
data class ShortUrlDataOut(
    val url: URI? = null,
    val qr: URI? = null,
    val properties: Map<String, Any> = emptyMap()
)


/**
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@RestController
class UrlShortenerControllerImpl(
        val redirectUseCase: RedirectUseCase,
        val logClickUseCase: LogClickUseCase,
        val createShortUrlUseCase: CreateShortUrlUseCase,
        @Qualifier("qrtemplate") val template: RabbitTemplate,
        val infoShortUrlUseCase: InfoShortUrlUseCase
) : UrlShortenerController {
    
    @GetMapping("/tiny-{id:.*}")
    @Tag(name = "Short URL", description = "Endpoints that access a single short URL and its information" )
    @Operation(
            summary = "Accesses a short URL",
            description = "Accesses a short URL stored with the given id",
            tags = ["Short URL"],
            responses = [
                ApiResponse(
                        description = "Redirect to the URL",
                        responseCode = "307"
                ),
                ApiResponse(description = "Not found", responseCode = "404"),
                ApiResponse(description = "Internal error", responseCode = "500")
            ]
    )
    override fun redirectTo(@PathVariable
                                @Parameter(description = "The id assigned to the short URL on the repository")
                                id: String, request: HttpServletRequest): ResponseEntity<Void> =
         redirectUseCase.redirectTo(id).let {
            val browser = getClientBrowser(request)
            val platform = getClientPlatform(request)
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr, platform = platform, browser = browser))
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))
        }

    @GetMapping("/{id:.*}-json", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
            summary = "Shows the info of the clicks",
            description = "Shows the info of the clicks of the short URL from the hash given",
            tags = ["Short URL"],
            responses = [
                ApiResponse(
                        description = "Success",
                        responseCode = "200",
                        content = [Content(mediaType = "application/json", schema = Schema(implementation = SimpleClick::class))]
                ),
                ApiResponse(description = "Not found", responseCode = "404"),
                ApiResponse(description = "Internal error", responseCode = "500")
            ]
    )
    override fun getInfo(@PathVariable
                             @Parameter(description = "The id assigned to the short URL on the repository")
                             id: String, request: HttpServletRequest): ResponseEntity<List<SimpleClick>> =
        redirectUseCase.redirectTo(id).let {
            val h = HttpHeaders()
            val response = infoShortUrlUseCase.info(id)
            return ResponseEntity<List<SimpleClick>>(response, h, HttpStatus.OK)
        }


    @PostMapping("/api/link", consumes = [ MediaType.APPLICATION_FORM_URLENCODED_VALUE ])
    @Operation(
            summary = "Creates a short URL",
            description = "Creates a Short URL if it is possible with the data sent in the request",
            tags = ["Short URL"],
            responses = [
                ApiResponse(
                        description = "Created",
                        responseCode = "201",
                        content = [Content(mediaType = "application/json", schema = Schema(implementation = ShortUrlDataOut::class))]
                ),
                ApiResponse(description = "Not found", responseCode = "404"),
                ApiResponse(description = "Internal error", responseCode = "500")
            ]
    )
    override fun shortener(data: ShortUrlDataIn, request: HttpServletRequest): ResponseEntity<ShortUrlDataOut> =
        createShortUrlUseCase.create(
            url = data.url,
            data = ShortUrlProperties(
                ip = request.remoteAddr,
                sponsor = data.sponsor
            )
        ).let {
            val h = HttpHeaders()
            val url = linkTo<UrlShortenerControllerImpl> { redirectTo(it.hash, request) }.toUri()
            h.location = url
            var response : ShortUrlDataOut
            if (data.qr != null && data.qr == true){    //Si se pide generar el qr
                //Fijar los valores del formato: usando lo pasado en parámetros y lo establecido por defecto.
                var format = Format()
                val height: Int = data.qrHeight ?: format.height
                val width: Int = data.qrWidth ?: format.width
                val color: String = data.qrColor ?: format.color
                val background: String = data.qrBackground ?: format.background
                val typeImage: String = data.qrTypeImage ?: format.typeImage
                val errorCorrectionLevel: String = data.qrErrorCorrectionLevel ?: format.errorCorrectionLevel
                format = Format(height, width, color, background, typeImage, errorCorrectionLevel)

                //Encolar tarea de generar el codigo qr con el formato especificado usando rabbitmq
                template?.convertAndSend("QR_exchange", "QR_routingKey", QRCode2(it.hash, format))

                //Url del código qr
                val qr = linkTo<QRControllerImpl> { redirectTo(it.hash) }.toUri()
                response = ShortUrlDataOut(
                    url = url,
                    qr = qr,
                    properties = mapOf(
                        "safe" to it.properties.safe
                    )
                )
            } else{
                response = ShortUrlDataOut(
                    url = url,
                    properties = mapOf(
                        "safe" to it.properties.safe
                    )
                )
            }
            ResponseEntity<ShortUrlDataOut>(response, h, HttpStatus.CREATED)
        }


    fun getClientBrowser(request: HttpServletRequest): String? {
        val browserDetails = request.getHeader("User-Agent")
        val user = if(browserDetails.isNullOrBlank()) null else browserDetails.toLowerCase()
        var browser:String? = null

        //println(browserDetails)

        if(user.isNullOrBlank()){
            browser = null
        }else if (user.contains("msie")) {
            val substring: String = browserDetails.substring(browserDetails.indexOf("MSIE")).split(";").get(0)
            browser = substring.split(" ").get(0).replace("MSIE", "IE") + "-" + substring.split(" ").get(1)
        } else if (user.contains("safari") && user.contains("version")) {
            browser = browserDetails.substring(browserDetails.indexOf("Safari")).split(" ").get(0).split(
                "/"
            ).get(0) + "-" + browserDetails.substring(
                browserDetails.indexOf("Version")
            ).split(" ").get(0).split("/").get(1)
        } else if (user.contains("opr") || user.contains("opera")) {
            if (user.contains("opera")) browser =
                browserDetails.substring(browserDetails.indexOf("Opera")).split(" ").get(0)
                    .split(
                        "/"
                    ).get(0) + "-" + browserDetails.substring(
                    browserDetails.indexOf("Version")
                ).split(" ").get(0).split("/").get(1) else if (user.contains("opr")) browser =
                browserDetails.substring(browserDetails.indexOf("OPR")).split(" ").get(0)
                    .replace(
                        "/",
                        "-"
                    ).replace(
                        "OPR", "Opera"
                    )
        } else if (user.contains("chrome")) {
            browser = browserDetails.substring(browserDetails.indexOf("Chrome")).split(" ").get(0).replace("/", "-")
        } else if (user.indexOf("mozilla/7.0") > -1 || user.indexOf("netscape6") !== -1 || user.indexOf(
                "mozilla/4.7"
            ) !== -1 || user.indexOf("mozilla/4.78") !== -1 || user.indexOf(
                "mozilla/4.08"
            ) !== -1 || user.indexOf("mozilla/3") !== -1
        ) {
            browser = "Netscape-?"
        } else if (user.contains("firefox")) {
            browser = browserDetails.substring(browserDetails.indexOf("Firefox")).split(" ").get(0).replace("/", "-")
        } else if (user.contains("rv")) {
            browser = "IE"
        } else {
            browser = "UnKnown, More-Info: $browserDetails"
        }
        return browser
    }

    fun getClientPlatform(request: HttpServletRequest): String? {
        val browserDetails = request.getHeader("User-Agent")

        val lowerCaseBrowser = if(browserDetails.isNullOrBlank()) null else browserDetails.toLowerCase()
        return if(lowerCaseBrowser.isNullOrBlank()){
            null
        }else if (lowerCaseBrowser.contains("windows")) {
            "Windows"
        } else if (lowerCaseBrowser.contains("mac")) {
            "Mac"
        } else if (lowerCaseBrowser.contains("x11")) {
            "Unix"
        } else if (lowerCaseBrowser.contains("android")) {
            "Android"
        } else if (lowerCaseBrowser.contains("iphone")) {
            "IPhone"
        } else {
            "UnKnown, More-Info: $browserDetails"
        }
    }
}
