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
        private val uRIReachableService : URIReachableService,
        private val qrService: QRService,
        private val qrCodeRepository: QRCodeRepositoryService
) : QRUrlUseCase {
    override fun generateQR(id: String, format: Format): ByteArray {
        //Check id/hash
        val redirection: Redirection = shortUrlRepository.findByKey(id)?.redirection
                ?: throw RedirectionNotFound(id)
        if (!uRIReachableService.isReachable(redirection.target)) {
            throw UrlNotReachable(redirection.target)
        }
        return qrCodeRepository.findByKey(id)?.qrCode
                ?: throw RedirectionNotFound(id)
    }

}
