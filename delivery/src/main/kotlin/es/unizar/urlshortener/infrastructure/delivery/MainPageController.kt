package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.ClickProperties
import es.unizar.urlshortener.core.Format
import es.unizar.urlshortener.core.ShortUrlProperties
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.InputStreamSource
import org.springframework.core.io.Resource
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
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
 * The implementation of the controller.
 *
 * **Note**: Spring Boot is able to discover this [RestController] without further configuration.
 */
@Controller
class MainPageController(
        val sseRepository: SseRepository,
        val csvUrlRepositoryService: CsvUrlRepositoryService,
        val template: RabbitTemplate
){

    val QUEUE = "csvqueue"
    val EXCHANGE = "csvexchange"
    val ROUTING_KEY = "csvqueue_routingKey"


    @GetMapping("/")
    fun index(model: Model): String {
        model["uuid"] = UUID.randomUUID().toString()
        return "index"
    }

    @PostMapping("/csv")
    fun csvUpload(@RequestParam("uuid") id: String, @RequestParam("file") file : MultipartFile, @RequestParam("qrCSV") qr : Boolean?, request: HttpServletRequest) : String{//ResponseEntity<Resource> {
        if(file.isEmpty){
            throw EmptyFile(file.name)
        } else {
            val reader = BufferedReader(InputStreamReader(file.inputStream))
            val csvParser = CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(','))

            var numTotal = 0

            csvParser.map { it.map {
                val toRabbit = ShortUrlCSVRabbit(
                        url = it,
                        id = id,
                        remoteAddr = request.remoteAddr,
                        qr = qr
                )
                template.convertAndSend(EXCHANGE, ROUTING_KEY,  toRabbit)
                numTotal ++
            }}

            val listener = sseRepository.createProgressListener(id)
            var numProcessed = csvUrlRepositoryService.countByUuid(id).toInt()

            while(numProcessed != numTotal){
                sleep()
                numProcessed = csvUrlRepositoryService.countByUuid(id).toInt()
                val progress = (numProcessed/numTotal) * 100

                listener.onProgress(progress)
            }

            listener.onCompletion()
        }

       return  "index"
    }

    fun sleep() {
        try {
            Thread.sleep(50)
        } catch (e: InterruptedException) {
        }
    }
}
