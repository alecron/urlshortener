package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import io.ktor.client.request.*

/**
 * Given a key returns a [Redirection] that contains a [URI target][Redirection.target]
 * and an [HTTP redirection mode][Redirection.mode].
 *
 */
interface RedirectUseCase {
    fun redirectTo(key: String): Redirection
}

/**
 * Implementation of [RedirectUseCase].
 */
class RedirectUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
) : RedirectUseCase {
    override fun redirectTo(key: String): Redirection {
        val su : ShortUrl? = shortUrlRepository.findByKey(key)

        val redirection : Redirection = su?.redirection
            ?: throw RedirectionNotFound(key)
        if (!su.properties.validated) {
            // Case url has not been validated yet
            throw UrlNotValidatedYet(redirection.target)
        } else if (su.properties.validated && !su.properties.reachable) {
            // Case url has been validated but it is not reachable
            throw UrlNotReachable(redirection.target)
        }
        return redirection
    }
}

