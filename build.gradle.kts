import java.util.zip.ZipFile
import java.security.MessageDigest
import java.time.Instant
import java.nio.file.Files

plugins {
    idea
    `maven-publish`
    jacoco
    id("net.minecraftforge.gradle") version "6.0.54"
    id("org.parchmentmc.librarian.forgegradle") version "1.2.0"
    id("org.spongepowered.mixin") version "0.7.38"
}

group = "com.bettercontent"
version = property("mod_version") as String
val mixinExtrasVersion = "0.5.0"
// Native optional-absence profile; Ars Nouveau requires Curios and cannot be present in it.
val backpackWithoutCurios = providers.gradleProperty("backpackWithoutCurios").map(String::toBoolean).getOrElse(false)
val spoilageWithoutHeat = providers.gradleProperty("spoilageWithoutHeat").map(String::toBoolean).getOrElse(false)
base {
    archivesName.set(property("artifact_name") as String)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

// Process only the two chopping hooks with Mixin AP; all other production hooks retain
// their existing compilation/remapping path and bytecode.
val choppingPaths = listOf(
    "com/bettercontent/bettercompatfixes/mixin/minecraft/ChoppingProgressMixin.java",
    "com/bettercontent/bettercompatfixes/mixin/minecraft/ChoppingHitEffectsMixin.java"
)
val choppingMixins by sourceSets.creating {
    java.setSrcDirs(listOf("src/main/java"))
    java.setIncludes(choppingPaths)
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}
sourceSets.main { java.exclude(choppingPaths) }
// Track the AP's actual task-named output, not the final packaged refmap name. Always
// process both tiny hooks together so classpath-only recompilation cannot leave a stale map.
tasks.named<JavaCompile>(choppingMixins.compileJavaTaskName) {
    options.isIncremental = false
    val refmap = layout.buildDirectory.file("tmp/compileChoppingMixinsJava/compileChoppingMixinsJava-refmap.json")
    outputs.file(refmap)
    doLast { check(refmap.get().asFile.isFile) { "Chopping annotation processor did not produce its refmap" } }
}

// Mirror the pack's authored disabled optional fillet recipe only in source GameTests.
// This source set is never included in the production JAR.
val nativeFixture by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}
val authoredStarcatcherFillet = file("../../better-content-modpack/kubejs/data/starcatcher/recipes/aquaculture_fillet.json")
// Exact authored pack repairs for malformed optional upstream data, never shipping fixes here.
val authoredRespiteOverrides = listOf(
    "recipes/filling/water_cup.json", "recipes/pouring/water_cup.json",
    "recipes/emptying/water_cup.json", "loot_tables/blocks/kettle.json",
    "advancements/main/stunt_tea_bush.json"
).associateWith { file("../../better-content-modpack/kubejs/data/farmersrespite/$it") }
tasks.named<ProcessResources>(nativeFixture.processResourcesTaskName) {
    from(authoredStarcatcherFillet) { into("data/starcatcher/recipes") }
    authoredRespiteOverrides.forEach { (relative, authored) ->
        from(authored) { into("data/farmersrespite/${relative.substringBeforeLast('/')}") }
    }
    doLast { check(authoredStarcatcherFillet.isFile) { "Missing authored disabled Starcatcher recipe" } }
}

minecraft {
    mappings("official", property("minecraft_version") as String)
    copyIdeResources = true
    jarJar.enable()

    runs {
        configureEach {
            workingDirectory(project.file("run"))
            property("forge.logging.console.level", "debug")
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "${projectDir}/build/createSrgToMcp/output.srg")
            mods {
                create(property("mod_id") as String) {
                    source(sourceSets.main.get())
                    source(choppingMixins)
                }
            }
        }
        create("client")
        create("server") { arg("--nogui") }
        create("gameTestServer") {
            workingDirectory(project.file("run-gametest"))
            property("forge.enableGameTest", "true")
            property("forge.gameTestServer", "true")
            property("forge.enabledGameTestNamespaces", property("mod_id") as String)
            property("better_compat_fixes.expectCuriosAbsent", backpackWithoutCurios.toString())
            property("better_compat_fixes.expectHeatAbsent", spoilageWithoutHeat.toString())
            mods { named(property("mod_id") as String) { source(nativeFixture) } }
            // Mixin's actual launcher option, scoped only to this nonshipping native source run.
            args("--mixin.config", "better_compat_fixes.native_fixture.mixins.json")
            arg("--nogui")
        }
    }
}

// CI and fresh-release builds provide verified runtime JARs explicitly.
// Ordinary local builds retain the canonical sibling build/libs convention.
fun betterContentJar(repository: String, artifact: String): java.io.File {
    val directory = providers.environmentVariable("BC_CUSTOM_MOD_JAR_DIR").orNull
    require(directory == null || directory.isNotBlank()) { "BC_CUSTOM_MOD_JAR_DIR must not be blank" }
    val jar = if (directory == null) file("../$repository/build/libs/$artifact") else file(directory).resolve(artifact)
    require(jar.isFile) {
        "Missing Better Content provider $artifact at $jar; prepare BC_CUSTOM_MOD_JAR_DIR or build $repository first"
    }
    return jar
}

val heatProvider = if (spoilageWithoutHeat) null else betterContentJar("better-industrial-heat", "better-industrial-heat-0.1.0.jar")
repositories {
    if (heatProvider != null) ivy {
        name = "industrialHeatLocal"
        url = uri(heatProvider.parentFile)
        patternLayout { artifact("[artifact]-[revision].[ext]") }
        metadataSources { artifact() }
        content { includeGroup("bettercontent.local.heat") }
    }
    maven("https://maven.minecraftforge.net")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.createmod.net")
    maven("https://maven.ithundxr.dev/mirror")
    maven("https://maven.tterrag.com/")
    maven("https://harleyoconnor.com/maven")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    maven("https://maven.llamalad7.mixinextras.org/releases/")
    maven("https://maven.valkyrienskies.org") { content { includeGroup("org.valkyrienskies.core") } }
    maven("https://www.cursemaven.com") { content { includeGroup("curse.maven") } }
    mavenCentral()
}

val pneumaticNativePins = configurations.create("pneumaticNativePins") { isTransitive = false }
val dhDepthNativePins = configurations.create("dhDepthNativePins") { isTransitive = false }
val markerRenderNativePins = configurations.create("markerRenderNativePins") { isTransitive = false }
val quarkSortNativePins = configurations.create("quarkSortNativePins") { isTransitive = false }
val backpackEquipmentNativePins = configurations.create("backpackEquipmentNativePins") { isTransitive = false }
val starcatcherNativePins = configurations.create("starcatcherNativePins") { isTransitive = false }
val heatNativePins = configurations.create("heatNativePins") { isTransitive = false }
val aetherRenderNativePins = configurations.create("aetherRenderNativePins") { isTransitive = false }

