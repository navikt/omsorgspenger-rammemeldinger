import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val junitJupiterVersion = "5.8.1"
val jsonassertVersion = "1.5.0"
val k9rapidVersion = "1.20210920084849-2ac86f1"
val awaitilityVersion = "4.1.0"
val mockkVersion = "1.12.0"
val ulidVersion = "8.2.0"
val ktorVersion = "1.6.3"
val assertjVersion = "3.21.0"
val dusseldorfVersion = "3.1.6.3-bf04e18"
val schemaValidatorVersion = "1.0.58"

// Database
val flywayVersion = "8.0.0"
val hikariVersion = "5.0.0"
val kotliqueryVersion = "1.6.1"
val postgresVersion = "42.2.24"
val embeddedPostgres = "1.3.1"

val mainClass = "no.nav.omsorgspenger.AppKt"

plugins {
    kotlin("jvm") version "1.5.31"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

dependencies {
    implementation("no.nav.k9.rapid:river:$k9rapidVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("no.nav.helse:dusseldorf-ktor-core:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-client:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-oauth2-client:$dusseldorfVersion")
    implementation("no.nav.helse:dusseldorf-ktor-auth:$dusseldorfVersion")

    // Database
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.github.seratch:kotliquery:$kotliqueryVersion")
    runtimeOnly("org.postgresql:postgresql:$postgresVersion")
    testImplementation("io.zonky.test:embedded-postgres:$embeddedPostgres")

    // Test
    testImplementation("no.nav.k9.rapid:overfore-omsorgsdager:$k9rapidVersion")
    testImplementation("no.nav.k9.rapid:overfore-korona-omsorgsdager:$k9rapidVersion")
    testImplementation("no.nav.k9.rapid:fordele-omsorgsdager:$k9rapidVersion")
    
    testImplementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:$ulidVersion")
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfVersion") {
        exclude(group = "com.github.jknack")
    }
    testImplementation("com.networknt:json-schema-validator:$schemaValidatorVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
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
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
    maven { url = uri("https://jitpack.io") }
    mavenCentral()
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "16"
    }

    named<KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "16"
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
        gradleVersion = "7.2"
    }

}