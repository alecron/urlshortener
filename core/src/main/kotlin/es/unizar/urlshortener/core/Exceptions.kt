package es.unizar.urlshortener.core

class InvalidUrlException(val url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(val key: String) : Exception("[$key] is not known")

class InvalidQRParameter(val msg: String) : Exception("[$msg]")

class QRFailure(val process: String) : Exception("[$process] has failed")

class UrlNotReachable(val url: String) : Exception("[$url] is not reachable")

class UrlNotValidatedYet(val url: String) : Exception("[$url] has not been validated yet")

class EmptyFile(val file: String) : Exception("[$file] is empty")
