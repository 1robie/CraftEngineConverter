# Development guide

The build is Gradle (Kotlin DSL) with a version catalog and convention plugins. Everything below
assumes the wrapper, so no Gradle installation is required.

```
CraftEngineConverter/
├─ settings.gradle.kts          module list, plugin repositories, toolchain auto-provisioning
├─ build.gradle.kts             root (intentionally almost empty)
├─ gradle.properties            group/version, plugin metadata, runServer knobs
├─ gradle/libs.versions.toml    every dependency and plugin version
├─ gradle/gradle-daemon-jvm.properties   pins the Gradle daemon to Java 21
├─ build-logic/                 convention plugins (included build)
│  └─ src/main/kotlin/
│     ├─ catalog.kt                              version-catalog access helper
│     ├─ craftengineconverter.java-conventions.gradle.kts   toolchain, encoding, tests, REPOSITORIES
│     └─ craftengineconverter.hook-conventions.gradle.kts   shared deps for Hooks/*
├─ API/  Common/  Hooks/{BOM,PacketEvent,PlaceholderAPI,Nexo,ItemsAdder}/  Plugin/
```

## Everyday commands

| Command | What it does |
| --- | --- |
| `./gradlew build` | Compiles everything, runs the tests, produces `Plugin/build/libs/CraftEngineConverter-<version>.jar` |
| `./gradlew :Plugin:shadowJar` | Just the shaded plugin jar |
| `./gradlew runServer` | Downloads Paper, creates `run/`, installs the plugin and the sample pack, starts the server |
| `./gradlew installDevPack` | Just the sample pack + a dev `config.yml`, into `run/plugins/CraftEngineConverter/` |
| `./gradlew devConvert` | Runs a conversion against that folder with **no server** (see caveat below) |
| `./gradlew test` | Tests only |
| `./gradlew build -PdevBuild` | Same as the old `-P dev-build` Maven profile: appends `-Dev` to the jar version |
| `./gradlew build -PdevSuffix=-RC1` | Arbitrary version suffix |
| `./gradlew cleanPaperCache` | Drops run-paper's cached Paper jars |

## The development server

`./gradlew runServer` needs nothing installed:

1. run-paper resolves the latest Paper build for the Minecraft version of the paperweight dev
   bundle, so the test server can never drift away from the API the plugin compiles against.
2. It creates `run/` (git-ignored) and starts the server with `--nogui`.
3. The freshly built shaded jar is handed to Paper via `-add-plugin=...`, so nothing is copied into
   a plugins directory and there is no stale-jar problem.
4. `-Ddisable.watchdog=true` is set for you, so sitting on a breakpoint does not kill the server.

### The sample pack

`runServer` depends on `installDevPack`, which lays out what `BedrockConverter` expects under the plugin's data
folder — every path it uses is derived from that folder:

```
run/plugins/CraftEngineConverter/
├─ config.yml                 written once; never overwritten, so tuning survives
├─ bedrock/items/             CraftEngine's configuration trees (the YAML)
├─ bedrock/pack/assets/       the Java resource pack input
└─ bedrock-converted/Geyser-Spigot/    output, wiped and rewritten each run
```

The pack goes in as `bedrock/pack/assets`, not as the extracted `resource_pack_unprotected/` folder:
`convertPackDirectory` only descends into a child directory literally named `assets`, or into a zip. Dropping the
extracted folder in produces a conversion with **no assets at all** and no error.

The dev `config.yml` sets two things that are easy to get wrong:

- `vanilla-assets.path` → `Minecraft-default-assets-latest/` in the repo root, if present. Without it a missing
  vanilla parent can only be rebuilt from its *name*, which recovers a cube and the common poses but not real UVs,
  face rotations or display transforms. It is the difference between an anvil and a flat billboard.
- `default-material: "auto"`. The shipped default is `PAPER`, which being a real material wins outright over
  CraftEngine's own `item.default-material` and changes every materialless item's id and creative category.

Then, in game: `/cec bedrock`, and read `run/plugins/CraftEngineConverter/bedrock-converted/`.

### `devConvert`, and what it cannot tell you

`./gradlew devConvert` runs the same conversion headless, in about 1.5 s, writing into the same folder. It is the
fast loop for mapper and geometry work.

