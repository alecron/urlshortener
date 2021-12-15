package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/test")
class CsvPubController(
        val template: RabbitTemplate
) {
    val QUEUE = "csvqueue"
    val EXCHANGE = "csvexchange"
    val ROUTING_KEY = "csvqueue_routingKey"

    @PostMapping("")
    fun csvUpload(@RequestParam("file") file : MultipartFile, @RequestParam("qr") qr : Boolean, request: HttpServletRequest) : String {//ResponseEntity<Resource> {

        if(file.isEmpty){
            throw EmptyFile(file.name)
        } else {
            val reader = BufferedReader(InputStreamReader(file.inputStream))
            val csvParser = CSVParser(reader, CSVFormat.DEFAULT.withDelimiter(','))


            val listOfURL = csvParser.map { it.map { it }}

            template.convertAndSend(EXCHANGE, ROUTING_KEY,  listOfURL)

            //TODO: How to return the analyzed CSV to the user
            return "success"
        }
    }
}