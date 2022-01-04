package es.unizar.urlshortener.infrastructure.repositories

import org.springframework.data.jpa.repository.JpaRepository

/**
 * Specification of the repository of [ShortUrlEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ShortUrlEntityRepository : JpaRepository<ShortUrlEntity, String> {
    fun findByHash(hash: String): ShortUrlEntity?
}

/**
 * Specification of the repository of [QRCodeEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface QRCodeEntityRepository : JpaRepository<QRCodeEntity, String> {
    fun findByHash(hash: String): QRCodeEntity?
}

/**
 * Specification of the repository of [ClickEntity].
 *
 * **Note**: Spring Boot is able to discover this [JpaRepository] without further configuration.
 */
interface ClickEntityRepository : JpaRepository<ClickEntity, Long>{
    //Revisar sintaxis
    fun findAllByHash(hash: String) : List<ClickEntity>
}

interface CsvUrlEntityRepository : JpaRepository<CsvUrlEntity, Long> {
    fun countByUuid(uuid : String) : Long
    fun findAllByUuid(uuid : String) : List<CsvUrlEntity>
}
