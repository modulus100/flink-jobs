
plugins {
    id("buildlogic.java-library-conventions")
    alias(libs.plugins.shadow)
    alias(libs.plugins.protobuf)
}

dependencies {
    implementation(libs.protobuf.java)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protoc.get()}"
    }
}

sourceSets {
    named("main") {
        java.srcDir(layout.buildDirectory.dir("generated/source/proto/main/java"))

        proto {
            srcDir("src/main/java/proto")
        }
    }
}