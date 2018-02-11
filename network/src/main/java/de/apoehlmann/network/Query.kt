package de.apoehlmann.network

import java.net.URI

/**
 * Created by alex on 10.02.18.
 */

sealed class Query<T>(uri: URI, response: Class<T>) {
    data class PostQuery<T>(val uri: URI, val body: Any, val bodyType: BodyType, val response: Class<T>) : Query<T>(uri, response)
    data class GetQuery<T>(val uri: URI, val response: Class<T>) : Query<T>(uri, response)
}

enum class BodyType {
    JSON, XML
}