import java.io.ByteArrayOutputStream

plugins {
    id("java-library")
    id("maven-publish")
    id("io.github.goooler.shadow") version "8.1.7"
}

subprojects {
    plugins.apply("java-library")
    plugins.apply("maven-publish")
    plugins.apply("io.github.goooler.shadow")

    group = "${project.property("group")}"
    version = "${project.property("version")}.${commitsSinceLastTag()}"

    repositories {
        mavenCentral()
        maven("https://repo.codemc.io/repository/maven-public/")
    }

    dependencies {
        compileOnly(group = "org.apache.logging.log4j", name = "log4j-api", version = "2.14.1")
        compileOnly(group = "com.google.code.gson", name = "gson", version = "2.8.9")
        compileOnly(group = "org.popcraft", name = "chunky-common", version = "${project.property("target")}")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
    }

    tasks {
        withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.release = 21
        }
        jar {
            archiveClassifier.set("noshade")
        }
        shadowJar {
            archiveClassifier.set("")
            archiveFileName.set("${project.property("artifactName")}-${project.version}.jar")
        }
        build {
            dependsOn(shadowJar)
        }
    }

    publishing {
        repositories {
            if (project.hasProperty("mavenUsername") && project.hasProperty("mavenPassword")) {
                maven {
                    credentials {
                        username = "${project.property("mavenUsername")}"
                        password = "${project.property("mavenPassword")}"
                    }
                    url = uri("https://repo.codemc.io/repository/maven-releases/")
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = "${project.group}"
                artifactId = project.name
                version = "${project.version}"
                from(components["java"])
            }
        }
    }
}

fun commitsSinceLastTag(): String {
    return try {
        val output = ByteArrayOutputStream()
        exec {
            commandLine("git", "describe", "--tags")
            standardOutput = output
            isIgnoreExitValue = true // Avoid failing the build
        }
        val desc = output.toString().trim()
        if (desc.contains('-')) desc.split('-')[1] else "0"
    } catch (e: Exception) {
        "0"
    }
}
