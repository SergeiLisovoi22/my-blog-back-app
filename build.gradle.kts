plugins {
    java
    war
}

group = "ru.practicum"
version = "1.0.0"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

repositories { mavenCentral() }

dependencies {
    implementation("org.springframework:spring-webmvc:6.2.12")
    implementation("org.springframework:spring-jdbc:6.2.12")
    implementation("org.springframework.data:spring-data-jdbc:3.5.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("com.h2database:h2:2.3.232")
    providedCompile("jakarta.servlet:jakarta.servlet-api:6.0.0")

    testImplementation("org.springframework:spring-test:6.2.12")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("com.jayway.jsonpath:json-path:2.9.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
}

tasks.war { archiveFileName.set("ROOT.war") }
tasks.test { useJUnitPlatform() }
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    options.encoding = "UTF-8"
}