It is **partial for items**: there is no Bukkit material registry and no CraftEngine, and the item pipeline needs
both. On the sample pack it emits 23 item geometries and no rendered icons, where a real server emits far more.
Blocks, blockstates, textures, fonts, sounds and the geometry conversion itself are all exercised normally. For
anything item-shaped, `/cec bedrock` is the authoritative run.

`-PconvertDir=<path>` points it at a different data folder.

Two things are still manual, once:

- **EULA.** The first run stops and writes `run/eula.txt`. Read it and set `eula=true` yourself -
  the build will not accept Mojang's licence on your behalf.
- **CraftEngine.** It is a hard dependency (`required: true` in `paper-plugin.yml`) and is not
  publicly downloadable, so drop its jar into `run/plugins/` once. Optional soft dependencies
  (PlaceholderAPI, packetevents) can be fetched automatically - see the commented `downloadPlugins`
  block at the bottom of [Plugin/build.gradle.kts](Plugin/build.gradle.kts).

### JVM arguments

All configured from `gradle.properties`, or per invocation:

```bash
./gradlew runServer -PrunServer.jvmArgs="-Xms4G -Xmx4G -XX:+UseZGC"
```

| Property | Purpose |
| --- | --- |
| `runServer.jvmArgs` | Full JVM argument list for the dev server |
| `runServer.javaAgent` | Path to a `-javaagent:` jar (HotswapAgent, profiler, ...) |
| `runServer.javaVersion` | Run the server on a different JDK than the compile toolchain |
| `runServer.javaVendor` | Toolchain vendor filter, e.g. `JETBRAINS` |

## Debugging in IntelliJ IDEA

1. Open the project directory. IntelliJ detects `settings.gradle.kts` and imports the Gradle build,
   including `build-logic` as a separate module.
2. In the Gradle tool window, open **CraftEngineConverter → Tasks → run paper → runServer**,
   right-click and choose **Debug 'runServer'**. IntelliJ attaches the debugger to the forked server
   JVM automatically - there is no remote-debug configuration or `-agentlib` flag to set up.
3. In **Settings → Build, Execution, Deployment → Compiler**, enable **Build project automatically**.

The intended loop:

```
Debug runServer  →  server starts  →  edit Java  →  Ctrl+F9 (Build)  →  changes are live
```

`Ctrl+F9` recompiles into `Plugin/build/classes`, and IntelliJ hot-swaps the changed classes into
the running server.

### Toolchains and the daemon JVM

`gradle/gradle-daemon-jvm.properties` pins the **Gradle daemon itself** to Java 21, and Gradle
downloads that JDK if it is missing. This makes the daemon JVM independent of IntelliJ's
"Gradle JVM" setting, so it does not matter what that is set to.

That file is not cosmetic. Without it the daemon runs on whatever JVM launched it, and on a JDK
newer than the embedded Kotlin compiler supports (JDK 26 at the time of writing) Kotlin silently
falls back to an older JVM target and warns `Inconsistent JVM-target compatibility ... 'compileJava'
(26) and 'compileKotlin' (25)`. Regenerate it with:

```bash
./gradlew updateDaemonJvm --jvm-version=21
```

The build then compiles with Java 21 (`java = "21"` in `libs.versions.toml`), independently of the
JDK running Gradle. If no JDK 21 is installed, the foojay resolver applied in `settings.gradle.kts`
downloads one - that path is verified: on a machine with only JDK 25, the first build provisioned
Temurin 21 and `runServer` reported `Running Java 21 (... Temurin-21.0.8+9)`. Point IntelliJ at the
Gradle JVM of your choice; it does not have to be 21.

## HotSwap

### Level 1 - method bodies, nothing to install

Start the server with **Debug 'runServer'**, not Run. Edit a method body, press `Ctrl+F9`, and the
JVM's standard HotSwap replaces the code in the running server. This is plain JDK functionality and
needs no extra JVM, no agent and no configuration.

Its limit: **method bodies only**. Adding or removing a method or field, or changing a class
hierarchy, fails with "operation not supported by VM".

### Level 2 - structural changes, still nothing to install

IntelliJ bundles a JetBrains Runtime, and JBR has DCEVM built in. It sits outside the directories
Gradle scans, so `gradle.properties` registers it via `org.gradle.java.installations.paths`
(adjust the path if your IDE lives elsewhere). Then:

