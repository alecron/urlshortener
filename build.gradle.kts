import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.util.findInterfaceImplementation
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "2.5.5" apply false
    id("io.spring.dependency-management") version "1.0.11.RELEASE" apply false
    kotlin("jvm") version "1.5.31" apply false
    kotlin("plugin.spring") version "1.5.31" apply false
    kotlin("plugin.jpa") version "1.5.31" apply false
}

group = "es.unizar"
version = "1.0.0"

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    repositories {
        mavenCentral()
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
        }
    }
    tasks.withType<Test> {
        useJUnitPlatform()
    }
    dependencies {
        "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }
}

project(":core") {
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-amqp")
        "implementation"("io.ktor:ktor-client-core:1.6.5")
        "implementation" ("io.ktor:ktor-client-cio:1.6.5")
        "implementation" ("com.fasterxml.jackson.module:jackson-module-kotlin")

        "implementation"("org.springframework.boot:spring-boot-starter")
    }
    tasks.getByName<BootJar>("bootJar") {
        enabled = false
    }
}

project(":repositories") {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    dependencies {
        "implementation"(project(":core"))
        "implementation"("org.springframework.boot:spring-boot-starter-data-jpa")
    }
    tasks.getByName<BootJar>("bootJar") {
        enabled = false
    }
}

project(":delivery") {
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    dependencies {
        "implementation"(project(":core"))
        "implementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springframework.boot:spring-boot-starter-hateoas")
        "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "implementation"("commons-validator:commons-validator:1.6")
        "implementation"("com.google.guava:guava:23.0")
        "implementation"("org.webjars:webjars-locator:0.42")
        "implementation"("org.webjars.npm:htmx.org:1.6.0")
        //QR generator
        "implementation"("com.google.zxing:core:3.3.3")
        "implementation"("com.google.zxing:javase:3.3.3")
        "implementation"("io.ktor:ktor-client-core:1.6.5")
        "implementation" ("io.ktor:ktor-client-cio:1.6.5")
        //CSV
        "implementation" ("org.apache.commons:commons-csv:1.5")
        //Rabbitmq
        "implementation"("org.springframework.boot:spring-boot-starter-amqp")
        //Swagger and OpenAPI
        "implementation" ("org.springdoc:springdoc-openapi-ui:1.5.2")
        "implementation" ("io.springfox:springfox-swagger2:3.0.0")
        "implementation" ("io.springfox:springfox-spring-web:3.0.0")
        "implementation" ("io.springfox:springfox-oas:3.0.0")
        "implementation"("io.swagger:swagger-annotations:1.5.21")
        "implementation"("io.swagger:swagger-models:1.5.21")
        "implementation"("io.springfox:springfox-boot-starter:3.0.0")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:3.2.0")
    }
    tasks.getByName<BootJar>("bootJar") {
        enabled = false
    }
}

project(":app") {
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    dependencies {
        "implementation"(project(":core"))
        "implementation"(project(":delivery"))
        "implementation"(project(":repositories"))
        "implementation"("org.springframework.boot:spring-boot-starter")
        "implementation"( "org.webjars:bootstrap:3.3.5")
        "implementation"("org.webjars:jquery:2.1.4")
        "implementation"("org.springframework.boot:spring-boot-starter-amqp")
        "implementation"("org.webjars.npm:htmx.org:1.6.0")
        "implementation"("com.google.guava:guava:23.0")
        //Swagger and OpenAPI
        "implementation" ("org.springdoc:springdoc-openapi-ui:1.5.2")
        "implementation" ("io.springfox:springfox-swagger2:3.0.0")
        "implementation" ("io.springfox:springfox-spring-web:3.0.0")
        "implementation" ("io.springfox:springfox-oas:3.0.0")
        "implementation"("io.swagger:swagger-annotations:1.5.21")
        "implementation"("io.swagger:swagger-models:1.5.21")
        "implementation"("io.springfox:springfox-boot-starter:3.0.0")

        "runtimeOnly"("org.hsqldb:hsqldb")

        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "implementation"("org.springframework.boot:spring-boot-starter-hateoas")
        "testImplementation"("org.springframework.boot:spring-boot-starter-web")
        "implementation"("org.springframework.boot:spring-boot-starter-thymeleaf")
        "testImplementation"("org.springframework.boot:spring-boot-starter-jdbc")
        "testImplementation"("org.mockito.kotlin:mockito-kotlin:3.2.0")
        "testImplementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
        "testImplementation"("org.apache.httpcomponents:httpclient")

        //Rabbitmq
        "implementation"("org.springframework.boot:spring-boot-starter-amqp")
    }
}
