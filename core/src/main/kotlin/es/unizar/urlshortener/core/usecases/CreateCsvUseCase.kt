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
            // Se devuelve únicamente el comentario del error
            // si no es una URL válida
            return "La URI no es valida "
        } else {
            // De lo contrario se genera una URL acortada
            // preparada para almacenar en el CSV
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