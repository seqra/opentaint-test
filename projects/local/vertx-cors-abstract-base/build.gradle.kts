plugins { java; kotlin("jvm") version "1.9.25" }
java { sourceCompatibility = JavaVersion.VERSION_1_8; targetCompatibility = JavaVersion.VERSION_1_8 }
repositories { mavenCentral() }
dependencies {
    // Vert.x and coroutines are needed only to resolve types at compile time; the
    // analyzer models them as external library methods (see the dataflow
    // approximations and rules wired up in projects/repos.yaml).
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.5.21")
    compileOnly("io.vertx:vertx-web:3.8.1")
    compileOnly("io.vertx:vertx-core:3.8.1")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:3.8.1")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> { kotlinOptions { jvmTarget = "1.8" } }
