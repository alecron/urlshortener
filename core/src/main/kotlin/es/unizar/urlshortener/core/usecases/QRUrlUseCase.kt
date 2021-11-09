package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

/**
 * Given an id returns a [ByteArray] that contains the QR code
 *
 * **Note**: This is an example of functionality.
 */
interface QRUrlUseCase {
    fun generateQR(id: String, format: Format): ByteArray
}

/**
 * Implementation of [QRUrlUseCase].
 */
class QRUrlUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService,
        private val qrService: QRService
) : QRUrlUseCase {
    override fun generateQR(id: String, format: Format): ByteArray =
        //Check id/hash
        if (shortUrlRepository.findByKey(id) != null)
            qrService.generateQR("http://localhost:8080/tiny-$id", format)
        else
            throw RedirectionNotFound(id)
}
