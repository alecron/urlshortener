package es.unizar.urlshortener.core.usecases

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import es.unizar.urlshortener.core.*

interface InfoShortUrlUseCase{
    fun info(id: String): List<SimpleClick>
}

/**
 * Implementation of [InfoShortUrlUseCase].
 */
class InfoShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService, 
    private val clickRepository: ClickRepositoryService
) : InfoShortUrlUseCase {
    override fun info(id: String): List<SimpleClick> {

        val shortUrl = shortUrlRepository.findByKey(id)

        if(shortUrl==null) throw RedirectionNotFound(id)
        else {
            // Devuelve todos los clicks almacenados para el hash dado
            val clicks = clickRepository.findAllByHash(shortUrl.hash)

            val simpleClicks = clicks.map{
                SimpleClick(
                        hash = it.hash,
                        browser = it.properties.browser,
                        platform = it.properties.platform
                )
            }
            return simpleClicks
        }
    }
}
    
