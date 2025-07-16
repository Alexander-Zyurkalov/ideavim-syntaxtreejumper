plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.3.0"

    id("java-test-fixtures")
//    id("org.jetbrains.changelog")
//    id("com.diffplug.spotless")
//    id("pmd")

}

group = "com.zyurkalov"
version = "1.0.9"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IC", "2025.1.1.1")
        plugins("IdeaVIM:2.24.0" )
        bundledPlugin("com.intellij.java")
        pluginVerifier()
        zipSigner()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
    testRuntimeOnly("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            untilBuild = "251.*"
        }

        changeNotes = """
            Initial version with multi-language support:
            - Java syntax tree navigation
            - JavaScript/TypeScript support  
            - Python syntax tree jumping
            - Rust language support
            - C/C++ syntax navigation
        """.trimIndent()
    }

    pluginVerification {
        ides {
            // Test against multiple IDE types
            ide("IC", "2025.1.4")     // IntelliJ IDEA Community
            ide("IU", "2025.1.4")     // IntelliJ IDEA Ultimate
            ide("PY", "2025.1.4")     // PyCharm Professional
            ide("PC", "2025.1.4")     // PyCharm Community
            ide("CL", "2025.1.4")     // CLion
            ide("RR", "2025.1.4")     // RustRover
        }
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }

    test {
        useJUnitPlatform()
    }
}
