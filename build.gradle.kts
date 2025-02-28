import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val junitJupiterVersion = "5.12.0"
val jsonassertVersion = "1.5.3"
val k9rapidVersion = "1.20250225145424-cc27101"
val ulidVersion = "8.3.0"
val ktorVersion = "3.1.0"
val dusseldorfVersion = "6.1.1"

// Database
val flywayVersion = "11.3.4"
val hikariVersion = "6.2.1"
val kotliqueryVersion = "1.9.1"
val postgresVersion = "42.7.5"

// Test
val testcontainersVersion = "1.20.5"
val mockkVersion = "1.13.17"
val schemaValidatorVersion = "1.5.6"
val awaitilityVersion = "4.3.0"
val assertjVersion = "3.27.3"

val mainClass = "no.nav.omsorgspenger.AppKt"

plugins {
    kotlin("jvm") version "2.1.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.sonarqube") version "6.0.1.5171"
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("no.nav.k9.rapid:river:$k9rapidVersion")
    implementation("no.nav.helse:dusseldorf-ktor-core:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-client:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-jackson:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-oauth2-client:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-auth:$dusseldorfVersion")
    implementation("io.ktor:ktor-client-jackson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    // Database
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
    runtimeOnly("org.postgresql:postgresql:$postgresVersion")

    // Test
    testImplementation("no.nav.k9.rapid:overfore-omsorgsdager:$k9rapidVersion")
    testImplementation("no.nav.k9.rapid:overfore-korona-omsorgsdager:$k9rapidVersion")
    testImplementation("no.nav.k9.rapid:fordele-omsorgsdager:$k9rapidVersion")
    testImplementation("no.nav.k9.rapid:river-test:$k9rapidVersion")

    testImplementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:$ulidVersion")
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfVersion")
    testImplementation("com.networknt:json-schema-validator:$schemaValidatorVersion")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")

    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
}

repositories {
    mavenLocal()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/k9-rapid")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: "x-access-token"
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
    mavenCentral()
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        finalizedBy(jacocoTestReport)
    }

    withType<ShadowJar> {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to mainClass
                )
            )
        }
        // Fix for flyway bug https://github.com/flyway/flyway/issues/3482#issuecomment-1189357338
        mergeServiceFiles()
    }

    withType<Wrapper> {
        gradleVersion = "8.8"
    }

    withType<JacocoReport> {
        dependsOn(test) // tests are required to run before generating the report
        reports {
            xml.required.set(true)
            csv.required.set(false)
        }
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_omsorgspenger-rammemeldinger")
        property("sonar.organization", "navikt")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.token", System.getenv("SONAR_TOKEN"))
        property("sonar.sourceEncoding", "UTF-8")
        property("sonar.gradle.skipCompile", "true")
    }
}
