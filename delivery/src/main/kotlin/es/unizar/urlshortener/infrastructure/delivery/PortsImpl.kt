package es.unizar.urlshortener.infrastructure.delivery

import com.google.common.hash.Hashing
import es.unizar.urlshortener.core.HashService
import es.unizar.urlshortener.core.URIReachableService
import es.unizar.urlshortener.core.ValidatorService
import org.apache.commons.validator.routines.UrlValidator
import java.nio.charset.StandardCharsets
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

private const val CONNECTION_TIMEOUT = 3000L

/**
 * Implementation of the port [ValidatorService].
 */
class ValidatorServiceImpl : ValidatorService {
    override fun isValid(url: String) = urlValidator.isValid(url)

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
 * Implementation of the port [URIReachableService].
 */
class URIReachableServiceImpl : URIReachableService {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = CONNECTION_TIMEOUT
        }
    }
    override fun isReachable(url: String): Boolean {
        val response: HttpResponse?
        runBlocking {
            response = try { client.get(url) }
            catch (e: Exception) { null }
        }
        return response?.status == HttpStatusCode.OK
    }
}