dependencies {
    add(choppingMixins.annotationProcessorConfigurationName, "org.spongepowered:mixin:0.8.5:processor")
    add(aetherRenderNativePins.name, "curse.maven:the-aether-255308:6134920")
    add(heatNativePins.name, "curse.maven:kotlin-for-forge-351264:7291067")
    add(heatNativePins.name, "curse.maven:chemlib-340666:5128632")
    add(backpackEquipmentNativePins.name, "curse.maven:curios-api-309927:6418456")
    add(backpackEquipmentNativePins.name, "curse.maven:sophisticated-backpacks-422301:7916619")
    compileOnly(fg.deobf("curse.maven:quark-243121:6427817"))
    compileOnly(fg.deobf("curse.maven:zeta-968868:7335229"))
    add(quarkSortNativePins.name, "curse.maven:quark-243121:6427817")
    add(quarkSortNativePins.name, "curse.maven:zeta-968868:7335229")
    add(quarkSortNativePins.name, "curse.maven:sophisticated-core-618298:7916595")
    add(quarkSortNativePins.name, "curse.maven:curios-api-309927:6418456")
    add(markerRenderNativePins.name, "curse.maven:realistic-block-physics-375616:6393411")
    add(markerRenderNativePins.name, "curse.maven:realistic-physics-1030082:6026115")
    add(markerRenderNativePins.name, "curse.maven:epic-fight-mod-405076:8049910")
    compileOnly(fg.deobf("curse.maven:realistic-block-physics-375616:6393411"))
    compileOnly(fg.deobf("curse.maven:realistic-physics-1030082:6026115"))
    add(dhDepthNativePins.name, "curse.maven:oculus-581495:6020952")
    add(dhDepthNativePins.name, "curse.maven:distant-horizons-508933:7375280")
    add(pneumaticNativePins.name, "curse.maven:pretty-pipes-376737:4769646")
    add(pneumaticNativePins.name, "curse.maven:pneumaticcraft-repressurized-281849:7307654")
    add(starcatcherNativePins.name, "curse.maven:starcatcher-1357603:7582886")
    compileOnly(fg.deobf("curse.maven:starcatcher-1357603:7582886"))
    runtimeOnly(fg.deobf("curse.maven:starcatcher-1357603:7582886"))
    if (!spoilageWithoutHeat) {
        runtimeOnly(fg.deobf("bettercontent.local.heat:better-industrial-heat:0.1.0"))
        // Actual pack pins, not Heat's lower compatible development floor.
        runtimeOnly(fg.deobf("curse.maven:kotlin-for-forge-351264:7291067"))
        runtimeOnly(fg.deobf("curse.maven:chemlib-340666:5128632"))
    }
    minecraft("net.minecraftforge:forge:${property("minecraft_version")}-${property("forge_version")}")
    compileOnly(files(betterContentJar("better-gameplay-notices", "better-gameplay-notices-1.0.0.jar")))
    runtimeOnly(files(betterContentJar("better-gameplay-notices", "better-gameplay-notices-1.0.0.jar")))
    compileOnly(files(betterContentJar("better-survival-hud", "better-survival-hud-1.0.0.jar")))
    compileOnly(fg.deobf("curse.maven:pneumaticcraft-repressurized-281849:7307654"))
    runtimeOnly(fg.deobf("curse.maven:pneumaticcraft-repressurized-281849:7307654"))
    runtimeOnly(fg.deobf("curse.maven:pollution-of-the-realms-269973:8554528"))
    compileOnly(fg.deobf("curse.maven:forgeendertech-244844:8554308"))
    runtimeOnly(fg.deobf("curse.maven:forgeendertech-244844:8554308"))
    implementation(fg.deobf("com.simibubi.create:create-${property("minecraft_version")}:6.0.8-291:slim"))
    implementation(fg.deobf("net.createmod.ponder:Ponder-Forge-${property("minecraft_version")}:1.0.92"))
    compileOnly(fg.deobf("dev.engine-room.flywheel:flywheel-forge-api-${property("minecraft_version")}:1.0.5"))
    runtimeOnly(fg.deobf("dev.engine-room.flywheel:flywheel-forge-${property("minecraft_version")}:1.0.5"))
    implementation(fg.deobf("com.tterrag.registrate:Registrate:MC1.20-1.3.3"))
    runtimeOnly(fg.deobf("maven.modrinth:create-sifting:1.20.1-1.8.6-6.0.6"))
    compileOnly(fg.deobf("curse.maven:the-deep-void-982999:8507128"))
    runtimeOnly(fg.deobf("curse.maven:the-deep-void-982999:8507128"))
    testImplementation(fg.deobf("curse.maven:the-deep-void-982999:8507128"))
    compileOnly(fg.deobf("curse.maven:hyle-609850:7736352"))
    compileOnly(fg.deobf("curse.maven:thirst-was-taken-679270:6660408"))
    compileOnly(fg.deobf("curse.maven:cold-sweat-506194:7893262"))
    compileOnly(fg.deobf("curse.maven:pollution-of-the-realms-269973:8554528"))
    compileOnly(fg.deobf("curse.maven:little-logistics-570050:4799459"))
    compileOnly(fg.deobf("curse.maven:weather-storms-tornadoes-237746:5244118"))
    runtimeOnly(fg.deobf("curse.maven:weather-storms-tornadoes-237746:5244118"))
    compileOnly(fg.deobf("curse.maven:coroutil-237749:5096038"))
    runtimeOnly(fg.deobf("curse.maven:coroutil-237749:5096038"))
    compileOnly(fg.deobf("curse.maven:serene-seasons-291874:6398227"))
    runtimeOnly(fg.deobf("curse.maven:serene-seasons-291874:6398227"))
    runtimeOnly(fg.deobf("curse.maven:glitchcore-955399:5787839"))
    compileOnly(fg.deobf("curse.maven:creativecore-257814:7649757"))
    compileOnly(fg.deobf("curse.maven:ambientsounds-254284:7550220"))
    compileOnly(fg.deobf("curse.maven:oculus-581495:6020952"))
    compileOnly(fg.deobf("curse.maven:sophisticated-core-618298:7916595"))
    compileOnly(fg.deobf("curse.maven:sophisticated-storage-619320:7973265"))
    compileOnly(fg.deobf("curse.maven:sophisticated-backpacks-422301:7916619"))
    compileOnly(fg.deobf("curse.maven:curios-api-309927:6418456"))
    compileOnly(fg.deobf("curse.maven:mantle-74924:7563777"))
    compileOnly(fg.deobf("curse.maven:tinkers-construct-74072:7449219"))
    compileOnly(fg.deobf("curse.maven:polymorph-388800:6450982"))
    compileOnly(fg.deobf("curse.maven:architectury-api-419699:5137938"))
    compileOnly(fg.deobf("curse.maven:epic-fight-mod-405076:8049910"))
    runtimeOnly(fg.deobf("curse.maven:epic-fight-mod-405076:8049910"))
    compileOnly(fg.deobf("curse.maven:parcool-482378:7760589"))
    compileOnly(fg.deobf("curse.maven:sleeping-overhaul-2-887716:6471182"))
    compileOnly(fg.deobf("curse.maven:distant-horizons-508933:7375280"))
    compileOnly(fg.deobf("curse.maven:structure-generation-improver-1473087:8102460"))
    compileOnly(fg.deobf("curse.maven:yungs-better-caves-340583:8686226"))
    compileOnly(fg.deobf("curse.maven:fallout-wastelands-431248:7127023"))
    compileOnly(fg.deobf("curse.maven:untamed-wilds-au-naturel-1399279:7441092"))
    compileOnly(fg.deobf("curse.maven:the-twilight-forest-227639:5468648"))
    compileOnly(fg.deobf("curse.maven:explosion-overhaul-a-new-level-of-destruction-1296203:7659431"))
    compileOnly(fg.deobf("curse.maven:valkyrien-skies-258371:7906689"))
    compileOnly(fg.deobf("curse.maven:timeless-and-classics-zero-1028108:8141310"))
    runtimeOnly(fg.deobf("curse.maven:sleeping-overhaul-2-887716:6471182"))
    runtimeOnly(fg.deobf("curse.maven:yungs-api-421850:5769971"))
    runtimeOnly(fg.deobf("curse.maven:yungs-better-caves-340583:8686226"))
    compileOnly(fg.deobf("curse.maven:patchouli-306770:7731017"))
    compileOnly("org.valkyrienskies.core:api:1.1.0+cf208d8b56")
    runtimeOnly(fg.deobf("curse.maven:thirst-was-taken-679270:6660408"))
    runtimeOnly(fg.deobf("curse.maven:realistic-block-physics-375616:6393411"))
    runtimeOnly(fg.deobf("curse.maven:realistic-physics-1030082:6026115"))
    if (!backpackWithoutCurios) {
        runtimeOnly(fg.deobf("curse.maven:curios-api-309927:6418456"))
        runtimeOnly(fg.deobf("curse.maven:ars-nouveau-401955:6688854"))
    }
    runtimeOnly(fg.deobf("curse.maven:architectury-api-419699:5137938"))
    runtimeOnly(fg.deobf("curse.maven:mantle-74924:7563777"))
    runtimeOnly(fg.deobf("curse.maven:tinkers-construct-74072:7449219"))
    runtimeOnly(fg.deobf("curse.maven:polymorph-388800:6450982"))
    runtimeOnly(fg.deobf("curse.maven:geckolib-388172:7553267"))
    runtimeOnly(fg.deobf("curse.maven:sophisticated-core-618298:7916595"))
    runtimeOnly(fg.deobf("curse.maven:sophisticated-storage-619320:7973265"))
    runtimeOnly(fg.deobf("curse.maven:sophisticated-backpacks-422301:7916619"))
    runtimeOnly(fg.deobf("curse.maven:farmers-delight-398521:8007609"))
    runtimeOnly(fg.deobf("curse.maven:farmers-respite-551453:5543547"))
    runtimeOnly(fg.deobf("curse.maven:brewin-and-chewin-637808:6237884"))
    runtimeOnly(fg.deobf("curse.maven:patchouli-306770:7731017"))
    compileOnly(fg.deobf("com.ferreusveritas.dynamictrees:DynamicTrees-1.20.1:1.4.10"))
    runtimeOnly(fg.deobf("com.ferreusveritas.dynamictrees:DynamicTrees-1.20.1:1.4.10"))
    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:$mixinExtrasVersion")!!)
    implementation(jarJar("io.github.llamalad7:mixinextras-forge:[$mixinExtrasVersion,0.6.0)")!!)
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("com.google.code.gson:gson:2.10.1")
    testImplementation(fg.deobf("curse.maven:pneumaticcraft-repressurized-281849:7307654"))
    // Pretty Pipes embeds this exact graph API via JarJar; compile-only avoids a second runtime copy.
    compileOnly("org.jgrapht:jgrapht-core:1.5.2")
    compileOnly(fg.deobf("curse.maven:pretty-pipes-376737:4769646"))
    runtimeOnly(fg.deobf("curse.maven:pretty-pipes-376737:4769646"))
    testImplementation(fg.deobf("curse.maven:pretty-pipes-376737:4769646"))
}

