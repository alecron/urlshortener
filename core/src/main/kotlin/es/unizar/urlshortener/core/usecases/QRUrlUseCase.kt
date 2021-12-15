package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

/**
 * Given an id returns a [ByteArray] that contains the QR code
 *
 * **Note**: This is an example of functionality.
 */
interface QRUrlUseCase {
    fun generateQR(id: String): ByteArray
}

/**
 * Implementation of [QRUrlUseCase].
 */
class QRUrlUseCaseImpl(
        private val shortUrlRepository: ShortUrlRepositoryService,
        private val validatorService: ValidatorService,
        private val qrService: QRService,
        private val qrCodeRepository: QRCodeRepositoryService
) : QRUrlUseCase {
    override fun generateQR(id: String): ByteArray {
        //Check id/hash
        val su : ShortUrl? = shortUrlRepository.findByKey(id)
        val redirection : Redirection = su?.redirection
                ?: throw RedirectionNotFound(id)
        //Check url is reachable
        if (!su.properties.reachable) {
            throw UrlNotReachable(redirection.target)
        }
        //if the hash exists in the db, the program returns the qr code stored in the db
        //in other case, the program generates the qr code with the default format
        return qrCodeRepository.findByKey(id)?.qrCode
                ?: qrService.generateQR("http://localhost:8080/tiny-$id", Format())
    }

}
