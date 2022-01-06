package es.unizar.urlshortener

import com.google.common.collect.Sets
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.ApiInfoBuilder
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.service.ApiInfo
import springfox.documentation.service.Contact
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import springfox.documentation.swagger2.annotations.EnableSwagger2


/**
 * Swagger documentation interface available at:
 * http://localhost:8080/swagger-ui.html
 * Swagger metrics as a JSON file available at:
 * http://localhost:8080/api-docs
 * Swagger metrics as a YAML file available at (this will download a yaml file to your computer):
 * http://localhost:8080/api-docs.yaml
 */
@Configuration
@EnableSwagger2
class SwaggerConf {

    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.SWAGGER_2)
                .useDefaultResponseMessages(false)
                .protocols(Sets.newHashSet("http", "https"))
                .apiInfo(getInfo())
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build()
                .apiInfo(getInfo())
    }

    private fun getInfo(): ApiInfo {
        return ApiInfoBuilder()
                .title("URL shortener API")
                .description("URL shortener with QR generation and massive URL shortening via CSV")
                .version("1.0.0")
                .license("Apache 2.0")
                .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0")
                .contact(Contact("Contact", "https://github.com/alecron/urlshortener", "779354@unizar.es"))
                .build()
    }
}