tasks.named<Jar>("jar") {
    from(choppingMixins.output)
    manifest.attributes("MixinConfigs" to "better_compat_fixes.mixins.json,better_compat_fixes.chopping.mixins.json")
    filesMatching("better_compat_fixes.chopping.refmap.json") { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
    finalizedBy("reobfJar")
}

val stageRuntimeJar by tasks.registering(Copy::class) {
    group = "build"
    description = "Stages the reobfuscated runtime jar into build/libs using the canonical release filename."
    dependsOn(tasks.named("reobfJar"))
    mustRunAfter(tasks.named("jarJar"))
    mustRunAfter(tasks.named("reobfJarJar"))
    from(layout.buildDirectory.file("reobfJar/output.jar"))
    into(layout.buildDirectory.dir("libs"))
    rename { "${base.archivesName.get()}-$version.jar" }
}

tasks.named("assemble") {
    dependsOn(stageRuntimeJar)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.register("headlessGameTest") {
    group = "verification"
    description = "Runs Forge game tests in a headless dedicated server."
    dependsOn(tasks.named("runGameTestServer"))
}

tasks.register("verifyFast") {
    group = "verification"
    description = "Runs deterministic unit/resource checks without Forge game tests."
    dependsOn(tasks.named("check"))
}

val verifyNativeDiagnostics by tasks.registering {
    group = "verification"
    dependsOn(tasks.named("headlessGameTest"))
    doLast {
        val nativeLog = file("run-gametest/logs/latest.log")
        check(nativeLog.isFile) { "Missing current native GameTest diagnostics" }
        val errors = nativeLog.readLines().filter { line ->
            listOf("/ERROR]", "/FATAL]", "[ERROR]", "[FATAL]", "Mixin apply failed").any(line::contains)
        }
        check(errors.isEmpty()) { "Native GameTest failed-operation/ERROR/FATAL diagnostics: ${errors.take(8).joinToString("\n")}" }
    }
}
tasks.register("verifyFull") {
    group = "verification"
    description = "Runs the full verification lane including headless Forge game tests and strict native diagnostics."
    dependsOn(tasks.named("verifyFast"))
    dependsOn(verifyNativeDiagnostics)
}

val resetGameTestMods = tasks.register("resetGameTestMods") {
    doLast {
        val retained = file("run-gametest/retained/${Instant.now().toEpochMilli()}")
        for (name in listOf("mods", "world", "logs")) {
            val previous = file("run-gametest/$name")
            if (previous.exists()) {
                retained.mkdirs()
                Files.move(previous.toPath(), retained.resolve(name).toPath())
            }
        }
    }
}

val syncGameTestStructures = tasks.register<Sync>("syncGameTestStructures") {
    from(layout.projectDirectory.dir("src/main/resources/gameteststructures"))
    into(layout.projectDirectory.dir("run-gametest/gameteststructures"))
}

tasks.matching { it.name.startsWith("prepareRunGameTestServer") }.configureEach {
    dependsOn(resetGameTestMods)
    dependsOn(syncGameTestStructures)
    dependsOn(tasks.named(nativeFixture.processResourcesTaskName))
}

// The optional hook must remain safe for consumers without Deep Void, but this owner's full
// gate must actually execute its pinned native regression rather than silently skip registration.
tasks.matching { it.name == "runGameTestServer" }.configureEach {
    doFirst {
        val heatPresent = configurations.runtimeClasspath.get().files.any { file ->
            file.isFile && file.extension == "jar" && ZipFile(file).use { zip ->
                zip.getEntry("com/bettercontent/betterindustrialheat/HeatSyncMod.class") != null
            }
        }
        check(heatPresent != spoilageWithoutHeat) { "Native Heat profile classpath does not match requested presence/absence" }
        val curiosPresent = configurations.runtimeClasspath.get().files.any { file ->
            file.isFile && file.extension == "jar" && ZipFile(file).use { zip ->
                zip.getEntry("top/theillusivec4/curios/api/CuriosApi.class") != null
            }
        }
        check(curiosPresent != backpackWithoutCurios) { "Native Curios profile classpath does not match the requested presence/absence" }
        check(configurations.runtimeClasspath.get().files.any { file ->
            file.isFile && file.extension == "jar" && ZipFile(file).use { zip ->
                zip.getEntry("net/mcreator/thedeepvoid/procedures/IfPlayerFallsInVoidProcedure.class") != null &&
                    zip.getEntry("META-INF/mods.toml")?.let { entry ->
                        zip.getInputStream(entry).use { it.readBytes() }.toString(Charsets.UTF_8)
                            .contains("version=\"1.98.1\"")
                    } == true
            }
        }) { "Required pinned Deep Void 1.98.1 native GameTest fixture is missing" }
    }
}

tasks.processResources {
    val props = mapOf(
        "minecraft_version" to project.property("minecraft_version"),
        "forge_version" to project.property("forge_version"),
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name"),
        "mod_version" to project.property("mod_version")
    )
    inputs.properties(props)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(props)
    }
}

mixin {
    add(choppingMixins, "better_compat_fixes.chopping.refmap.json")
    config("better_compat_fixes.mixins.json")
    config("better_compat_fixes.chopping.mixins.json")
}
// Standard dev-module resource placement, separate from processResources to avoid a cycle.
val syncChoppingRefmap by tasks.registering(Copy::class) {
    dependsOn(tasks.named(choppingMixins.compileJavaTaskName), tasks.named("processResources"))
    from(layout.buildDirectory.file("tmp/compileChoppingMixinsJava/compileChoppingMixinsJava-refmap.json"))
    rename { "better_compat_fixes.chopping.refmap.json" }
    into(layout.buildDirectory.dir("resources/main"))
    doLast {
        check(layout.buildDirectory.file("resources/main/better_compat_fixes.chopping.refmap.json").get().asFile.isFile) {
            "Missing annotation-processor generated chopping refmap"
        }
    }
}
tasks.withType<JavaCompile>().configureEach {
    if (name != "compileJava" && name != choppingMixins.compileJavaTaskName) dependsOn(syncChoppingRefmap)
}
tasks.withType<Test>().configureEach { dependsOn(syncChoppingRefmap) }
tasks.withType<Jar>().configureEach { dependsOn(syncChoppingRefmap) }
tasks.matching { it.name == "addMixinsToJar" || it.name.startsWith("prepareRun") }
    .configureEach { dependsOn(syncChoppingRefmap) }

val verifyRuntimeDispenserAlias by tasks.registering {
    group = "verification"
    description = "Requires the runtime JAR dispenser initializer expression hook without a final-field write bridge."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            val mixin = zip.getEntry("com/bettercontent/bettercompatfixes/mixin/minecraft/DispenserBlockMixin.class")
                ?: throw GradleException("Runtime JAR is missing DispenserBlockMixin: $runtimeJar")
            val bytecode = zip.getInputStream(mixin).use { it.readBytes() }.toString(Charsets.ISO_8859_1)
            check(bytecode.contains("Util;make(Ljava/lang/Object;Ljava/util/function/Consumer;)Ljava/lang/Object;")
                    && bytecode.contains("Util;m_137469_(Ljava/lang/Object;Ljava/util/function/Consumer;)Ljava/lang/Object;")
                    && bytecode.contains("ModifyExpressionValue") && bytecode.contains("synchronizedMap")
                    && !bytecode.contains("WrapOperation") && !bytecode.contains("PUTSTATIC")) {
                "Runtime dispenser mixin must wrap the native initializer result, never the final-field write: $runtimeJar"
            }
        }
    }
}

