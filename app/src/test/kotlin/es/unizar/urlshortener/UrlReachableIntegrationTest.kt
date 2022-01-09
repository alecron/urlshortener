package es.unizar.urlshortener

import es.unizar.urlshortener.infrastructure.delivery.ShortUrlDataOut
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.jdbc.JdbcTestUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.net.URI
import java.util.concurrent.TimeUnit

/*
* Informacion para tests que emplean funciones asincronas:
*   https://newbedev.com/junit-testing-a-spring-async-void-service-method
*/


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UrlReachableIntegrationTest {
    @LocalServerPort
    private val port = 0

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    @Qualifier("taskExecutor")
    private val executor: ThreadPoolTaskExecutor? = null

    @BeforeEach
    fun setup() {
        val httpClient = HttpClientBuilder.create()
            .disableRedirectHandling()
            .build()
        (restTemplate.restTemplate.requestFactory as HttpComponentsClientHttpRequestFactory).httpClient = httpClient

        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }

    @AfterEach
    fun tearDowns() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "shorturl", "click")
    }


    @Test
    fun `creates returns created if url is not reachable`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = "http://www.unizarFalso.es/"

        val response = restTemplate.postForEntity("http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
    }

    @Test
    fun `creates returns created if url is not reachable and redirects to that url returns bad request`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = "http://www.unizarFalso.es/"

        val response = restTemplate.postForEntity("http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java)
        Thread.sleep(4_000)
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)

        val secondResponse =
            restTemplate.getForEntity(response.headers.location,
                String::class.java)
        assertThat(secondResponse.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `creates returns created if url is reachable`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = "http://www.unizar.es/"

        val response = restTemplate.postForEntity("http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)
    }

    @Test
    fun `creates returns created if url is  reachable and redirects to that url returns bad request`() {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val data: MultiValueMap<String, String> = LinkedMultiValueMap()
        data["url"] = "http://www.unizar.es/"

        val response = restTemplate.postForEntity("http://localhost:$port/api/link",
            HttpEntity(data, headers), ShortUrlDataOut::class.java)
        Thread.sleep(4_000)

        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "shorturl")).isEqualTo(1)

        val secondResponse =
            restTemplate.getForEntity(response.headers.location,
                String::class.java)
        assertThat(secondResponse.statusCode).isEqualTo(HttpStatus.TEMPORARY_REDIRECT)
    }
}