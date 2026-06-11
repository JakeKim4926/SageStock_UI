package com.sagestock.data.remote

import com.sagestock.domain.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException

private const val MSG_NETWORK = "네트워크 연결을 확인해주세요."
private const val MSG_UNKNOWN = "알 수 없는 오류가 발생했습니다."

/**
 * Retrofit 호출을 [Result]로 감싼다(2xx→Success, 그 외→Error). api-spec §0:
 * `2xx → Result.Success`, 나머지 → `Result.Error(message)`.
 */
suspend fun <T> safeApiCall(
    io: CoroutineDispatcher,
    json: Json,
    block: suspend () -> T,
): Result<T> = withContext(io) {
    try {
        Result.Success(block())
    } catch (e: HttpException) {
        Result.Error(parseApiError(json, e.response()?.errorBody()?.string(), e.code()))
    } catch (e: IOException) {
        Result.Error(MSG_NETWORK)
    } catch (e: Exception) {
        Result.Error(e.message ?: MSG_UNKNOWN)
    }
}

/**
 * 에러 바디에서 사람이 읽을 메시지를 뽑는다. 두 형태를 모두 수용한다:
 * - api-spec §0: `{ "code": "...", "message": "..." }`
 * - FastAPI 기본(422): `{ "detail": "..." }` 또는 `{ "detail": [ { "msg": "..." } ] }`
 * 어느 쪽도 못 읽으면 HTTP 코드 폴백.
 */
internal fun parseApiError(json: Json, body: String?, code: Int): String {
    val fallback = "요청에 실패했습니다 (HTTP $code)"
    if (body.isNullOrBlank()) return fallback
    return runCatching {
        val obj = json.parseToJsonElement(body).jsonObject
        obj["message"]?.jsonPrimitive?.contentOrNull
            ?: obj["code"]?.jsonPrimitive?.contentOrNull
            ?: detailMessage(obj["detail"])
            ?: fallback
    }.getOrDefault(fallback)
}

private fun detailMessage(detail: kotlinx.serialization.json.JsonElement?): String? = when (detail) {
    is JsonPrimitive -> detail.contentOrNull
    is JsonArray -> detail.firstOrNull()
        ?.let { (it as? JsonObject) }
        ?.get("msg")?.jsonPrimitive?.contentOrNull
    else -> null
}
