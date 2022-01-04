package es.unizar.urlshortener.core.usecases

import es.unizar.urlshortener.core.ShortUrlCSV
import es.unizar.urlshortener.core.ShortUrlProperties
import es.unizar.urlshortener.core.ValidatorService



interface CreateCsvUseCase {
    fun transform(url:String, remoteAddr:String):Any
}

/**
 * implementation of [CreateCsvUseCase]
 */
class CreateCsvUseCaseImpl (
        private val createShortUrlUseCase: CreateShortUrlUseCase,
        private val validatorService: ValidatorService
) : CreateCsvUseCase {
    override fun transform(url:String, remoteAddr:String):Any {
        // String -> Comentario o ShortURL
        if(!validatorService.isValid(url)){
            // Se concatena la URL para mantener la estructura
            // del CSV
            return "La URI no es valida "
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