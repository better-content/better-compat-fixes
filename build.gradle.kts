import java.util.zip.ZipFile

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
base {
    archivesName.set(property("artifact_name") as String)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
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

repositories {
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

dependencies {
    minecraft("net.minecraftforge:forge:${property("minecraft_version")}-${property("forge_version")}")
    compileOnly(files(betterContentJar("better-content-notifications", "better-content-notifications-1.0.0.jar")))
    runtimeOnly(files(betterContentJar("better-content-notifications", "better-content-notifications-1.0.0.jar")))
    compileOnly(files(betterContentJar("dynamic-survival-hud", "dynamic-survival-hud-1.0.0.jar")))
    compileOnly(fg.deobf("curse.maven:pneumaticcraft-repressurized-281849:7307654"))
    runtimeOnly(fg.deobf("curse.maven:pneumaticcraft-repressurized-281849:7307654"))
    runtimeOnly(fg.deobf("curse.maven:pollution-of-the-realms-269973:8554528"))
    runtimeOnly(fg.deobf("curse.maven:forgeendertech-244844:8554308"))
    implementation(fg.deobf("com.simibubi.create:create-${property("minecraft_version")}:6.0.8-291:slim"))
    implementation(fg.deobf("net.createmod.ponder:Ponder-Forge-${property("minecraft_version")}:1.0.92"))
    compileOnly(fg.deobf("dev.engine-room.flywheel:flywheel-forge-api-${property("minecraft_version")}:1.0.5"))
    runtimeOnly(fg.deobf("dev.engine-room.flywheel:flywheel-forge-${property("minecraft_version")}:1.0.5"))
    implementation(fg.deobf("com.tterrag.registrate:Registrate:MC1.20-1.3.3"))
    runtimeOnly(fg.deobf("maven.modrinth:create-sifting:1.20.1-1.8.6-6.0.6"))
    compileOnly(fg.deobf("curse.maven:hyle-609850:7736352"))
    compileOnly(fg.deobf("curse.maven:thirst-was-taken-679270:6660408"))
    compileOnly(fg.deobf("curse.maven:cold-sweat-506194:7893262"))
    compileOnly(fg.deobf("curse.maven:pollution-of-the-realms-269973:8554528"))
    compileOnly(fg.deobf("curse.maven:little-logistics-570050:4799459"))
    compileOnly(fg.deobf("curse.maven:weather-storms-tornadoes-237746:5244118"))
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
    runtimeOnly(fg.deobf("curse.maven:curios-api-309927:6418456"))
    runtimeOnly(fg.deobf("curse.maven:architectury-api-419699:5137938"))
    runtimeOnly(fg.deobf("curse.maven:mantle-74924:7563777"))
    runtimeOnly(fg.deobf("curse.maven:tinkers-construct-74072:7449219"))
    runtimeOnly(fg.deobf("curse.maven:polymorph-388800:6450982"))
    runtimeOnly(fg.deobf("curse.maven:ars-nouveau-401955:6688854"))
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
}

tasks.named<Jar>("jar") {
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

tasks.register("verifyFull") {
    group = "verification"
    description = "Runs the full verification lane including headless Forge game tests."
    dependsOn(tasks.named("verifyFast"))
    dependsOn(tasks.named("headlessGameTest"))
}

val resetGameTestMods = tasks.register<Delete>("resetGameTestMods") {
    delete(layout.projectDirectory.dir("run-gametest/mods"))
    delete(layout.projectDirectory.dir("run-gametest/world"))
}

val syncGameTestStructures = tasks.register<Sync>("syncGameTestStructures") {
    from(layout.projectDirectory.dir("src/main/resources/gameteststructures"))
    into(layout.projectDirectory.dir("run-gametest/gameteststructures"))
}

tasks.matching { it.name.startsWith("prepareRunGameTestServer") }.configureEach {
    dependsOn(resetGameTestMods)
    dependsOn(syncGameTestStructures)
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
    config("better_content_fixes.mixins.json")
}

val verifyRuntimeDispenserAlias by tasks.registering {
    group = "verification"
    description = "Requires the runtime JAR dispenser mixin to retain its production SRG field alias."
    dependsOn(stageRuntimeJar)
    doLast {
        val runtimeJar = layout.buildDirectory.file("libs/${base.archivesName.get()}-$version.jar").get().asFile
        ZipFile(runtimeJar).use { zip ->
            val mixin = zip.getEntry("com/bettercontent/bettercontentfixes/mixin/minecraft/DispenserBlockMixin.class")
                ?: throw GradleException("Runtime JAR is missing DispenserBlockMixin: $runtimeJar")
            val bytecode = zip.getInputStream(mixin).use { it.readBytes() }.toString(Charsets.ISO_8859_1)
            check(bytecode.contains("f_52661_")) {
                "Runtime dispenser mixin lacks the production SRG alias f_52661_: $runtimeJar"
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
                "com/bettercontent/bettercontentfixes/mixin/minecraft/LocalPlayerSprintMixin.class") == null) {
                "Runtime JAR still contains the obsolete LocalPlayer sprint injector: $runtimeJar"
            }
            for (obsolete in listOf(
                "client/ParCoolDirectionalDodgeClient.class",
                "client/DirectionalDoubleTapTracker.class",
                "client/VanillaDoubleTapSprintSuppressor.class"
            )) {
                check(zip.getEntry("com/bettercontent/bettercontentfixes/$obsolete") == null) {
                    "Runtime JAR still contains obsolete directional double-tap code $obsolete: $runtimeJar"
                }
            }
            check(zip.getEntry(
                "com/bettercontent/bettercontentfixes/mixin/parcool/DodgeMixin.class") != null) {
                "Runtime JAR is missing the ParCool native double-tap suppression mixin: $runtimeJar"
            }

            val mixinConfig = zip.getEntry("better_content_fixes.mixins.json")
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
                "com/bettercontent/bettercontentfixes/compat/BetterCavesCarvingBounds.class")
            check(bounds.contains("m_151570_")) {
                "Runtime Better Caves bounds guard does not use the reobfuscated build-height predicate: $runtimeJar"
            }

            val mixin = classBytes(
                "com/bettercontent/bettercontentfixes/mixin/bettercaves/AbstractCarverMixin.class")
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
                "com/bettercontent/bettercontentfixes/compat/LostCitiesC2meDhSerialization.class")
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
                "com/bettercontent/bettercontentfixes/config/BcFixesConfig.class")
            check(config.contains("LostCitiesC2meDhSerialization")
                    && config.contains("dependenciesAvailable")) {
                "Runtime Lost Cities config gate is not linked to the corrected dependency policy: $runtimeJar"
            }

            val chunkGenerator = classBytes(
                "com/bettercontent/bettercontentfixes/mixin/lostcities/ChunkGeneratorMixin.class")
            val lostCityFeature = classBytes(
                "com/bettercontent/bettercontentfixes/mixin/lostcities/LostCityFeatureMixin.class")
            val sectionBounds = classBytes(
                "com/bettercontent/bettercontentfixes/compat/LostCitiesSectionBounds.class")
            val chunkDriver = classBytes(
                "com/bettercontent/bettercontentfixes/mixin/lostcities/ChunkDriverMixin.class")
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

            val mixinConfig = zip.getEntry("better_content_fixes.mixins.json")
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
                "com/bettercontent/bettercontentfixes/compat/FalloutStructurePlacementBounds.class")
            check(bounds.contains("WRITABLE_ENVELOPE_BLOCKS")
                    && bounds.contains("EDGE_UPDATE_MARGIN_BLOCKS")
                    && bounds.contains("PLACEABLE_TEMPLATE_BLOCKS")
                    && bounds.contains("fit")
                    && bounds.contains("m_71056_")
                    && bounds.contains("m_71058_")) {
                "Runtime Fallout bounds helper lacks its 48-block envelope and edge-update margin contract: $runtimeJar"
            }

            val mixin = classBytes(
                "com/bettercontent/bettercontentfixes/mixin/falloutwastelands/StructureFeatureMixin.class")
            check(mixin.contains("net.mcreator.falloutwastelands.world.features.StructureFeature")
                    && mixin.contains("m_142674_")
                    && mixin.contains("m_230328_")
                    && mixin.contains("FalloutStructurePlacementBounds")) {
                "Runtime Fallout mixin lacks its exact reobfuscated target or placement redirect: $runtimeJar"
            }

            val mixinConfig = zip.getEntry("better_content_fixes.mixins.json")
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
                "com/bettercontent/bettercontentfixes/compat/TwilightForestMazeSerialization.class")
            check(helper.contains("twilightforest")
                    && helper.contains("c2me")
                    && helper.contains("runSerialized")) {
                "Runtime Twilight Forest helper lacks its exact dependency and monitor contract: $runtimeJar"
            }

            val mixin = classBytes(
                "com/bettercontent/bettercontentfixes/mixin/twilightforest/TFMazeMixin.class")
            check(mixin.contains("twilightforest.world.components.structures.TFMaze")
                    && mixin.contains("copyToStructure")
                    && mixin.contains("runSerialized")
                    && mixin.contains("rand")) {
                "Runtime Twilight Forest mixin lacks its exact maze placement/random-source wrapper: $runtimeJar"
            }

            val strongholdMixin = classBytes(
                "com/bettercontent/bettercontentfixes/mixin/twilightforest/ConquerableStructureMixin.class")
            check(strongholdMixin.contains("twilightforest.world.components.structures.util.ConquerableStructure")
                    && strongholdMixin.contains("KnightStrongholdStructure")
                    && strongholdMixin.contains("generateCustom")
                    && strongholdMixin.contains("callStrongholdSerialized")) {
                "Runtime Twilight Forest mixin lacks full Knight Stronghold generation serialization: $runtimeJar"
            }

            val mixinConfig = zip.getEntry("better_content_fixes.mixins.json")
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
                "com/bettercontent/bettercontentfixes/mixin/minecraft/MonsterRoomFeatureMixin.class")
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

            val config = zip.getEntry("better_content_fixes.mixins.json")
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
                "com/bettercontent/bettercontentfixes/mixin/minecraft/CreativeModeInventoryScreenMixin.class")
                ?: throw GradleException("Runtime JAR is missing CreativeModeInventoryScreenMixin: $runtimeJar")
            val bytecode = zip.getInputStream(entry).use { it.readBytes() }.toString(Charsets.ISO_8859_1)
            check(bytecode.contains("init")
                    && bytecode.contains("m_7856_")
                    && bytecode.contains("CreativeModeTabRegistry")
                    && bytecode.contains("getSortedCreativeModeTabs")
                    && !bytecode.contains("CreativeModeTabs;tabs")) {
                "Runtime Creative tab redirect lacks its dual mapped selectors or Forge page source: $runtimeJar"
            }
            val config = zip.getEntry("better_content_fixes.mixins.json")
                ?: throw GradleException("Runtime JAR is missing its mixin configuration: $runtimeJar")
            val configText = zip.getInputStream(config).use { it.readBytes() }.toString(Charsets.UTF_8)
            check(!configText.contains("\"refmap\"")) {
                "Runtime mixin configuration declares an unpackaged refmap: $runtimeJar"
            }
        }
    }
}

tasks.named("verifyFast") {
    dependsOn(verifyRuntimeDispenserAlias)
    dependsOn(verifyRuntimeSprintBridge)
    dependsOn(verifyRuntimeBetterCavesBounds)
    dependsOn(verifyRuntimeLostCitiesSerialization)
    dependsOn(verifyRuntimeFalloutStructureBounds)
    dependsOn(verifyRuntimeTwilightForestMazeSerialization)
    dependsOn(verifyRuntimeMonsterRoomSpawnerRecovery)
    dependsOn(verifyRuntimeCreativeSearchTabMixin)
}

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
