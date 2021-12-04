package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.Format
import es.unizar.urlshortener.core.ShortUrlProperties
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.LogClickUseCase
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase
import es.unizar.urlshortener.core.usecases.RedirectUseCase
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
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

}

/**
 * Data required to create a short url.
 */
data class ShortUrlDataIn(
    val url: String,
    val qr: Boolean? = null,
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
    private val validatorService: ValidatorService
) : UrlShortenerController {

    @GetMapping("/tiny-{id:.*}")
    override fun redirectTo(@PathVariable id: String, request: HttpServletRequest): ResponseEntity<Void> =
        redirectUseCase.redirectTo(id).let {
            logClickUseCase.logClick(id, ClickProperties(ip = request.remoteAddr))
            val h = HttpHeaders()
            h.location = URI.create(it.target)
            ResponseEntity<Void>(h, HttpStatus.valueOf(it.mode))
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
            if (data.qr != null){
                val qr = linkTo<QRControllerImpl> { redirectTo(it.hash, Format(), request) }.toUri()
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
    fun csvProcessor(@RequestParam("file") file : MultipartFile, request: HttpServletRequest) : ResponseEntity<Resource>  {
        if(file.isEmpty){
            throw EmptyFile(file.name)
        } else{

            val reader = BufferedReader(InputStreamReader(file.inputStream))
            val csvParser = CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(',') )
            val byteArrayOutputStream = ByteArrayOutputStream()
            val writer = BufferedWriter(OutputStreamWriter(byteArrayOutputStream))
            val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withDelimiter(',') )

            var firstURL: URI? = null

            //Se leen todos los registros del fichero y se guarda la primera URI
            for(csvRecord in csvParser) {
                //En caso de que haya mas de una url por linea recorremos todas
                for(record in 0 until csvRecord.size()) {
                    val urlRecord = csvRecord.get(record)
                    var uriRecord = ""
                    var commentRecord = ""
                    if(!validatorService.isValid(urlRecord)){
                        commentRecord = "La URI no es valida "
                    } else {
                       val shorRecord = createShortUrlUseCase.create(
                                url = csvRecord.get(0),
                                data = ShortUrlProperties(
                                        ip = request.remoteAddr,
                                        sponsor = null
                                )
                       )
                        uriRecord = linkTo<UrlShortenerControllerImpl> { redirectTo(shorRecord.hash, request) }.toString()
                        if(firstURL == null){
                            // Se guarda la primera URI acortada
                            firstURL = linkTo<UrlShortenerControllerImpl> { redirectTo(shorRecord.hash, request) }.toUri()
                        }
                    }
                    csvPrinter.printRecord(urlRecord, uriRecord, commentRecord)
                    println("Linea: " + urlRecord + " -- " + uriRecord + " -- " + commentRecord)
                }
            }
            csvPrinter.flush()
            csvPrinter.close()

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

}
