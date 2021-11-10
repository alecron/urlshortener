package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import io.ktor.client.request.*

/**
 * Given a key returns a [Redirection] that contains a [URI target][Redirection.target]
 * and an [HTTP redirection mode][Redirection.mode].
 *
 * **Note**: This is an example of functionality.
 */
interface RedirectUseCase {
    fun redirectTo(key: String): Redirection
}

/**
 * Implementation of [RedirectUseCase].
 */
class RedirectUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val uRIReachableService : URIReachableService
) : RedirectUseCase {
    override fun redirectTo(key: String): Redirection {
        val redirection : Redirection = shortUrlRepository.findByKey(key)?.redirection
            ?: throw RedirectionNotFound(key)
        if (!uRIReachableService.isReachable(redirection.target)) {
            throw UrlNotReachable(redirection.target)
        }
        return redirection
    }
}