val verifyRuntimeSprintBridge by tasks.registering {
    group = "verification"
    description = "Requires the staged runtime JAR to disable directional dodge without replacing vanilla sprint."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            check(zip.getEntry(
                "com/bettercontent/bettercompatfixes/mixin/minecraft/LocalPlayerSprintMixin.class") == null) {
                "Runtime JAR still contains the obsolete LocalPlayer sprint injector: $runtimeJar"
            }
            for (obsolete in listOf(
                "client/ParCoolDirectionalDodgeClient.class",
                "client/DirectionalDoubleTapTracker.class",
                "client/VanillaDoubleTapSprintSuppressor.class"
            )) {
                check(zip.getEntry("com/bettercontent/bettercompatfixes/$obsolete") == null) {
                    "Runtime JAR still contains obsolete directional double-tap code $obsolete: $runtimeJar"
                }
            }
            check(zip.getEntry(
                "com/bettercontent/bettercompatfixes/mixin/parcool/DodgeMixin.class") != null) {
                "Runtime JAR is missing the ParCool native double-tap suppression mixin: $runtimeJar"
            }

            val mixinConfig = zip.getEntry("better_compat_fixes.mixins.json")
                ?: throw GradleException("Runtime JAR is missing its mixin configuration: $runtimeJar")
            val mixins = zip.getInputStream(mixinConfig).use { it.readBytes() }.toString(Charsets.UTF_8)
            check(!mixins.contains("LocalPlayerSprintMixin")) {
                "Runtime mixin configuration still enables the obsolete LocalPlayer sprint injector: $runtimeJar"
            }
        }
    }
}

val verifyRuntimeBetterCavesBounds by tasks.registering {
    group = "verification"
    description = "Requires the reobfuscated runtime JAR to retain the exact Better Caves bounds hook."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            fun classBytes(path: String): String {
                val entry = zip.getEntry(path) ?: throw GradleException("Runtime JAR is missing $path: $runtimeJar")
                return zip.getInputStream(entry).use { it.readBytes() }.toString(Charsets.ISO_8859_1)
            }

            val bounds = classBytes(
                "com/bettercontent/bettercompatfixes/compat/BetterCavesCarvingBounds.class")
            check(bounds.contains("m_151570_")) {
                "Runtime Better Caves bounds guard does not use the reobfuscated build-height predicate: $runtimeJar"
            }

            val mixin = classBytes(
                "com/bettercontent/bettercompatfixes/mixin/bettercaves/AbstractCarverMixin.class")
            check(mixin.contains("com.yungnickyoung.minecraft.bettercaves.worldgen.carver.AbstractCarver")
                    && mixin.contains("carveBlock")
                    && mixin.contains("BetterCavesCarvingBounds")) {
                "Runtime Better Caves mixin lacks its exact target or bounds guard: $runtimeJar"
            }
        }
    }
}

tasks.named("verifyFast") {
}

