plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

// Only the libraries listed under `shade` are packed into the final jar (and relocated below).
// Everything Paper already puts on the server classpath stays compileOnly.
val shade: Configuration = configurations.create("shade")
configurations {
    implementation.get().extendsFrom(shade)
}

group = "com.racks"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Shaded and relocated — the server needs no connection-pool library of its own.
    shade("com.zaxxer:HikariCP:7.1.0")

    // Paper bundles sqlite-jdbc on the server classpath; compileOnly is sufficient.
    compileOnly("org.xerial:sqlite-jdbc:3.53.2.1")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // Test-only: JUnit 5 + a real sqlite driver + an slf4j binding for the storage logger.
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.slf4j:slf4j-simple:2.0.18")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.53.2.1")
    testImplementation("com.zaxxer:HikariCP:7.1.0")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-nowarn", "-Xlint:-deprecation"))
}

// Plain jar is not the deliverable; use shadowJar instead.
tasks.jar {
    archiveBaseName.set("RacksPlain")
    archiveClassifier.set("plain")
}

tasks.shadowJar {
    archiveBaseName.set("Racks")
    archiveVersion.set(version.toString())
    archiveClassifier.set("")

    configurations = listOf(shade)

    // Let duplicate entries reach mergeServiceFiles() instead of being dropped.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    exclude("META-INF/maven/**")
    exclude("META-INF/MANIFEST.MF")
    exclude("META-INF/LICENSE*")
    exclude("META-INF/NOTICE*")
    exclude("META-INF/native-image/**")
    exclude("META-INF/proguard/**")
    exclude("META-INF/licenses/**")
    exclude("org/slf4j/**")
    exclude("org/checkerframework/**")

    relocate("com.zaxxer.hikari", "com.racks.libs.hikari")

    mergeServiceFiles()
    // destinationDirectory.set(file("C:\\Users\\Admin\\Desktop\\Folia\\plugins"))
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}

tasks.runServer {
    downloadPlugins {
        modrinth("viaversion", "5.10.0")
    }
    minecraftVersion("1.21.11")
}
