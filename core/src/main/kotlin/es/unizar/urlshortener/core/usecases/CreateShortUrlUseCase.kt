package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import java.util.Date
import java.util.concurrent.CompletableFuture

/**
 * Given an url returns the key that is used to create a short URL.
 * When the url is created optional data may be added.
 *
 * **Note**: This is an example of functionality.
 */
interface CreateShortUrlUseCase {
    fun create(url: String, data: ShortUrlProperties): ShortUrl
}

/**
 * Implementation of [CreateShortUrlUseCase].
 */
class CreateShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService,
    private val validatorService: ValidatorService,
    private val hashService: HashService
) : CreateShortUrlUseCase {
    override fun create(url: String, data: ShortUrlProperties): ShortUrl {
        if (!validatorService.isValid(url)) {
            throw InvalidUrlException(url)
        }
        val id: String = hashService.hasUrl(url)
        val reachable = validatorService.isReachable(url)
        val su = ShortUrl(
            hash = id,
            redirection = Redirection(target = url),
            properties = ShortUrlProperties(
                safe = data.safe,
                ip = data.ip,
                sponsor = data.sponsor,
            )
        )
        val suReturned = shortUrlRepository.save(su)
        // Once it has been checked if the url is reachable,
        // reachable and validated fields of the shortUrl are updated
        reachable.handleAsync { _, _ ->
            val shortUrlSaved: ShortUrl? = shortUrlRepository.findByKey(id)
            if (shortUrlSaved != null) {
                if (reachable.isCompletedExceptionally) {
                    // In case an exception has occurred while reaching the remote server
                    shortUrlSaved.properties.reachable = false
                } else {
                    // If no exception has occurred but answer is not ready yet
                    // properties.reachable is set to false
                    shortUrlSaved.properties.reachable = reachable.getNow(false)
                }
                shortUrlSaved.properties.validated = true
                shortUrlRepository.save(shortUrlSaved)
            }
        }
        return suReturned
    }
}