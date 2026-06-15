package com.sagestock.data

import com.sagestock.data.remote.AuthApi
import com.sagestock.data.remote.AuthInterceptor
import com.sagestock.data.remote.TokenAuthenticator
import com.sagestock.domain.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class AuthFlowTest {

    private lateinit var server: MockWebServer
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private fun authApi(): AuthApi = Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AuthApi::class.java)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login success stores tokens and marks session`() = runTest {
        val session = mockk<SessionManager>(relaxed = true)
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"accessToken":"a-token","refreshToken":"r-token","accessExpiresIn":1800}""",
            ),
        )
        val repo = RetrofitAuthRepository(authApi(), session, json, UnconfinedTestDispatcher())

        val result = repo.login("user@example.com", "pw", autoLogin = true)

        assertTrue(result is Result.Success)
        verify { session.saveTokens("a-token", "r-token", 1800) }
        verify { session.login(true) }
    }

    @Test
    fun `login failure maps error and does not store tokens`() = runTest {
        val session = mockk<SessionManager>(relaxed = true)
        server.enqueue(
            MockResponse().setResponseCode(401).setBody(
                """{"code":"UNAUTHORIZED","message":"이메일 또는 비밀번호가 올바르지 않습니다."}""",
            ),
        )
        val repo = RetrofitAuthRepository(authApi(), session, json, UnconfinedTestDispatcher())

        val result = repo.login("user@example.com", "wrong", autoLogin = false)

        assertTrue(result is Result.Error)
        assertEquals("이메일 또는 비밀번호가 올바르지 않습니다.", (result as Result.Error).message)
        verify(exactly = 0) { session.saveTokens(any(), any(), any()) }
    }

    @Test
    fun `interceptor attaches bearer header when token present`() {
        val session = mockk<SessionManager>(relaxed = true)
        every { session.accessToken() } returns "abc"
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(session)).build()

        client.newCall(Request.Builder().url(server.url("/v1/signals")).build()).execute().close()

        val recorded = server.takeRequest()
        assertEquals("Bearer abc", recorded.getHeader("Authorization"))
    }

    @Test
    fun `authenticator refreshes on 401 and retries with new token`() {
        val session = mockk<SessionManager>(relaxed = true)
        every { session.refreshToken() } returns "r-token"
        val api = authApi()

        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/v1/auth/refresh" ->
                    MockResponse().setResponseCode(200)
                        .setBody("""{"accessToken":"new-access","accessExpiresIn":1800}""")
                request.getHeader("Authorization") == "Bearer new-access" ->
                    MockResponse().setResponseCode(200).setBody("ok")
                else -> MockResponse().setResponseCode(401).setBody("unauthorized")
            }
        }

        val client = OkHttpClient.Builder()
            .authenticator(TokenAuthenticator(session, api))
            .build()

        val response = client.newCall(
            Request.Builder()
                .url(server.url("/v1/protected"))
                .header("Authorization", "Bearer stale")
                .build(),
        ).execute()

        assertEquals(200, response.code)
        response.close()
        verify { session.updateAccessToken("new-access", 1800) }
    }
}