```bash
./gradlew runServer -PrunServer.javaVersion=25 -PrunServer.javaVendor=JetBrains \
  "-PrunServer.jvmArgs=-Xms2G -Xmx2G -XX:+AllowEnhancedClassRedefinition"
```

or make it permanent in `gradle.properties`:

```properties
runServer.javaVersion = 25
runServer.javaVendor  = JetBrains
runServer.jvmArgs     = -Xms2G -Xmx2G -Dfile.encoding=UTF-8 -XX:+AllowEnhancedClassRedefinition
```

Verified on this machine: the server reports
`Running Java 25 (... JetBrains s.r.o. JBR-25.0.3+1-329.124-jcef)` and the plugin initializes. Now
`Ctrl+F9` also handles added/removed methods and fields.

The dev server runs on 25 while the plugin still **compiles** against Java 21 - the toolchain is
unchanged, and Java 21 bytecode runs fine on a 25 JVM.

`-XX:+AllowEnhancedClassRedefinition` is **only valid on a JBR/DCEVM JVM**; a stock JDK refuses to
start with it. That is why it is not in the default `runServer.jvmArgs`.

### Level 3 - HotswapAgent (optional, rarely worth it here)

HotswapAgent adds *framework* reloading - Spring beans, Hibernate mappings, resource files. A Paper
plugin uses none of that, and DCEVM already covers the structural changes. Get `hotswap-agent.jar`
from <https://github.com/HotswapProjects/HotswapAgent/releases> (the HotSwapHelper IntelliJ plugin
also downloads one, to `~/.hotswap/hotswap-agent.jar`) and set:

```properties
runServer.javaVersion   = 25
runServer.javaVendor    = JetBrains
runServer.javaAgent     = C:/Users/you/.hotswap/hotswap-agent.jar
runServer.javaAgentArgs = disablePlugin=Log4j2
runServer.jvmArgs       = -Xms2G -Xmx2G -XX:+AllowEnhancedClassRedefinition -XX:HotswapAgent=external
```

`runServer.javaAgent` contributes the `-javaagent:` flag and `runServer.javaAgentArgs` is appended
after it as `-javaagent:<jar>=<args>`; `-XX:HotswapAgent=external` stays in `runServer.jvmArgs`
because it too is JBR-only.

**`disablePlugin=Log4j2` is not optional.** HotswapAgent's Log4j2 plugin tries to instrument
`org.apache.logging.log4j.core.LoggerContext`, but Paper loads log4j2 in a separate `URLClassLoader`
that cannot see `org.hotswap.agent.config.PluginManager`, so every startup prints
`CannotCompileException: no such class` stack traces. The server still boots, but the noise is
constant. Note the key is `disablePlugin` (singular) - `disabledPlugins` is the
`hotswap-agent.properties` key and is ignored as an agent argument.

Verified end to end: agent loads (`Loading Hotswap agent {2.0.2} - unlimited runtime class
redefinition`), zero HotswapAgent errors, plugin initializes.

### About the HotSwapHelper IntelliJ plugin

HotSwapHelper works by cloning a **run configuration** and relaunching it on a DCEVM JVM with the
agent attached. `runServer` is a Gradle task whose server JVM is forked by the Gradle daemon, not a
process IntelliJ launches, so HotSwapHelper cannot inject into it. That is fine - this build does
the same job through `runServer.javaVersion` / `javaVendor` / `javaAgent` / `jvmArgs`. The plugin is
still useful for the `hotswap-agent.jar` it downloads.

### What HotSwap can never do

Redefinition swaps bytecode. It does not re-run anything or rebuild existing objects, so a restart
is still required for:

- `onEnable` / `onDisable` / bootstrapper / loader changes - they already ran
- registering or unregistering event listeners, commands or bStats charts
- `paper-plugin.yml` changes (dependencies, api-version, main class)
- new fields on objects that already exist - they get default values, not your initialiser
- anything already cached or held by CraftEngine through `join-classpath: true`

### Where it pays off in this project

The converter code (`converter/**`, mappers, geometry, textures) is pure logic triggered by a
command, which is the ideal shape for HotSwap: edit a mapper, `Ctrl+F9`, re-run the convert command.

