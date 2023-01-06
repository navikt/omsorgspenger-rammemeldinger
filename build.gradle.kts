import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val junitJupiterVersion = "5.9.1"
val jsonassertVersion = "1.5.1"
val k9rapidVersion = "1.20221124140732-e07f6f7"
val ulidVersion = "8.3.0"
val ktorVersion = "2.2.2"
val dusseldorfVersion = "3.2.2.2-d9c7672"

// Database
val flywayVersion = "9.11.0"
val hikariVersion = "5.0.1"
val kotliqueryVersion = "1.9.0"
val postgresVersion = "42.5.1"

// Test
val embeddedPostgres = "2.0.1"
val embeddedPostgresBinaries = "12.9.0"
val mockkVersion = "1.13.2"
val schemaValidatorVersion = "1.0.76"
val awaitilityVersion = "4.2.0"
val assertjVersion = "3.24.1"

val mainClass = "no.nav.omsorgspenger.AppKt"

plugins {
    kotlin("jvm") version "1.8.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("no.nav.k9.rapid:river:$k9rapidVersion")
    implementation("no.nav.helse:dusseldorf-ktor-core:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-client:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-jackson:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-oauth2-client:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-auth:$dusseldorfVersion")

    // Database
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
    implementation("io.ktor:ktor-client-jackson-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    runtimeOnly("org.postgresql:postgresql:$postgresVersion")
    testImplementation("io.zonky.test:embedded-postgres:$embeddedPostgres")
    testImplementation(platform("io.zonky.test.postgres:embedded-postgres-binaries-bom:$embeddedPostgresBinaries"))

    // Test
    testImplementation("no.nav.k9.rapid:overfore-omsorgsdager:$k9rapidVersion")
    testImplementation("no.nav.k9.rapid:overfore-korona-omsorgsdager:$k9rapidVersion")
    testImplementation("no.nav.k9.rapid:fordele-omsorgsdager:$k9rapidVersion")
    
    testImplementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:$ulidVersion")
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfVersion")
    testImplementation("com.networknt:json-schema-validator:$schemaValidatorVersion")
    testImplementation("io.ktor:ktor-server-test-host-jvm:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
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
    maven { url = uri("https://jitpack.io") }
    mavenCentral()
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    named<KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "17"
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
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
    }

    withType<Wrapper> {
        gradleVersion = "7.6"
    }

}