plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.3.0"

    id("java-test-fixtures")
//    id("org.jetbrains.changelog")
//    id("com.diffplug.spotless")
//    id("pmd")

}

group = "com.zyurkalov"
version = "1.8.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
//        clion("2025.1.4")
//        bundledPlugin("com.intellij.clion")
        rustRover("2025.1.4")
//        create("IC", "2025.1.1")
//        bundledPlugin("com.intellij.java")
        plugins("IdeaVIM:2.27.0")
        plugins("com.tang:1.4.20-IDEA251")  // EmmyLua plugin
        pluginVerifier()
        zipSigner()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Bundled)
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
    testRuntimeOnly("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = """
      Initial version
    """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
//    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
//    }

    buildSearchableOptions {
        enabled = false // Temporarily disable if the issue persists
    }


    test {
        useJUnitPlatform()
//        jvmArgs = jvmArgs?.filter { !it.contains("kotlinx-coroutines-core") } ?: emptyList()
//        // Or more specifically:
//        jvmArgs("-XX:-UsePerfData") // Disable if using performance data

    }
}
