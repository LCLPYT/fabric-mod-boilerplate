import org.apache.tools.ant.filters.ReplaceTokens
import work.lclpnet.build.task.GithubDeploymentTask
import java.util.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.gradle.build.tools)
}

val props: Properties = buildUtils.loadProperties("publish.properties")  // will be empty, if the file is missing
val env: Map<String, String> = System.getenv()

version = "${project.property("mod_version")!!}+${libs.versions.minecraft.get()}"
group = project.property("maven_group")!!

val modId = project.property("mod_id")!!.toString()

base {
    archivesName.set(modId)
}

val javaVersion = libs.versions.java.get().toInt()

repositories {
    maven {
        url = uri("https://repo.lclpnet.work/repository/internal")
    }
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register(modId) {
            sourceSet(sourceSets.getByName("main"))
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

fabricApi {
    configureDataGeneration()
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.language.kotlin)

    testImplementation(libs.fabric.loader.junit)
    testImplementation(libs.kotlin.test)
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val tokens = mapOf(
        "version" to project.version,
        "loader_version" to libs.versions.fabric.loader.get(),
        "minecraft_compat" to project.property("minecraft_compat")!!,
        "java_version" to javaVersion.toString(),
        "fabric_language_kotlin" to libs.versions.fabric.language.kotlin.get(),
    )

    inputs.properties(tokens)

    filesMatching("fabric.mod.json") {
        expand(tokens)
    }

    filesMatching("$modId.mixins.json") {
        // only replace tokens with braces because mixins may reference inner classes with a '$' symbol
        filter(ReplaceTokens::class, mapOf(
            "beginToken" to $$"${",
            "endToken" to "}",
            "tokens" to tokens
        ))
    }
}

tasks.named<ProcessResources>("processClientResources") {
    val tokens = mapOf(
        "java_version" to javaVersion.toString(),
    )

    inputs.properties(tokens)

    filesMatching("$modId.client.mixins.json") {
        // only replace tokens with braces because mixins may reference inner classes with a '$' symbol
        filter(ReplaceTokens::class, mapOf(
            "beginToken" to $$"${",
            "endToken" to "}",
            "tokens" to tokens
        ))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaVersion)
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

kotlin {
    jvmToolchain(javaVersion)
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}

tasks.register<GithubDeploymentTask>("github") {
    description = "Create a new GitHub release and uploads the built jar to github"

    val artifactTask = tasks.getByName<Jar>("jar")

    dependsOn(artifactTask)

    config {
        token = env["GITHUB_TOKEN"]
        repository = env["GITHUB_REPOSITORY"]
    }

    release {
        title = "[${libs.versions.minecraft.get()}] ${project.name} ${project.version}"
        tag = project.version.toString()
    }

    assets.add(artifactTask.archiveFile.get())
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()

            from(components["java"])

            pom {
                name.set(project.property("artifact_name")!!.toString())
                description.set(project.property("artifact_description")!!.toString())
            }
        }
    }

    // automatically use DEPLOY_URL, DEPLOY_USER and DEPLOY_PASSWORD environment variables
    // or mavenHost, mavenUser and mavenPassword from props
    buildUtils.setupPublishRepository(repositories, props)
}