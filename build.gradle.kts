plugins {
    java
    application
}

group = "com.dtsx.dh"
version = "1.0.0-alpha.1"

repositories {
    mavenCentral()
}

dependencies {
    // cli stuff
    implementation("info.picocli:picocli:4.7.7")
    annotationProcessor("info.picocli:picocli-codegen:4.7.7")
    implementation("info.picocli:picocli-codegen:4.7.7")

    // snapshot testing
    implementation("com.approvaltests:approvaltests:25.0.23")

    // test settings from .env files
    implementation("io.github.cdimascio:dotenv-java:3.2.0")

    // shuts up some errors about missing SLF4J implementations
    implementation("org.slf4j:slf4j-nop:2.0.17")

    // yaml
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.0.3")
    implementation("tools.jackson.core:jackson-databind:3.0.3")

    // static utils
    compileOnly("org.jetbrains:annotations:26.0.2")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    // other utils
    implementation("commons-io:commons-io:2.21.0")

    // no idea what this is :)
    implementation("com.datastax.astra:astra-db-java:2.1.4")
}

application {
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
    mainClass.set("com.dtsx.dh.HelperCli")
}

tasks.register<Jar>("fatJar") {
    dependsOn(configurations.runtimeClasspath)

    group = "build"
    description = "fat jar"

    archiveFileName.set("verifier.jar")
    archiveVersion.set(project.version.toString())

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "com.dtsx.dh.HelperCli"
    }

    from(sourceSets.main.get().output)

    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
