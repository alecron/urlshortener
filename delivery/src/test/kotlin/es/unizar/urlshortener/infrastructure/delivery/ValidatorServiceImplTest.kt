package es.unizar.urlshortener.infrastructure.delivery

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.TimeUnit


@SpringBootTest(classes = [ValidatorServiceImpl::class])
internal class ValidatorServiceImplTest {
    @Autowired
    private lateinit var validatorService: ValidatorServiceImpl

    @Test
    fun `Url is not reachable`(){
        assertEquals(false,
            validatorService.isReachable("http://www.unizarFalso.es/").get(3L,TimeUnit.SECONDS))
    }

    @Test
    fun `Url is reachable`(){
        assertEquals(true,
            validatorService.isReachable("http://www.unizar.es/").get(3L,TimeUnit.SECONDS))
    }


    @Test
    fun `HTTP Url is valid`(){
        assertEquals(true,
            validatorService.isValid("http://www.unizar.es/"))

    }

    @Test
    fun `HTTPS Url is valid`(){
        assertEquals(true,
            validatorService.isValid("https://www.unizar.es/"))
    }

    @Test
    fun `Url not valid`(){
        assertEquals(false,
            validatorService.isValid("fake://www.unizar.es/"))
    }
}