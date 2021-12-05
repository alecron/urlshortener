package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ShortUrl
import es.unizar.urlshortener.core.ShortUrlCSV
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.ValidatorService
import es.unizar.urlshortener.core.usecases.CreateShortUrlUseCase



interface CreateCsvUseCase {
    fun transform(url:String, remoteAddr:String):Any
}

class CreateCsvUseCaseImpl (
        val createShortUrlUseCase: CreateShortUrlUseCase,
        private val validatorService: ValidatorService) : CreateCsvUseCase {
    override fun transform(url:String, remoteAddr:String):Any {
        // String -> Comentario o ShortURL
        if(!validatorService.isValid(url)){
            // Se concatena la URL para mantener la estructura
            // del CSV
            return url + ",,La URI no es valida "
        } else {
            return ShortUrlCSV(
                    url,
                    createShortUrlUseCase.create(
                            url = url,
                            data = ShortUrlProperties(
                                    ip = remoteAddr,
                                    sponsor = null
                            )
                    )
            )

        }
    }
}