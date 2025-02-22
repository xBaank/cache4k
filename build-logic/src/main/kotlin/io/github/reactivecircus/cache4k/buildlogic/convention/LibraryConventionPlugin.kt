package io.github.reactivecircus.cache4k.buildlogic.convention

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.MavenPublishPlugin
import com.vanniktech.maven.publish.SonatypeHost
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal class LibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        // configure KMP library project
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        group = property("GROUP") as String
        version = property("VERSION_NAME") as String

        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            extensions.configure<KotlinMultiplatformExtension> {
                explicitApi()
                configureTargets()
                sourceSets.configureEach {
                    languageSettings.apply {
                        languageVersion = "1.8"
                        progressiveMode = true
                        enableLanguageFeature("NewInference")
                        optIn("kotlin.time.ExperimentalTime")
                    }
                }
            }
        }

        // apply dokka plugin
        pluginManager.apply(DokkaPlugin::class.java)

        // configure detekt
        pluginManager.apply(DetektPlugin::class.java)
        dependencies.add("detektPlugins", the<LibrariesForLibs>().plugin.detektFormatting)
        plugins.withType<DetektPlugin> {
            extensions.configure<DetektExtension> {
                source = files("src/")
                config = files("${project.rootDir}/detekt.yml")
                buildUponDefaultConfig = true
                allRules = true
                parallel = true
            }
            tasks.withType<Detekt>().configureEach {
                jvmTarget = JvmTarget.JVM_11.target
                reports {
                    xml.required.set(false)
                    txt.required.set(false)
                    sarif.required.set(false)
                    md.required.set(false)
                }
            }
        }

        // configure publishing
        pluginManager.apply(MavenPublishPlugin::class.java)
        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
            signAllPublications()
        }
    }
}

@Suppress("LongMethod", "MagicNumber")
private fun KotlinMultiplatformExtension.configureTargets() {
    targets {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(20))
            vendor.set(JvmVendorSpec.AZUL)
        }

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        targetHierarchy.default {
            group("jvmAndIos") {
                withJvm()
                withIosX64()
                withIosArm64()
                withIosSimulatorArm64()
            }
            group("unixDesktop") {
                withLinuxX64()
                withMacosX64()
                withMacosArm64()
            }
            group("nonIosApple") {
                withTvosX64()
                withTvosArm64()
                withTvosSimulatorArm64()
                withWatchosX64()
                withWatchosArm64()
                withWatchosSimulatorArm64()
            }
        }

        jvm {
            compilations.configureEach {
                compilerOptions.configure {
                    jvmTarget.set(JvmTarget.JVM_11)
                    freeCompilerArgs.addAll(
                        "-Xjvm-default=all"
                    )
                }
            }
        }
        js(IR) {
            compilations.all {
                compilerOptions.configure {
                    moduleKind.set(JsModuleKind.MODULE_COMMONJS)
                }
            }
            browser()
            nodejs()
        }
        iosX64()
        iosArm64()
        iosSimulatorArm64()
        macosX64()
        macosArm64()
        tvosX64()
        tvosArm64()
        tvosSimulatorArm64()
        watchosX64()
        watchosArm64()
        watchosSimulatorArm64()
        linuxX64()
        mingwX64()
    }
}
