package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.*

interface infoShortUrlUseCase{
    fun info(id: String): ShortUrlDataInfo
}

class InfoShortUrlUseCaseImpl(
    private val shortUrlRepository: ShortUrlRepositoryService, 
    private val clickRepository: ClickRepositoryService
) : infoShortUrlUseCase {
    override fun info(id: String): ShortUrlDataInfo{

        val shortUrl = shortUrlRepository.findByKey(id)

        if(shortUrl==null) throw RedirectionNotFound(id)
        else {
            //En esta val me devolverá todos los clicks
            val clicks = clickRepository.findAllByHash(hash)

            //val mapper = jacksonObjectMapper()
            val mapper = ObjectMapper()
            //Object to JSON in String
            val jsonInString: String = mapper.writeValueAsString(clicks)

            return jsonInString
        }
    }
}
    