val verifyRuntimeLostCitiesSerialization by tasks.registering {
    group = "verification"
    description = "Requires the reobfuscated runtime JAR to retain dimension-scoped Lost Cities/C2ME serialization."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            fun classBytes(path: String): String {
                val entry = zip.getEntry(path) ?: throw GradleException("Runtime JAR is missing $path: $runtimeJar")
                return zip.getInputStream(entry).use { it.readBytes() }.toString(Charsets.ISO_8859_1)
            }

            val serialization = classBytes(
                "com/bettercontent/bettercompatfixes/compat/LostCitiesC2meDhSerialization.class")
            check(serialization.contains("lostcities")
                    && serialization.contains("lostcity")
                    && serialization.contains("c2me")
                    && !serialization.contains("distanthorizons")
                    && serialization.contains("ReentrantLock")
                    && serialization.contains("runSerialized")
                    && serialization.contains("callSerialized")) {
                "Runtime Lost Cities serialization must require only Lost Cities/C2ME and retain its shared reentrant lock: $runtimeJar"
            }

            val config = classBytes(
                "com/bettercontent/bettercompatfixes/config/BcFixesConfig.class")
            check(config.contains("LostCitiesC2meDhSerialization")
                    && config.contains("dependenciesAvailable")) {
                "Runtime Lost Cities config gate is not linked to the corrected dependency policy: $runtimeJar"
            }

            val chunkGenerator = classBytes(
                "com/bettercontent/bettercompatfixes/mixin/lostcities/ChunkGeneratorMixin.class")
            val lostCityFeature = classBytes(
                "com/bettercontent/bettercompatfixes/mixin/lostcities/LostCityFeatureMixin.class")
            val sectionBounds = classBytes(
                "com/bettercontent/bettercompatfixes/compat/LostCitiesSectionBounds.class")
            val chunkDriver = classBytes(
                "com/bettercontent/bettercompatfixes/mixin/lostcities/ChunkDriverMixin.class")
            check(chunkGenerator.contains("shouldSerialize")
                    && chunkGenerator.contains("runSerialized")
                    && lostCityFeature.contains("shouldSerialize")
                    && lostCityFeature.contains("callSerialized")) {
                "Runtime Lost Cities decoration and feature wrappers do not share the serialization helper: $runtimeJar"
            }
            check(sectionBounds.contains("m_151570_")
                    && chunkDriver.contains("mcjty.lostcities.worldgen.ChunkDriver")
                    && chunkDriver.contains("getBlockSafe")
                    && chunkDriver.contains("setBlock")
                    && chunkDriver.contains("LostCitiesSectionBounds")) {
                "Runtime Lost Cities section-cache boundary hook did not survive reobfuscation: $runtimeJar"
            }

            val mixinConfig = zip.getEntry("better_compat_fixes.mixins.json")
                ?: throw GradleException("Runtime JAR is missing its mixin configuration: $runtimeJar")
            val mixins = zip.getInputStream(mixinConfig).use { it.readBytes() }.toString(Charsets.UTF_8)
            check(mixins.contains("lostcities.ChunkGeneratorMixin")
                    && mixins.contains("lostcities.ChunkDriverMixin")
                    && mixins.contains("lostcities.LostCityFeatureMixin")) {
                "Runtime mixin configuration is missing a Lost Cities serialization wrapper: $runtimeJar"
            }
        }
    }
}

val verifyRuntimeFalloutStructureBounds by tasks.registering {
    group = "verification"
    description = "Requires the reobfuscated runtime JAR to retain Fallout's bounded template-placement redirect."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            fun classBytes(path: String): String {
                val entry = zip.getEntry(path) ?: throw GradleException("Runtime JAR is missing $path: $runtimeJar")
                return zip.getInputStream(entry).use { it.readBytes() }.toString(Charsets.ISO_8859_1)
            }

            val bounds = classBytes(
                "com/bettercontent/bettercompatfixes/compat/FalloutStructurePlacementBounds.class")
            check(bounds.contains("WRITABLE_ENVELOPE_BLOCKS")
                    && bounds.contains("EDGE_UPDATE_MARGIN_BLOCKS")
                    && bounds.contains("PLACEABLE_TEMPLATE_BLOCKS")
                    && bounds.contains("fit")
                    && bounds.contains("m_71056_")
                    && bounds.contains("m_71058_")) {
                "Runtime Fallout bounds helper lacks its 48-block envelope and edge-update margin contract: $runtimeJar"
            }

            val mixin = classBytes(
                "com/bettercontent/bettercompatfixes/mixin/falloutwastelands/StructureFeatureMixin.class")
            check(mixin.contains("net.mcreator.falloutwastelands.world.features.StructureFeature")
                    && mixin.contains("m_142674_")
                    && mixin.contains("m_230328_")
                    && mixin.contains("FalloutStructurePlacementBounds")) {
                "Runtime Fallout mixin lacks its exact reobfuscated target or placement redirect: $runtimeJar"
            }

            val mixinConfig = zip.getEntry("better_compat_fixes.mixins.json")
                ?: throw GradleException("Runtime JAR is missing its mixin configuration: $runtimeJar")
            val mixins = zip.getInputStream(mixinConfig).use { it.readBytes() }.toString(Charsets.UTF_8)
            check(mixins.contains("falloutwastelands.StructureFeatureMixin")) {
                "Runtime mixin configuration is missing the Fallout placement redirect: $runtimeJar"
            }
        }
    }
}

val verifyRuntimeTwilightForestMazeSerialization by tasks.registering {
    group = "verification"
    description = "Requires the runtime JAR to retain the exact Twilight Forest/C2ME maze-random lock."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            fun classBytes(path: String): String {
                val entry = zip.getEntry(path) ?: throw GradleException("Runtime JAR is missing $path: $runtimeJar")
                return zip.getInputStream(entry).use { it.readBytes() }.toString(Charsets.ISO_8859_1)
            }

            val helper = classBytes(
                "com/bettercontent/bettercompatfixes/compat/TwilightForestMazeSerialization.class")
            check(helper.contains("twilightforest")
                    && helper.contains("c2me")
                    && helper.contains("runSerialized")) {
                "Runtime Twilight Forest helper lacks its exact dependency and monitor contract: $runtimeJar"
            }

            val mixin = classBytes(
                "com/bettercontent/bettercompatfixes/mixin/twilightforest/TFMazeMixin.class")
            check(mixin.contains("twilightforest.world.components.structures.TFMaze")
                    && mixin.contains("copyToStructure")
                    && mixin.contains("runSerialized")
                    && mixin.contains("rand")) {
                "Runtime Twilight Forest mixin lacks its exact maze placement/random-source wrapper: $runtimeJar"
            }

            val strongholdMixin = classBytes(
                "com/bettercontent/bettercompatfixes/mixin/twilightforest/ConquerableStructureMixin.class")
            check(strongholdMixin.contains("twilightforest.world.components.structures.util.ConquerableStructure")
                    && strongholdMixin.contains("KnightStrongholdStructure")
                    && strongholdMixin.contains("generateCustom")
                    && strongholdMixin.contains("callStrongholdSerialized")) {
                "Runtime Twilight Forest mixin lacks full Knight Stronghold generation serialization: $runtimeJar"
            }

            val mixinConfig = zip.getEntry("better_compat_fixes.mixins.json")
                ?: throw GradleException("Runtime JAR is missing its mixin configuration: $runtimeJar")
            val mixins = zip.getInputStream(mixinConfig).use { it.readBytes() }.toString(Charsets.UTF_8)
            check(mixins.contains("twilightforest.TFMazeMixin")) {
                "Runtime mixin configuration is missing the Twilight Forest maze wrapper: $runtimeJar"
            }
            check(mixins.contains("twilightforest.ConquerableStructureMixin")) {
                "Runtime mixin configuration is missing the Twilight Forest stronghold wrapper: $runtimeJar"
            }
        }
    }
}

