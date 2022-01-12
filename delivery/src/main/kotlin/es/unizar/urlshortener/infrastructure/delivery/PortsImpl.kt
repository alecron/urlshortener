package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.client.j2se.MatrixToImageConfig
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.CharacterSetECI
import com.google.zxing.qrcode.QRCodeWriter
import es.unizar.urlshortener.core.*
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.ValidatorService
import org.apache.commons.validator.routines.UrlValidator
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import javax.imageio.ImageIO
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Async
import java.util.concurrent.CompletableFuture

private const val CONNECTION_TIMEOUT = 3000L

/**
 * Implementation of the port [ValidatorService].
 */
open class ValidatorServiceImpl : ValidatorService {
    override fun isValid(url: String) = urlValidator.isValid(url)

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = CONNECTION_TIMEOUT
        }
    }
    /**
     * Check if the url [url] is reachable
     * @return A CompletableFuture<Boolean> whose value will be true
     * if and only if an HTTP GET petition to the url returns code [HttpStatusCode.OK]
     * within the first [CONNECTION_TIMEOUT] seconds
     */
    @Async("taskExecutorReachable")
    open override fun isReachable(url : String) : CompletableFuture<Boolean> {
        val response: HttpResponse?
        runBlocking {
            response = try { client.get(url) }
            catch (e: Exception) { null }
        }
        return CompletableFuture.completedFuture(response?.status == HttpStatusCode.OK)
    }

    companion object {
        val urlValidator = UrlValidator(arrayOf("http", "https"))
    }
}

/**
 * Implementation of the port [HashService].
 */
@Suppress("UnstableApiUsage")
class HashServiceImpl : HashService {
    override fun hasUrl(url: String) = Hashing.murmur3_32().hashString(url, StandardCharsets.UTF_8).toString()
}


/**
 * Implementation of the port [QRService]
 */
class QRServiceImpl : QRService {
    override fun generateQR(url: String, format: Format): ByteArray {
        // Check Size
        if (format.height <= 0 || format.width <= 0) {
            throw InvalidQRParameter("Height and width must be greater than 0")
        }
        // Check colors
        val hexRegex = Regex("0x[0-9a-fA-F]{8}")
        if (!format.color.matches(hexRegex) || !format.background.matches(hexRegex)) {
            throw InvalidQRParameter("Colors must be in hexadecimal format.")
        }
        // Check response type
        if (!arrayListOf("PNG", "JPEG").contains(format.typeImage)){
            throw InvalidQRParameter("The image type must be 'PNG' or 'JPEG'.")
        }
        // Add options
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.ERROR_CORRECTION] = format.errorCorrectionLevel
        hints[EncodeHintType.CHARACTER_SET] = CharacterSetECI.UTF8
        // We can add other parameters as margin
        //val margin = 5
        //hints[EncodeHintType.MARGIN] = if (format.height > format.width) 100 * margin / format.height else 100 * margin / format.width

        val qrImage: BufferedImage
        try {
            val color = format.color.substring(2).toLong(16).toInt()
            val background = format.background.substring(2).toLong(16).toInt()
            val qrCodeWriter = QRCodeWriter()
            qrImage = MatrixToImageWriter.toBufferedImage(
                    qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, format.width, format.height, hints),
                    MatrixToImageConfig(color, background)
            )
        } catch (e: WriterException) {
            throw QRFailure("QR encoding")
        }

        try {
            val byteArrayOutputStream = ByteArrayOutputStream()
            ImageIO.write(qrImage, format.typeImage, byteArrayOutputStream)
            return byteArrayOutputStream.toByteArray()
        } catch (e: IOException) {
            throw QRFailure("QR image generation")
        }
    }
}


