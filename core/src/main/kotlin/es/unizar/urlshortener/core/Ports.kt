package es.unizar.urlshortener.core


import java.util.concurrent.CompletableFuture

/**
 * [ClickRepositoryService] is the port to the repository that provides persistence to [Clicks][Click].
 */
interface ClickRepositoryService {
    fun save(cl: Click): Click
    fun findAllByHash(hash: String): List<Click>
}

/**
 * [ShortUrlRepositoryService] is the port to the repository that provides management to [ShortUrl][ShortUrl].
 */
interface ShortUrlRepositoryService {
    fun findByKey(id: String): ShortUrl?
    fun save(su: ShortUrl): ShortUrl
}

/**
 * [QRCodeRepositoryService] is the port to the repository that provides management to [QRCode][QRCode].
 */
interface QRCodeRepositoryService {
    fun findByKey(id: String): QRCode?
    fun save(qrCode: QRCode): QRCode
}

/**
 * [ValidatorService] is the port to the service that validates if an url can be shortened.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface ValidatorService {
    fun isValid(url: String): Boolean
    fun isReachable(url : String) : CompletableFuture<Boolean>
}

/**
 * [HashService] is the port to the service that creates a hash from a URL.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface HashService {
    fun hasUrl(url: String): String
}

/**
 * [QRService] is the port to the service that generates a qr from a short URL and a format.
 *
 * **Note**: It is a design decision to create this port. It could be part of the core .
 */
interface QRService{
    fun generateQR(url: String, format: Format): ByteArray
}



