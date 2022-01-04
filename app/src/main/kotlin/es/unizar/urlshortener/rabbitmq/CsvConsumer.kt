package es.unizar.urlshortener.rabbitmq

import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.usecases.CreateCsvUseCase
import es.unizar.urlshortener.infrastructure.delivery.QRControllerImpl
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.hateoas.server.mvc.linkTo
import org.springframework.stereotype.Component

@Component
class CsvConsumer(
        val createCsvUseCase: CreateCsvUseCase,
        private val csvUrlRepositoryService: CsvUrlRepositoryService
) {
    @RabbitListener(queues = ["csvqueue"])
    fun consumeMessage(recibo: ShortUrlCSVRabbit){
        val processed = createCsvUseCase.transform(recibo.url, recibo.remoteAddr)
        if(processed is ShortUrlCSV){
            val urlHash = processed.shortUrl.hash

            var qrRecord = ""
            if (recibo.qr != null && recibo.qr!!) {
                qrRecord = linkTo<QRControllerImpl> { redirectTo(urlHash, Format()) }.toString()
            }

            val csvurl = CsvUrl(
                uuid = recibo.id,
                urlHash = urlHash,
                originalUri = recibo.url,
                comment = "",
                qrRecord = qrRecord
            )
            csvUrlRepositoryService.save(csvurl)

        } else if(processed is String){
            val csvurl = CsvUrl(
                    uuid = recibo.id,
                    urlHash = "",
                    originalUri = recibo.url,
                    comment = processed,
                    qrRecord = ""
            )
            csvUrlRepositoryService.save(csvurl)
        }

    }
}