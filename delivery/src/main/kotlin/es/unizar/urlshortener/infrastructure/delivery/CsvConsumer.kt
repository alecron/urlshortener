package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.Format
import es.unizar.urlshortener.core.ShortUrlCSV
import es.unizar.urlshortener.core.usecases.CreateCsvUseCase
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.URI

@Component
class CsvConsumer(
        val createCsvUseCase: CreateCsvUseCase
) {
    @RabbitListener(queues = ["csvqueue"])
    fun consumeMessage(uris: List<List<String>>){
        println("LISTA RECIBIDA: " + uris)

        var firstURL: URI? = null

        val byteArrayOutputStream = ByteArrayOutputStream()
        val writer = BufferedWriter(OutputStreamWriter(byteArrayOutputStream))
        val csvPrinter = CSVPrinter(writer, CSVFormat.DEFAULT.withDelimiter(',') )

        uris.map {
            it.map{
                createCsvUseCase.transform(it, "aa")
            }.map{
                // Si es shortURL -> lista de los elementos que se guardan
                // Si no se guarda la propia cadena y ya

                if (it is ShortUrlCSV) {
                    val urlHash = it.shortUrl.hash
                    val uriRecord = linkTo<UrlShortenerControllerImpl> { redirectTo(urlHash, request) }.toString()
                    var qrRecord = ""
                    if(qr) qrRecord = linkTo<QRControllerImpl> { redirectTo(urlHash, Format(), request) }.toString()
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
    }
}