val verifyRuntimeMonsterRoomSpawnerRecovery by tasks.registering {
    group = "verification"
    description = "Requires the runtime JAR to retain the exact protected-origin dungeon failure path."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            val entry = zip.getEntry(
                "com/bettercontent/bettercompatfixes/mixin/minecraft/MonsterRoomFeatureMixin.class")
                ?: throw GradleException("Runtime JAR is missing MonsterRoomFeatureMixin: $runtimeJar")
            val bytecode = zip.getInputStream(entry).use { it.readBytes() }.toString(Charsets.ISO_8859_1)
            check(bytecode.contains("MonsterRoomFeature")
                    && bytecode.contains("m_142674_")
                    && bytecode.contains("org/slf4j/Logger")
                    && bytecode.contains("CallbackInfoReturnable")
                    && bytecode.contains("setReturnValue")
                    && !bytecode.contains("SpawnerBlockEntity")
                    && !bytecode.contains("m_142169_")) {
                "Runtime dungeon correction lacks the exact failed-placement return path: $runtimeJar"
            }

            val config = zip.getEntry("better_compat_fixes.mixins.json")
                ?: throw GradleException("Runtime JAR is missing its mixin configuration: $runtimeJar")
            val mixins = zip.getInputStream(config).use { it.readBytes() }.toString(Charsets.UTF_8)
            check(mixins.contains("minecraft.MonsterRoomFeatureMixin")) {
                "Runtime mixin configuration is missing dungeon spawner recovery: $runtimeJar"
            }
        }
    }
}

val verifyRuntimeCreativeSearchTabMixin by tasks.registering {
    group = "verification"
    description = "Requires the Creative search-tab redirect to retain both development and production selectors."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            val entry = zip.getEntry(
                "com/bettercontent/bettercompatfixes/mixin/minecraft/CreativeModeInventoryScreenMixin.class")
                ?: throw GradleException("Runtime JAR is missing CreativeModeInventoryScreenMixin: $runtimeJar")
            val bytecode = zip.getInputStream(entry).use { it.readBytes() }.toString(Charsets.ISO_8859_1)
            check(bytecode.contains("init")
                    && bytecode.contains("m_7856_")
                    && bytecode.contains("CreativeModeTabRegistry")
                    && bytecode.contains("getSortedCreativeModeTabs")
                    && !bytecode.contains("CreativeModeTabs;tabs")) {
                "Runtime Creative tab redirect lacks its dual mapped selectors or Forge page source: $runtimeJar"
            }
            val config = zip.getEntry("better_compat_fixes.mixins.json")
                ?: throw GradleException("Runtime JAR is missing its mixin configuration: $runtimeJar")
            val configText = zip.getInputStream(config).use { it.readBytes() }.toString(Charsets.UTF_8)
            check(!configText.contains("\"refmap\"")) {
                "Runtime mixin configuration declares an unpackaged refmap: $runtimeJar"
            }
        }
    }
}

val verifyRuntimeWeather2ResidentQueries by tasks.registering {
    group = "verification"
    description = "Requires scoped non-loading Weather2 observations and both no-refmap biome selectors."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            fun bytecode(path: String) = zip.getInputStream(zip.getEntry(path)
                ?: throw GradleException("Runtime JAR is missing $path")).use { it.readBytes() }.toString(Charsets.ISO_8859_1)
            val storm = bytecode("com/bettercontent/bettercompatfixes/mixin/weather2/StormObjectMixin.class")
            check(storm.contains("Level;getBiome(") && storm.contains("Level;m_204166_(")
                    && storm.contains("initFirstTime") && storm.contains("tickProgression")
                    && storm.contains("Level;isLoaded(") && storm.contains("Level;m_46749_(")
                    && storm.contains("m_7131_")) {
                "Weather2 native creation/progression wrappers lack development/production selectors or resident FULL lookup"
            }
            val endertech = bytecode("com/bettercontent/bettercompatfixes/mixin/forgeendertech/WorldDataChunkReadinessMixin.class")
            check(endertech.contains("ChunkSource;hasChunk(II)Z") && endertech.contains("ChunkSource;m_5563_(II)Z")
                    && endertech.contains("ServerLevel;getChunk(II)") && endertech.contains("ServerLevel;m_6325_(II)")
                    && endertech.contains("m_7131_")) {
                "ForgeEndertech notification adapter lacks native entrypoints/production resident-chunk lookup"
            }
            val seasonal = bytecode("com/bettercontent/bettercompatfixes/mixin/weather2/CoroUtilCompatibilityMixin.class")
            check(seasonal.contains("getAdjustedTemperature") && seasonal.contains("Level;getBiome(") && seasonal.contains("Level;m_204166_(")) {
                "CoroUtil seasonal biome wrapper lacks native entrypoint/development/production selectors"
            }
            val queries = bytecode("com/bettercontent/bettercompatfixes/compat/Weather2LoadedChunkQueries.class")
            check(queries.contains("m_7131_") && queries.contains("m_203675_")) {
                "Weather2 observations must retain native resident-chunk and uncached-noise calls"
            }
            val config = bytecode("better_compat_fixes.mixins.json")
            check(config.contains("weather2.WeatherUtilBlockMixin") && config.contains("weather2.StormObjectMixin")
                    && config.contains("weather2.CoroUtilCompatibilityMixin")
                    && config.contains("forgeendertech.WorldDataChunkReadinessMixin")
                    && config.contains("forgeendertech.WorldDataAccessor"))
        }
    }
}

val verifyRuntimeDeepVoidFallBoundary by tasks.registering {
    group = "verification"
    description = "Requires actual-dimension fall entry to survive production reobfuscation."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            fun bytecode(path: String) = zip.getInputStream(zip.getEntry(path)
                ?: throw GradleException("Runtime JAR is missing $path")).use { it.readBytes() }.toString(Charsets.ISO_8859_1)
            val mixin = bytecode("com/bettercontent/bettercompatfixes/mixin/deepvoid/IfPlayerFallsInVoidProcedureMixin.class")
            check(mixin.contains("IfPlayerFallsInVoidProcedure") && mixin.contains("Double;doubleValue()D")
                    && mixin.contains("DeepVoidFallBoundary") && mixin.contains("m_9236_")
                    && mixin.contains("m_141937_") && mixin.contains("require") && mixin.contains("allow")) {
                "Deep Void fall expression lacks its native selector or production entity/height accessors"
            }
            check(bytecode("better_compat_fixes.mixins.json").contains("deepvoid.IfPlayerFallsInVoidProcedureMixin")) {
                "Runtime mixin configuration is missing actual-bottom Deep Void fall entry"
            }
            bytecode("com/bettercontent/bettercompatfixes/compat/DeepVoidFallBoundary.class")
        }
    }
}

