plugins {
    id("buildlogic.java-application-conventions")
}

dependencies {
    // Flink core (provided on cluster; runtimeOnly for local run)
    compileOnly(libs.flink.java)
    compileOnly(libs.flink.streaming.java)
    compileOnly(libs.flink.clients)

    // Table/SQL API
    implementation(libs.flink.table.api)
    implementation(libs.flink.table.runtime)
    implementation(libs.flink.table.planner.loader)

    // Kafka connector for Table API
    implementation(libs.flink.connector.kafka)
    implementation(libs.flink.sql.connector.kafka)

    // Confluent SQL Protobuf format
    implementation(libs.confluent.flink.sql.protobuf)

    // for local execution convenience
    runtimeOnly(libs.flink.java)
    runtimeOnly(libs.flink.streaming.java)
    runtimeOnly(libs.flink.clients)
}

// Collect jars needed by Flink SQL Client into build/sql-libs
tasks.register<Copy>("prepareSqlLibs") {
    val dest = layout.buildDirectory.dir("sql-libs")
    from(configurations.runtimeClasspath)
    into(dest)
}
