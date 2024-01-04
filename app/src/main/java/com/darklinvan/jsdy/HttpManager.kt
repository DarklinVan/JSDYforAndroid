import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.IOException

class HttpManager private constructor() {

    data class HttpResponse(
        val statusCode: Int,
        val headers: Headers,
        val body: String
    )

    interface HttpCallback {
        fun onSuccess(response: HttpResponse)
        fun onError(error: String)
        fun onCompleted()
    }


    companion object {
        private const val TIMEOUT_SECONDS = 5L

        fun get(url: String, callback: HttpCallback) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val response = withContext(Dispatchers.IO) {
                        executeRequest(buildGetRequest(url))
                    }
                    withContext(Dispatchers.Main) {
                        callback.onSuccess(response)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback.onError(e.message ?: "Unknown error")
                    }
                }
                callback.onCompleted()
            }
        }

        fun post(url: String, requestBody: RequestBody, callback: HttpCallback) {
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val response = withContext(Dispatchers.IO) {
                        executeRequest(buildPostRequest(url, requestBody))
                    }
                    withContext(Dispatchers.Main) {
                        callback.onSuccess(response)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        callback.onError(e.message ?: "Unknown error")
                    }
                }
                callback.onCompleted()
            }
        }

        private fun executeRequest(request: Request): HttpResponse {
            val client = OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }

            val statusCode = response.code
            val headers = response.headers
            val responseBody = response.body?.string() ?: throw IOException("Empty response body")

            return HttpResponse(statusCode, headers, responseBody)
        }

        private fun buildGetRequest(url: String): Request {
            return Request.Builder()
                .url(url)
                .get()
                .build()
        }

        private fun buildPostRequest(url: String, requestBody: RequestBody): Request {
            return Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
        }
    }
}
