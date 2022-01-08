package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.net.URI
import javax.servlet.http.HttpServletRequest

interface ProgressListener {
    fun onProgress(value: Int)
    fun onCompletion()
}

fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy { LoggerFactory.getLogger(this::class.java.name) }
}

// Fuente: https://lankydan.dev/documenting-a-spring-rest-api-following-the-openapi-specification
@RestController
@RequestMapping("/csv")
@Tag(name = "CSV", description = "Controller for downloading a user's generated CSV" )
class CsvPubController(
        val csvUrlRepositoryService: CsvUrlRepositoryService
) {
    @GetMapping("/download")
    @Operation(
            summary = "Downloads a CSV of Short URLs",
            description = "Downloads the CSV generated for the job with the given UUID",
            tags = ["CSV"],
            responses = [
                ApiResponse(
                        description = "Success",
                        responseCode = "200",
                        content = [Content(mediaType = "text/csv")]
                ),
                ApiResponse(description = "Not found", responseCode = "404"),
                ApiResponse(description = "Internal error", responseCode = "500")
            ]
    )
    fun csvDownload(@RequestParam("uuid")
                    @Parameter(description = "The Id assigned to the user's CSV")
                    id: String, request: HttpServletRequest) : ResponseEntity<Resource>{
        val byteArrayOutputStream = ByteArrayOutputStream()
        val writer = BufferedWriter(OutputStreamWriter(byteArrayOutputStream))
        val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withDelimiter(',') )

        val uris = csvUrlRepositoryService.findAllByuuid(id)
        var firstURL:URI? = null


        uris.map{

            if(it.urlHash != ""){
                if(firstURL == null) firstURL = linkTo<UrlShortenerControllerImpl> { redirectTo(it.urlHash, request) }.toUri()
                it.urlHash = linkTo<UrlShortenerControllerImpl> { redirectTo(it.urlHash, request) }.toString()
            }
            csvPrinter.printRecord(it.originalUri, it.urlHash, it.comment, it.qrRecord)
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