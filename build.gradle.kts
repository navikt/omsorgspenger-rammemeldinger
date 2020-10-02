import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val junitJupiterVersion = "5.6.2"
val jsonassertVersion = "1.5.0"
val k9rapidVersion = "1.fe10445"
val awaitilityVersion = "4.0.3"
val mockkVersion = "1.10.0"
val ulidVersion = "8.2.0"

val mainClass = "no.nav.omsorgspenger.AppKt"

plugins {
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

dependencies {
    implementation("no.nav.k9.rapid:river:$k9rapidVersion")

    // Test
    testImplementation("no.nav.k9.rapid:overfore-omsorgsdager:$k9rapidVersion")
    testImplementation("de.huxhorn.sulky:de.huxhorn.sulky.ulid:$ulidVersion")
    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
    testImplementation("org.skyscreamer:jsonassert:$jsonassertVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
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
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/kotlin/ktor")
}

tasks {

    compileKotlin {
        kotlinOptions.jvmTarget = "12"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "12"
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
        gradleVersion = "6.6.1"
    }

}