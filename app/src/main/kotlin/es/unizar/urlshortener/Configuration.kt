package es.unizar.urlshortener

import es.unizar.urlshortener.core.usecases.*
import es.unizar.urlshortener.infrastructure.delivery.HashServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.QRServiceImpl
import es.unizar.urlshortener.infrastructure.delivery.ValidatorServiceImpl
import es.unizar.urlshortener.infrastructure.repositories.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executor


/**
 * Wires use cases with service implementations, and services implementations with repositories.
 *
 * **Note**: Spring Boot is able to discover this [Configuration] without further configuration.
 */

/* Information about asyncExecutor : https://howtodoinjava.com/spring-boot2/rest/enableasync-async-controller/
* */
@EnableAsync(proxyTargetClass = true)
@Configuration
class ApplicationConfiguration(
    @Autowired val shortUrlEntityRepository: ShortUrlEntityRepository,
    @Autowired val clickEntityRepository: ClickEntityRepository,
    @Autowired val csvUrlEntityRepository: CsvUrlEntityRepository,
    @Autowired val qrCodeEntityRepository: QRCodeEntityRepository
) {

    /**
     * Thread pool configuration for the URI information service
     */
    @Bean(name = ["taskExecutorUriInformation"])
    fun executorTask(): Executor? {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 4
        executor.maxPoolSize = 10
        executor.setQueueCapacity(150)
        executor.initialize()
        return executor
    }

    /**
     * Thread pool configuration for the Reachability service
     */
    @Bean(name = ["taskExecutorReachable"])
    fun taskExecutor(): Executor? {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 4
        executor.maxPoolSize = 10
        executor.setQueueCapacity(150)
        executor.initialize()
        return executor
    }


    @Bean
    fun clickRepositoryService() = ClickRepositoryServiceImpl(clickEntityRepository)

    @Bean
    fun shortUrlRepositoryService() = ShortUrlRepositoryServiceImpl(shortUrlEntityRepository)

    @Bean
    fun csvUrlRepositoryService() = CsvUrlRepositoryServiceImpl(csvUrlEntityRepository)

    @Bean
    fun qrCodeRepositoryService() = QRCodeRepositoryServiceImpl(qrCodeEntityRepository)

    @Bean
    fun validatorService() = ValidatorServiceImpl()

    @Bean
    fun hashService() = HashServiceImpl()

    @Bean
    fun qrService() = QRServiceImpl()

    @Bean
    fun redirectUseCase() = RedirectUseCaseImpl(shortUrlRepositoryService())

    @Bean
    fun logClickUseCase() = LogClickUseCaseImpl(clickRepositoryService())

    @Bean
    fun qrUrlUseCase() = QRUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), qrService(), qrCodeRepositoryService())

    @Bean
    fun infoShortUrlUseCase() = InfoShortUrlUseCaseImpl(shortUrlRepositoryService(), clickRepositoryService())

    @Bean
    fun createShortUrlUseCase() = CreateShortUrlUseCaseImpl(shortUrlRepositoryService(), validatorService(), hashService())

    @Bean
    fun createCsvUseCase() = CreateCsvUseCaseImpl(createShortUrlUseCase(), validatorService())
}
