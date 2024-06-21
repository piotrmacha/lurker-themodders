buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.flywaydb:flyway-gradle-plugin:10.15.0")
        classpath("org.flywaydb:flyway-database-postgresql:10.15.0")
    }
}

plugins {
    id("org.springframework.boot") version("3.3.0")
    id("io.spring.dependency-management") version("1.1.5")
    id("java")
    id("org.flywaydb.flyway") version "10.15.0"
    id("org.jooq.jooq-codegen-gradle") version "3.19.10"
    id("org.graalvm.buildtools.native") version "0.10.2"
    id("io.freefair.lombok") version "8.6"
}

group = "pl.piotrmacha.lurker"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        mavenBom("org.springframework.shell:spring-shell-dependencies:3.3.0")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.shell:spring-shell-starter")
    implementation("org.jooq:jooq:3.19.10")
    implementation("org.jooq:jooq-meta:3.19.10")
    implementation("org.jooq:jooq-codegen:3.19.10")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.google.guava:guava:33.2.1-jre")
    implementation("com.github.rholder:guava-retrying:2.0.0")
    jooqCodegen("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.0")
}

file(".env").readLines().forEach { line ->
    val (key, value) = line.split("=")
    System.setProperty(key, value)
}

flyway {
    url = "jdbc:postgresql://${System.getProperty("POSTGRES_HOST")}:${System.getProperty("POSTGRES_PORT")}/${System.getProperty("POSTGRES_DB")}"
    user = System.getProperty("POSTGRES_USER")
    password = System.getProperty("POSTGRES_PASSWORD")
}

jooq {
    configuration {
        jdbc {
            url = "jdbc:postgresql://${System.getProperty("POSTGRES_HOST")}:${System.getProperty("POSTGRES_PORT")}/${System.getProperty("POSTGRES_DB")}"
            user = System.getProperty("POSTGRES_USER")
            password = System.getProperty("POSTGRES_PASSWORD")
            driver = "org.postgresql.Driver"
        }
        generator {
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                inputSchema = "public"
            }
            target {
                packageName = "pl.piotrmacha.lurker.jooq"
                directory = "src/main/java"
            }
        }
    }
}

tasks.getByName("jooqCodegen").dependsOn("flywayMigrate")

graalvmNative {
    useArgFile = true
}