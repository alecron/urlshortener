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
        } else {
            val id: String = hashService.hasUrl(url)
            val reachable = validatorService.isReachable(url)
            // Case url already stored and not reachable
            val shortUrlAlreadySaved: ShortUrl? = shortUrlRepository.findByKey(id)
            if(shortUrlAlreadySaved != null) {
                if (shortUrlAlreadySaved.properties.validated  &&
                    !shortUrlAlreadySaved.properties.reachable) {
                    throw UrlNotReachable(url)
                }
                return shortUrlAlreadySaved
            } else {
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

                reachable.handleAsync { _, _ ->
                    val shortUrlSaved: ShortUrl? = shortUrlRepository.findByKey(id)
                    if(shortUrlSaved != null) {
                        if(reachable.isCompletedExceptionally) {
                            shortUrlSaved.properties.reachable = false
                        } else {
                            shortUrlSaved.properties.reachable = reachable.getNow(false)
                        }
                        shortUrlSaved.properties.validated = true
                        shortUrlRepository.save(shortUrlSaved)
                    }
                    }
                return suReturned
                }
            }
        }
    }