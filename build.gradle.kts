plugins {
    java
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.grupavii"
version = "0.0.1-SNAPSHOT"
description = "DT-mapping-india"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val jvmOpenArgs = listOf(
    "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED",
    "--add-opens", "java.base/sun.misc=ALL-UNNAMED"
)

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs(jvmOpenArgs)
}

tasks.named<JavaExec>("bootRun") {
    jvmArgs(jvmOpenArgs)
}

tasks.register<JavaExec>("runNaive") {
    description = "Run naive heap grid demo that exceeds the constrained heap"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.grupavii.dtmappingindia.DtMappingIndiaApplication"
    jvmArgs(jvmOpenArgs + listOf("-Xmx256m"))
    args("--demo.step=naive", "--grid.rows=10000", "--grid.cols=10000")
}

tasks.register<JavaExec>("runSingleBuffer") {
    description = "Show the single MappedByteBuffer limit without creating a multi-GB file"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.grupavii.dtmappingindia.DtMappingIndiaApplication"
    jvmArgs(jvmOpenArgs + listOf("-Xmx512m"))
    args("--demo.step=single-buffer", "--grid.rows=20000", "--grid.cols=20000")
}

tasks.register<JavaExec>("runRegionMapped") {
    description = "Run a small grid split across two configurable mapped regions"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.grupavii.dtmappingindia.DtMappingIndiaApplication"
    jvmArgs(jvmOpenArgs + listOf("-Xmx512m"))
    args("--demo.step=region-mapped", "--grid.rows=512", "--grid.cols=512",
            "--grid.region-bytes=1048576")
}

tasks.register<JavaExec>("runPanama") {
    description = "Run Panama grid demo (java.lang.foreign — no Unsafe, no --add-opens)"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass = "com.grupavii.dtmappingindia.DtMappingIndiaApplication"
    jvmArgs("-Xmx512m")
    args("--demo.step=panama", "--grid.rows=1000", "--grid.cols=1000")
}
