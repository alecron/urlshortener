package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.*

/**
 * Implementation of the port [ClickRepositoryService].
 */
class ClickRepositoryServiceImpl(
    private val clickEntityRepository: ClickEntityRepository
) : ClickRepositoryService {
    override fun save(cl: Click): Click = clickEntityRepository.save(cl.toEntity()).toDomain()
    override fun findAllByHash(hash: String): List<Click> {
        return clickEntityRepository.findAllByHash(hash).map{
            it.toDomain()
        }
    }
}

/**
 * Implementation of the port [ShortUrlRepositoryService].
 */
class ShortUrlRepositoryServiceImpl(
    private val shortUrlEntityRepository: ShortUrlEntityRepository
) : ShortUrlRepositoryService {
    override fun findByKey(id: String): ShortUrl? = shortUrlEntityRepository.findByHash(id)?.toDomain()

    override fun save(su: ShortUrl): ShortUrl = shortUrlEntityRepository.save(su.toEntity()).toDomain()
}

class CsvUrlRepositoryServiceImpl(
        private val csvUrlEntityRepository: CsvUrlEntityRepository
) : CsvUrlRepositoryService {
    override fun save(csvurl: CsvUrl): CsvUrl = csvUrlEntityRepository.save(csvurl.toEntity()).toDomain()

    override fun findAllByuuid(uuid: String): List<CsvUrl> = csvUrlEntityRepository.findAllByUuid(uuid).map{ it.toDomain() }

    override fun countByUuid(uuid: String): Long = csvUrlEntityRepository.countByUuid(uuid)
}
/**
 * Implementation of the port [QRCodeRepositoryService].
 */
class QRCodeRepositoryServiceImpl(
        private val qrCodeEntityRepository: QRCodeEntityRepository
) : QRCodeRepositoryService {
    override fun findByKey(id: String): QRCode? = qrCodeEntityRepository.findByHash(id)?.toDomain()

    override fun save(qrCode: QRCode): QRCode = qrCodeEntityRepository.save(qrCode.toEntity()).toDomain()
}
