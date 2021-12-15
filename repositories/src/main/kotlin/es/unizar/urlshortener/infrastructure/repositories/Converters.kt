package es.unizar.urlshortener.infrastructure.repositories

import es.unizar.urlshortener.core.*

/**
 * Extension method to convert a [ClickEntity] into a domain [Click].
 */
fun ClickEntity.toDomain() = Click(
    hash = hash,
    created = created,
    properties = ClickProperties(
        ip = ip,
        referrer = referrer,
        browser = browser,
        platform = platform,
        country = country
    )
)

/**
 * Extension method to convert a domain [Click] into a [ClickEntity].
 */
fun Click.toEntity() = ClickEntity(
    id = null,
    hash = hash,
    created = created,
    ip = properties.ip,
    referrer = properties.referrer,
    browser = properties.browser,
    platform = properties.platform,
    country = properties.country
)

/**
 * Extension method to convert a [ShortUrlEntity] into a domain [ShortUrl].
 */
fun ShortUrlEntity.toDomain() = ShortUrl(
    hash = hash,
    redirection = Redirection(
        target = target,
        mode = mode),
    created = created,
    properties = ShortUrlProperties(
        sponsor = sponsor,
        owner = owner,
        safe = safe,
        reachable = reachable,
        ip = ip,
        country = country
    )
)

/**
 * Extension method to convert a domain [ShortUrl] into a [ShortUrlEntity].
 */
fun ShortUrl.toEntity() = ShortUrlEntity(
    hash = hash,
    target = redirection.target,
    mode = redirection.mode,
    created = created,
    owner = properties.owner,
    sponsor = properties.sponsor,
    safe = properties.safe,
    reachable = properties.reachable,
    ip = properties.ip,
    country = properties.country
)

/**
 * Extension method to convert a [QRCodeEntity] into a domain [QRCode].
 */
fun QRCodeEntity.toDomain() = QRCode(
        hash = hash,
        format = Format(
                height = height,
                width = width,
                color = color,
                background = background,
                typeImage = typeImage,
                errorCorrectionLevel = errorCorrectionLevel
        ),
        qrCode = qrCode
)

/**
 * Extension method to convert a domain [QRCode] into a [QRCodeEntity].
 */
fun QRCode.toEntity() = QRCodeEntity(
        hash = hash,
        height = format.height,
        width = format.width,
        color = format.color,
        background = format.background,
        typeImage = format.typeImage,
        errorCorrectionLevel = format.errorCorrectionLevel,
        qrCode = qrCode
)
