package es.unizar.urlshortener.core.usecases

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import es.unizar.urlshortener.core.*

interface InfoShortUrlUseCase{
    fun info(id: String): String
}

/**
 * Implementation of [InfoShortUrlUseCase].
 */
class InfoShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService, 
    private val clickRepository: ClickRepositoryService
) : InfoShortUrlUseCase {
    override fun info(id: String): String{

        val shortUrl = shortUrlRepository.findByKey(id)

        if(shortUrl==null) throw RedirectionNotFound(id)
        else {
            //En esta val me devolverá todos los clicks
            val clicks = clickRepository.findAllByHash(shortUrl.hash)

            val simpleClicks = clicks.map{
                SimpleClick(
                        hash = it.hash,
                        browser = it.properties.browser,
                        platform = it.properties.platform
                )
            }

            val mapper = jacksonObjectMapper()
            //Object to JSON in String

            val jsonInString: String = mapper.writeValueAsString(simpleClicks)

            return jsonInString
        }
    }
}
    
