import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("buildlogic.java-application-conventions")
    alias(libs.plugins.shadow)
}

// Dependencies that WILL go into fat JAR
configurations {
    create("flinkShadowJar").apply {
        // Exclude core Flink runtime libs from the fat JAR, but allow connector artifacts
        exclude(group = "org.apache.flink", module = "flink-java")
        exclude(group = "org.apache.flink", module = "flink-streaming-java")
        exclude(group = "org.apache.flink", module = "flink-clients")
        exclude(group = "com.google.code.findbugs", module = "jsr305")
        exclude(group = "org.slf4j")
        exclude(group = "org.apache.logging.log4j")
    }
}

dependencies {
    implementation(project(":schema-registry"))
    implementation(libs.confluent.kafka.protobuf.serializer)
    implementation(libs.confluent.schema.registry)
    implementation(libs.confluent.kafka.protobuf.provider)

    // --- Flink provided by EMR cluster (not shaded) ---
    compileOnly(libs.flink.java)
    compileOnly(libs.flink.streaming.java)
    compileOnly(libs.flink.clients)
    implementation(libs.flink.connector.kafka)
    implementation(libs.flink.connector.base)

    // --- allow execution in IntelliJ and local run ---
    runtimeOnly(libs.flink.java)
    runtimeOnly(libs.flink.streaming.java)
    runtimeOnly(libs.flink.clients)
    // runtimeOnly(libs.flink.connector.kafka) // not needed since using implementation above

    // --- Only dependencies that will be included in the final fat JAR ---
    add("flinkShadowJar", project(":schema-registry"))
    add("flinkShadowJar", libs.commons.text)
    add("flinkShadowJar", libs.confluent.kafka.protobuf.serializer)
    add("flinkShadowJar", libs.confluent.schema.registry)
    add("flinkShadowJar", libs.confluent.kafka.protobuf.provider)
    // TEMP: bundle Flink Kafka connector to avoid CNFEs on EMR on EKS where the connector
    // may not be on the cluster classpath yet. Prefer removing this once the image includes
    // org.apache.flink:flink-connector-kafka:${libs.versions.flink-kafka.get()} in /opt/flink/lib.
    add("flinkShadowJar", libs.flink.connector.kafka)
    // Include connector-base alongside the Kafka connector
    add("flinkShadowJar", libs.flink.connector.base)
}

application {
    mainClass.set("org.example.kafka.consumer.KafkaConsumerProtobufJob")
    applicationDefaultJvmArgs = listOf(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
        "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.nio=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED"
    )
}

/* ---- disable unused packaging tasks ---- */
tasks.jar { enabled = false }
tasks.named<org.gradle.jvm.application.tasks.CreateStartScripts>("startScripts") { enabled = false }
tasks.named("distTar") { enabled = false }
tasks.named("distZip") { enabled = false }

/* ---- final JAR for EMR ---- */
tasks.named<ShadowJar>("shadowJar") {
    // remove if all is needed in JAR name
    archiveClassifier.set("")
    configurations = listOf(project.configurations["flinkShadowJar"])

    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    manifest { attributes["Main-Class"] = application.mainClass.get() }
}

tasks.named("build") { dependsOn("shadowJar") }