val verifyPneumaticNativePins by tasks.registering {
    group = "verification"
    description = "Reproducibly fetch and hash-verify Pretty Pipes and PNCR native fixtures."
    doLast {
        val pins = mapOf("4769646" to "8e69a3719b89e67b0d26792d7ce0d77fbfaf02e4",
                "7307654" to "cbe2d3c5df7c2770ac59eb6f02489d8917627c03")
        val files = pneumaticNativePins.resolve()
        check(files.size == pins.size)
        pins.forEach { (id, expected) ->
            val native = files.single { it.name.contains(id) }
            val digest = MessageDigest.getInstance("SHA-1")
                .digest(native.readBytes()).joinToString("") { "%02x".format(it) }
            check(digest == expected) { "Native pneumatic fixture $id does not match committed pack pin" }
        }
    }
}
tasks.test {
    dependsOn(verifyPneumaticNativePins, verifyRuntimeDispenserAlias)
    inputs.files(dhDepthNativePins, markerRenderNativePins, quarkSortNativePins, backpackEquipmentNativePins,
        starcatcherNativePins, aetherRenderNativePins, authoredStarcatcherFillet, authoredRespiteOverrides.values)
    dependsOn(tasks.named(nativeFixture.classesTaskName))
    doFirst {
        systemProperty("bc.aetherRender.native", aetherRenderNativePins.resolve().single().absolutePath)
        systemProperty("bc.starcatcher.native", starcatcherNativePins.resolve().single().absolutePath)
        val backpackNatives = backpackEquipmentNativePins.resolve()
        for (id in listOf("6418456", "7916619")) {
            systemProperty("bc.backpackEquipment.$id", backpackNatives.single { it.name.contains(id) }.absolutePath)
        }
        val quarkNatives = quarkSortNativePins.resolve()
        for (id in listOf("6427817", "7335229", "7916595", "6418456")) {
            systemProperty("bc.quarkSort.$id", quarkNatives.single { it.name.contains(id) }.absolutePath)
        }
        val markerNatives = markerRenderNativePins.resolve()
        for (id in listOf("6393411", "6026115", "8049910")) {
            systemProperty("bc.markerRender.$id", markerNatives.single { it.name.contains(id) }.absolutePath)
        }
        val natives = dhDepthNativePins.resolve()
        systemProperty("bc.dhDepth.oculus", natives.single { it.name.contains("6020952") }.absolutePath)
        systemProperty("bc.dhDepth.dh", natives.single { it.name.contains("7375280") }.absolutePath)
    }
}
val verifySpoilageNativePins by tasks.registering {
    group = "verification"
    doLast {
        val pins = mapOf("7291067" to "962fdb760409d6d71cbf079235f1ca94e3863a22",
            "5128632" to "fe8feeed94c9cd7354c4a075ad81fc6f5e0a0d2c")
        // Starcatcher's SHA-256 is additionally checked by StarcatcherNativeFixtureTest.
        for (native in heatNativePins.resolve()) {
            val id = pins.keys.single { native.name.contains(it) }
            val digest = MessageDigest.getInstance("SHA-1").digest(native.readBytes()).joinToString("") { "%02x".format(it) }
            check(digest == pins.getValue(id)) { "Spoilage native dependency $id does not match actual pack pin" }
        }
    }
}
tasks.test { dependsOn(verifySpoilageNativePins) }
tasks.matching { it.name == "runGameTestServer" }.configureEach {
    dependsOn(verifyPneumaticNativePins, verifySpoilageNativePins)
}

val verifyRuntimePneumaticPressurizer by tasks.registering {
    group = "verification"
    description = "Requires native PNCR air/gauge integration and production selectors in the staged JAR."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            fun bytecode(path: String) = zip.getInputStream(zip.getEntry(path)
                ?: throw GradleException("Runtime JAR is missing $path")).use { it.readBytes() }.toString(Charsets.ISO_8859_1)
            val root = "com/bettercontent/bettercompatfixes/"
            val be = bytecode(root + "mixin/prettypipes/PressurizerBlockEntityMixin.class")
            check(be.contains("pressurizeItem") && be.contains("getCapability") && be.contains("createTierOneAirHandler")
                    && be.contains("m_183515_") && be.contains("m_142466_") && be.contains("m_7131_")
                    && be.contains("AIR_HANDLER_MACHINE_CAPABILITY") && be.contains("ENERGY")
                    && be.contains("better_compat_fixes:pressurizer_air")) {
                "Pressurizer air replacement lacks its production persistence/capability/resident-chunk targets"
            }
            val gui = bytecode(root + "mixin/prettypipes/PressurizerGuiMixin.class")
            check(gui.contains("PressureGaugeRenderer2D") && gui.contains("drawPressureGauge")
                    && gui.contains("WidgetTooltipArea") && gui.contains("printManometerMessage")
                    && gui.contains("m_88315_") && gui.contains("m_7286_") && gui.contains("m_280003_")) {
                "Pressurizer GUI must directly host PNCR's actual component on production screen selectors"
            }
            check(!gui.contains("info.prettypipes.energy") && !gui.contains("getEnergyPercentage"))
            check(bytecode(root + "mixin/prettypipes/PressurizerContainerMixin.class").contains("PressurizerMenuAir"))
            val config = bytecode("better_compat_fixes.mixins.json")
            check(config.contains("prettypipes.PressurizerBlockEntityMixin")
                    && config.contains("prettypipes.PressurizerContainerMixin")
                    && config.contains("prettypipes.PressurizerGuiMixin"))
        }
    }
}

val verifyRuntimeRespawnMarkerPhysics by tasks.registering {
    group = "verification"
    description = "Requires the exact RBP marker admission guard in the production JAR."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            fun bytes(path: String) = zip.getInputStream(zip.getEntry(path)
                ?: throw GradleException("Runtime JAR is missing $path")).use { it.readBytes() }
                .toString(Charsets.ISO_8859_1)
            val root = "com/bettercontent/bettercompatfixes/"
            val hook = bytes(root + "mixin/rbp/BlockOperationSchedulerMixin.class")
            check(hook.contains("xbigellx.rbp.internal.physics.BlockOperationScheduler")
                    && hook.contains("RPBlockContext;") && hook.contains("PhysicsEngineBehaviour;")
                    && hook.contains("RespawnMarkerPhysics") && hook.contains("blockState")
                    && hook.contains("cancel")) { "Production RBP admission guard lost its native target/selector" }
            val policy = bytes(root + "compat/RespawnMarkerPhysics.class")
            check(policy.contains("better_spawns") && policy.contains("respawn_marker")
                    && policy.contains("getKey") && policy.contains("m_60734_")) {
                "Production marker identity guard lacks its native registry key or mapped block accessor"
            }
            check(bytes("better_compat_fixes.mixins.json").contains("rbp.BlockOperationSchedulerMixin"))
        }
    }
}

