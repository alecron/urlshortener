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
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
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
    fun getInfo(id: String, request: HttpServletRequest): ResponseEntity<String>


    fun csvProcessor(file : MultipartFile, qr : Boolean, request: HttpServletRequest) : ResponseEntity<Resource>
}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val qr: Boolean? = null,
    val qrHeight: Int? = null,
    val qrWidth: Int? = null,
    val qrColor: String? = null,
    val qrBackground: String? = null,
    val qrTypeImage: String? = null,
    val qrErrorCorrectionLevel: String? = null,
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
    val infoShortUrlUseCase: InfoShortUrlUseCase,
    val createCsvUseCase: CreateCsvUseCase,
    private val validatorService: ValidatorService
) : UrlShortenerController {

    @Autowired
    private val template: RabbitTemplate? = null

    @GetMapping("/tiny-{id:.*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> =
         redirectUseCase.redirectTo(id).let {
            val browser = getClientBrowser(request)
            val platform = getClientPlatform(request)
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr, platform = platform, browser = browser))
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))
        }


    @GetMapping("/{id:.*}-json")
    override fun getInfo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<String> =
        redirectUseCase.redirectTo(id).let {
            val h = HttpHeaders()
            val response = infoShortUrlUseCase.info(id)
            return ResponseEntity<String>(response, h, HttpStatus.OK)
        }
           

    @PostMapping("/api/link", consumes = [ MediaType.APPLICATION_FORM_URLENCODED_VALUE ])
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
                val qr = linkTo<QRControllerImpl> { redirectTo(it.hash, request) }.toUri()
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

    @PostMapping("/csv")
    override fun csvProcessor(@RequestParam("file") file : MultipartFile, @RequestParam("qr") qr : Boolean, request: HttpServletRequest) : ResponseEntity<Resource>  {
        if(file.isEmpty){
            throw EmptyFile(file.name)
        } else{
            val reader = BufferedReader(InputStreamReader(file.inputStream))
            val csvParser = CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(',') )
            val byteArrayOutputStream = ByteArrayOutputStream()
            val writer = BufferedWriter(OutputStreamWriter(byteArrayOutputStream))
            val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withDelimiter(',') )

            val firstURL = generarCsv(csvParser, csvPrinter, request, qr)

            val fileInputStream = InputStreamResource(ByteArrayInputStream(byteArrayOutputStream.toByteArray()))
            val h = HttpHeaders()
            h.location = firstURL
            h.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=uriFile.csv")
            h.set(HttpHeaders.CONTENT_TYPE, "text/csv")
            return ResponseEntity(
                    fileInputStream,
                    h,
                    HttpStatus.OK
            )
        }
    }

    fun getClientBrowser(request: HttpServletRequest): String? {
        val browserDetails: String = request.getHeader("User-Agent")
        val user: String = browserDetails.toLowerCase()
        var browser = ""

        println(browserDetails)

        if (user.contains("msie")) {
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
        val browserDetails: String = request.getHeader("User-Agent")

        val lowerCaseBrowser: String = browserDetails.toLowerCase()
        return if (lowerCaseBrowser.contains("windows")) {
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

    private fun generarCsv(csvParser: CSVParser, csvPrinter: CSVPrinter, request: HttpServletRequest, qr: Boolean): URI? {
        var firstURL: URI? = null

        csvParser.map { it.map { it }.map {
                createCsvUseCase.transform(it, request.remoteAddr)
            }.map {
                // Si es shortURL -> lista de los elementos que se guardan
                // Si no se guarda la propia cadena y ya
                if (it is ShortUrlCSV) {
                    val urlHash = it.shortUrl.hash
                    val uriRecord = linkTo<UrlShortenerControllerImpl> { redirectTo(urlHash, request) }.toString()
                    var qrRecord = ""

                    if(qr) qrRecord = linkTo<QRControllerImpl> { redirectTo(urlHash, request) }.toString()

                    if(firstURL == null){
                        // Se guarda la primera URI acortada
                        firstURL = linkTo<UrlShortenerControllerImpl> { redirectTo(urlHash, request) }.toUri()
                    }
                    csvPrinter.printRecord(it.url, uriRecord, "", qrRecord)
                } else if(it is String) {
                    csvPrinter.printRecord(it.split(','))
                }
            }
        }

        csvPrinter.flush()
        csvPrinter.close()

        return firstURL
    }

}
