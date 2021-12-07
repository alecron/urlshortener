package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*
import java.util.Date

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
    private val hashService: HashService,
    private val uRIReachableService: URIReachableService
) : CreateShortUrlUseCase {
    override fun create(url: String, data: ShortUrlProperties): ShortUrl =
        if (!validatorService.isValid(url)) {
            throw InvalidUrlException(url)
        } else if(!uRIReachableService.isReachable(url)) {
            throw UrlNotReachable(url)
        } else {
            val id: String = hashService.hasUrl(url)
            val su = ShortUrl(
                    hash = id,
                    redirection = Redirection(target = url),
                    properties = ShortUrlProperties(
                            safe = data.safe,
                            ip = data.ip,
                            sponsor = data.sponsor
                    )
            )
            shortUrlRepository.save(su)
        }
}
