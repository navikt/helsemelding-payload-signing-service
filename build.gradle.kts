import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("io.ktor.plugin") version "3.0.3"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
    id("com.gradleup.shadow") version "8.3.6"
}

tasks {
    shadowJar {
        archiveFileName.set("app.jar")
    }
    test {
        useJUnitPlatform()
    }
    ktlintFormat {
        enabled = true
    }
    ktlintCheck {
        dependsOn("ktlintFormat")
    }
    build {
        dependsOn("ktlintCheck")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs = listOf(
            "-opt-in=kotlin.uuid.ExperimentalUuidApi,arrow.fx.coroutines.await.ExperimentalAwaitAllApi"
        )
    }
}

dependencies {
    api(project(":payload-signing-model"))
    implementation(libs.arrow.functions)
    implementation(libs.arrow.suspendapp)
    implementation(libs.arrow.suspendapp.ktor)
    implementation(libs.bundles.logging)
    implementation(libs.bundles.prometheus)
    implementation(libs.hoplite.hocon)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth.jvm)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.netty)
    implementation(libs.kotlin.logging)
    implementation(libs.bundles.bouncycastle)
    implementation(libs.token.validation.ktor.v3)
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:2.16.0-alpha")

    testImplementation(testLibs.bundles.kotest)
    testImplementation(testLibs.ktor.server.test.host)
    testImplementation(testLibs.mock.oauth2.server)
    testImplementation("io.mockk:mockk:1.13.7")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("no.nav.helsemelding.payloadsigning.AppKt")
}
