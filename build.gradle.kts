import java.util.Properties

plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.gradle.build.tools)
}

val props: Properties = buildUtils.loadProperties("publish.properties")  // will be empty, if the file is missing

version = "${project.property("mod_version")!!}+${libs.versions.minecraft.get()}"
group = project.property("maven_group")!!

base {
    archivesName.set(project.property("mod_id")!!.toString())
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
        register(project.property("mod_id")!!.toString()) {
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

    mappings(loom.officialMojangMappings())

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

    testImplementation(libs.fabric.loader.junit)
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    inputs.properties(
        "version" to project.version,
        "loader_version" to libs.versions.fabric.loader.get(),
        "minecraft_compat" to project.property("minecraft_compat")!!,
        "java_version" to javaVersion,
    )

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "loader_version" to libs.versions.fabric.loader.get(),
            "minecraft_compat" to project.property("minecraft_compat")!!,
            "java_version" to javaVersion
        )
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

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()

            from(components["java"])

            pom {
                name.set("Test Mod")
                description.set("This is an example description! Tell everyone what your mod is about!")
            }
        }
    }

    // automatically use DEPLOY_URL, DEPLOY_USER and DEPLOY_PASSWORD environment variables
    // or mavenHost, mavenUser and mavenPassword from props
    buildUtils.setupPublishRepository(repositories, props)
}