For that same code the *fastest* loop is often no server at all. The suite is pure unit tests and runs in about a
second:

```bash
./gradlew :Plugin:test --tests '*TransformTest*' --tests '*AttachablePoseTest*'
```

The four tests that used to run a whole conversion are gone; `devConvert` and `/cec bedrock` replaced them. They
were ~9 s of a ~10 s suite, and their output was ordering-dependent — they shared a JVM with the rest of the suite
and only saw a complete conversion because another test had already initialised `Configuration`. The one check
worth keeping, that every `item_texture.json` shortname resolves to a real PNG, now runs at the end of *every*
conversion and logs what is missing.

## Migration notes (Maven → Gradle)

- The ten `pom.xml` files have been deleted; Gradle is the only build system. They remain in git
  history if you ever need to compare - `git show 84ce318:pom.xml` for the old root POM.
  If IntelliJ still shows the project as Maven, unlink it in the Maven tool window and re-import
  from `settings.gradle.kts`.
- Maven's `provided` scope maps to Gradle's `compileOnly`, but Maven also puts `provided`
  dependencies on the test classpath. `craftengineconverter.java-conventions` restores that with
  `testImplementation.extendsFrom(compileOnly)`.
- Resource filtering is applied only to `paper-plugin.yml` and `translations/**/messages.yml` - the
  only resources that ever contained `${...}` tokens. The resource files themselves are unchanged.
- Repositories live in `craftengineconverter.java-conventions`, not in `settings.gradle.kts`.
  paperweight-userdev registers extra repositories on `:Plugin` (mache, the plugin remapper, and
  whichever decompiler/param-mapping repos the dev bundle's metadata names). Gradle never *merges*
  settings repositories with project repositories - one side always wins - so a settings-level block
  would break paperweight's setup pipeline. It is still a single declaration site.
- `fr.robie:yamllibrary:1.0-SNAPSHOT` is published to no remote repository; it is `mvn install`ed
  into `~/.m2` by hand. Maven resolved it implicitly, Gradle cannot, so the convention plugin has a
  `mavenLocal()` entry **restricted to that one module**. Publishing yamllibrary somewhere real is
  the only thing standing between this build and a clean-machine CI run - see
  `craftengineconverter.java-conventions.gradle.kts`.
- "Deprecated Gradle features were used in this build" comes from paperweight-userdev itself
  (`val reobfJar by tasks.registering`), not from these scripts. Verify with
  `./gradlew help --warning-mode all`.

## Known upstream defect: JitPack parent-POM case collision

`MessageFlow` and `Paper-Dispatch` are published to JitPack with **both** the root aggregator and
the child module, whose artifactIds differ only in case:

| module | declared parent |
| --- | --- |
| `com.github.1robie.MessageFlow:message-flow` | `com.github.1robie.MessageFlow:Message-Flow` |
| `com.github.1robie.paper-dispatch:paper-dispatch` | `com.github.1robie.paper-dispatch:Paper-Dispatch` |

On a case-insensitive filesystem (Windows, default macOS) the parent resolves back to the child, so
Gradle walks an infinite parent chain and dies with a bare `java.lang.StackOverflowError` deep in
`GradlePomModuleDescriptorParser`. It is order-dependent: a warm metadata cache hides it, which is
why a CLI build can pass while an IntelliJ sync fails.

The workaround lives in `craftengineconverter.java-conventions.gradle.kts`: the jitpack repository
(already scoped to `com.github.1robie.*`) declares `metadataSources { artifact() }`, so Gradle takes
the jars and never reads those POMs. Nothing is lost - both are shaded fat jars, and the only
dependency either POM declares is `paper-api` at `provided` scope.

**The real fix is upstream.** In the `MessageFlow` and `Paper-Dispatch` repos, rename the root
aggregator's `artifactId` so it no longer collides case-insensitively with its child module (e.g.
`message-flow-parent`). Once republished, delete the `metadataSources` block.

To check whether any dependency has a cyclic parent chain, look for two POMs in the same cache
directory whose names differ only in case:

```bash
find ~/.gradle/caches/modules-2/files-2.1 -name '*.pom' -printf '%f\n' | sort -f | uniq -Di
```
