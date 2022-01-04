package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
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

@RestController
@RequestMapping("/csv")
class CsvPubController(
        val csvUrlRepositoryService: CsvUrlRepositoryService

) {
    @GetMapping("/download")
    fun csvDownload(@RequestParam("uuid") id: String, request: HttpServletRequest) : ResponseEntity<Resource>{
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