val verifyRuntimeSwimmingAttackPov by tasks.registering {
    group = "verification"
    description = "Requires the client-only swimming attack POV selector in the production JAR."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            fun bytes(path: String) = zip.getInputStream(zip.getEntry(path)
                ?: throw GradleException("Runtime JAR is missing $path")).use { it.readBytes() }
                .toString(Charsets.ISO_8859_1)
            val root = "com/bettercontent/bettercompatfixes/"
            val hook = bytes(root + "mixin/epicfight/LocalPlayerAttackPovMixin.class")
            check(hook.contains("clientTick(Lnet/minecraftforge/event/entity/living/LivingEvent\$LivingTickEvent;)V")
                    && hook.contains("iterVisibleLayersUntilFalse(Ljava/util/function/Function;)Z")
                    && hook.contains("FirstPersonPovPriority") && hook.contains("AttackAnimation")
                    && hook.contains("POV_ANIMATION") && hook.contains("getFirstPersonLayer")
                    && hook.contains("SWIM") && hook.contains("FLOAT")) {
                "Production swimming attack hook lacks its native selector or scoped POV policy"
            }
            bytes(root + "client/FirstPersonPovPriority.class")
            check(bytes("better_compat_fixes.mixins.json").contains("epicfight.LocalPlayerAttackPovMixin"))
            check(bytes(root + "mixin/BetterContentMixinPlugin.class").contains("LocalPlayerAttackPovMixin"))
        }
    }
}
tasks.named("verifyFast") { dependsOn(verifyRuntimeSwimmingAttackPov) }

val verifyRuntimeDhOpaqueDepth by tasks.registering {
    group = "verification"
    description = "Requires the pinned client-only DH opaque-depth correction in the production JAR."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            fun bytes(path: String) = zip.getInputStream(zip.getEntry(path)
                ?: throw GradleException("Runtime JAR is missing $path")).use { it.readBytes() }
                .toString(Charsets.ISO_8859_1)
            val root = "com/bettercontent/bettercompatfixes/"
            val hook = bytes(root + "mixin/oculus/DHCompatInternalMixin.class")
            check(hook.contains("net.irisshaders.iris.compat.dh.DHCompatInternal")
                    && hook.contains("getDepthTexNoTranslucent()I") && hook.contains("storedDepthTex")
                    && hook.contains("DhOpaqueDepthPolicy") && hook.contains("DISABLED")) {
                "Production DH opaque-depth hook lost its target, selector or conditional policy"
            }
            bytes(root + "compat/DhOpaqueDepthPolicy.class")
            check(bytes("better_compat_fixes.mixins.json").contains("oculus.DHCompatInternalMixin"))
        }
    }
}

val verifyRuntimeChoppingSpoilage by tasks.registering {
    group = "verification"
    description = "Requires production chopping/spoilage hooks and excludes the source-only recipe fixture."
    dependsOn(stageRuntimeJar)
    doLast {
        ZipFile(layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile).use { zip ->
            check(zip.entries().asSequence().none {
                it.name.startsWith("com/bettercontent/bettercompatfixes/nativefixture/")
                    || it.name == "com/bettercontent/bettercompatfixes/gametest/DispenserRegistryGameTests.class"
                    || it.name == "better_compat_fixes.native_fixture.mixins.json"
            }) { "Source-only native registry accessor/test/config leaked into production" }
            check(zip.entries().asSequence().none {
                it.name.startsWith("data/farmersrespite/") || it.name.startsWith("data/the_deep_void/dimension/")
            }) { "Source-only native fixture overrides leaked into production" }
            check(zip.getEntry("data/starcatcher/recipes/aquaculture_fillet.json") == null) {
                "Source-only authored disabled-recipe fixture leaked into production"
            }
            for (entry in listOf("compat/ChoppingParticles", "compat/ChoppingProgress", "compat/SpoilageReuse",
                    "compat/StarcatcherSpoilage", "mixin/minecraft/ChoppingProgressMixin", "mixin/minecraft/ChoppingHitEffectsMixin")) {
                check(zip.getEntry("com/bettercontent/bettercompatfixes/$entry.class") != null)
            }
            check(zip.getEntry("data/starcatcher/tags/items/baits.json") != null)
        }
    }
}

tasks.named("verifyFast") {
    dependsOn(verifyRuntimeChoppingSpoilage)
    dependsOn(verifyRuntimeRespawnMarkerPhysics)
    dependsOn(verifyRuntimeDhOpaqueDepth)
    dependsOn(verifyRuntimePneumaticPressurizer)
    dependsOn(verifyRuntimeDeepVoidFallBoundary)
    dependsOn(verifyRuntimeWeather2ResidentQueries)
    dependsOn(verifyRuntimeDispenserAlias)
    dependsOn(verifyRuntimeSprintBridge)
    dependsOn(verifyRuntimeBetterCavesBounds)
    dependsOn(verifyRuntimeLostCitiesSerialization)
    dependsOn(verifyRuntimeFalloutStructureBounds)
    dependsOn(verifyRuntimeTwilightForestMazeSerialization)
    dependsOn(verifyRuntimeMonsterRoomSpawnerRecovery)
    dependsOn(verifyRuntimeCreativeSearchTabMixin)
}

val verifyRuntimeBackpackEquipment by tasks.registering {
    group = "verification"
    description = "Requires exclusive native player backpack admission and production selectors in the staged JAR."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            fun bytes(path: String) = zip.getInputStream(zip.getEntry(path)
                ?: throw GradleException("Runtime JAR is missing $path")).use { it.readBytes() }
                .toString(Charsets.ISO_8859_1)
            val root = "com/bettercontent/bettercompatfixes/"
            val api = bytes(root + "mixin/backpackequipment/CuriosApiMixin.class")
            val handler = bytes(root + "mixin/backpackequipment/DynamicStackHandlerMixin.class")
            check(api.contains("isStackValid(Ltop/theillusivec4/curios/api/SlotContext;Lnet/minecraft/world/item/ItemStack;)Z")
                    && handler.contains("isItemValid(ILnet/minecraft/world/item/ItemStack;)Z")
                    && handler.contains("ctxBuilder") && handler.contains("RETURN") && api.contains("WrapMethod")
                    && api.contains("Operation"))
            val bag = bytes(root + "mixin/backpackequipment/BackpackItemMixin.class")
            check(bag.contains("canEquip") && bag.contains("m_147233_") && bag.contains("m_20743_")
                    && bag.contains("net/minecraft/world/entity/player/Player")) {
                "Native backpack armor guard lost its production Forge default/type calls"
            }
            val player = bytes(root + "mixin/backpackequipment/PlayerMixin.class")
            check(player.contains("canTakeItem(Lnet/minecraft/world/item/ItemStack;)Z")
                    && player.contains("m_7066_(Lnet/minecraft/world/item/ItemStack;)Z") && player.contains("RETURN"))
            val policy = bytes(root + "compat/BackpackEquipmentPolicy.class")
            check(policy.contains("better_backpack")
                    && policy.contains("PlayerLoggedInEvent") && policy.contains("BackpackArmorPolicy") && !policy.contains("m_8061_")
                    && !policy.contains("setStackInSlot"))
            val armorPolicy = bytes(root + "compat/BackpackArmorPolicy.class")
            check(armorPolicy.contains("BackpackItem") && armorPolicy.contains("PlayerLoggedInEvent")
                    && !armorPolicy.contains("top/theillusivec4/") && !player.contains("top/theillusivec4/")
                    && !bag.contains("top/theillusivec4/")) {
                "Native armor/dispenser support must not link optional Curios classes"
            }
            val config = bytes("better_compat_fixes.mixins.json")
            for (hook in listOf("CuriosApiMixin", "DynamicStackHandlerMixin", "BackpackItemMixin", "PlayerMixin")) {
                check(config.contains("backpackequipment.$hook"))
            }
        }
    }
}
tasks.named("verifyFast") { dependsOn(verifyRuntimeBackpackEquipment) }

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
