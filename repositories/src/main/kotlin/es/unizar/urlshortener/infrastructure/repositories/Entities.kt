package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.Format
import java.time.OffsetDateTime
import java.util.*
import javax.persistence.*

/**
 * The [ClickEntity] entity logs clicks.
 */
@Entity
@Table(name = "click")
class ClickEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long?,
    val hash: String,
    val created: OffsetDateTime,
    val ip: String?,
    val referrer: String?,
    val browser: String?,
    val platform: String?,
    val country: String?
)

/**
 * The [ShortUrlEntity] entity stores short urls.
 */
@Entity
@Table(name = "shorturl")
class ShortUrlEntity(
    @Id
    val hash: String,
    val target: String,
    val sponsor: String?,
    val created: OffsetDateTime,
    val owner: String?,
    val mode: Int,
    val safe: Boolean,
    val validated: Boolean,
    val reachable: Boolean,
    val ip: String?,
    val country: String?
)

@Entity
@Table(name="csvurl")
class CsvUrlEntity(
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        val id: Long?,
        val uuid: String,
        val urlHash: String,
        val originalUri: String,
        val comment: String?,
        val qrRecord: String?
)

/**
 * The [QRCodeEntity] entity stores qr codes.
 */
@Entity
@Table(name = "qrcode")
class QRCodeEntity(
    @Id
    val hash: String,
    val height: Int,
    val width: Int,
    val color: String,
    val background: String,
    val typeImage: String,
    val errorCorrectionLevel: String,
    @Lob
    @Column(name = "qrCode", columnDefinition="BLOB")
    val qrCode: ByteArray?
)