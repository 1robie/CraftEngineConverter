package fr.robie.craftengineconverter.converter.bedrock;

import java.io.File;

/**
 * Runs one conversion against a plugin data folder, with no server.
 * <p>
 * Reached through {@code ./gradlew devConvert}. This is the job the end-to-end tests used to do — drive the whole
 * pipeline over a real pack so the output can be inspected — but as a task rather than a test, so {@code build}
 * stays a fast pure-unit suite and the conversion is still one command away.
 * <p>
 * It lives in the <b>test</b> source set on purpose: it needs the same classpath the tests get, since {@code
 * paper-api} is a {@code compileOnly} dependency of the main source set and only the test runtime puts it back.
 * <p>
 * <b>Partial for items, and deliberately so.</b> There is no running server here, so no Bukkit material registry
 * and no CraftEngine, and the item pipeline leans on both. Measured on the sample pack this emits 23 item
 * geometries where a real server emits far more, and no rendered icons at all. Blocks, blockstates, textures,
 * fonts, sounds and the geometry conversion itself are unaffected, which is what makes this useful for the work it
 * is aimed at — a mapper or geometry change, checked in a second without booting Paper.
 * <p>
 * For anything item-shaped, {@code /cec bedrock} on the dev server is the authoritative run. The old end-to-end
 * tests were not a substitute either: they shared one JVM with the rest of the suite and only saw a full conversion
 * because another test had already initialised {@link fr.robie.craftengineconverter.api.configuration.Configuration},
 * so their output depended on test ordering.
 * <p>
 * Nothing here is shipped in the plugin jar.
 */
public final class DevConvert {

    private DevConvert() {
        throw new UnsupportedOperationException("DevConvert is an entry point and cannot be instantiated.");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("usage: DevConvert <plugin-data-folder>");
            System.exit(2);
            return;
        }

        File pluginFolder = new File(args[0]);
        if (!pluginFolder.isDirectory()) {
            System.err.println("Not a directory: " + pluginFolder.getAbsolutePath()
                    + " - run ./gradlew installDevPack first");
            System.exit(1);
            return;
        }

        System.out.println("Converting " + pluginFolder.getAbsolutePath());
        long started = System.currentTimeMillis();

        // The model / tint / select / range_dispatch loader registries, exactly as the plugin's onEnable does it.
        // Without this every ModelConfigurationRegistry.load(...) returns null, so an item whose appearance is
        // described only by a model tree — every armour piece, because its shape comes from a trim `select` in
        // assets/<ns>/items/<name>.json — falls through BedrockItemLoader.load() to its `return null` and is
        // dropped without a word. Armour, its worn texture, its attachable and its trim variants then simply do
        // not exist in the output, which reads as "the converter does not support armour" rather than
        // "this entry point forgot to load its registries".
        new fr.robie.craftengineconverter.RegistryHelper(DevConvert.class.getClassLoader()).loadRegistries();

        BedrockConverter converter = new BedrockConverter(pluginFolder);
        File config = new File(pluginFolder, "config.yml");
        if (config.isFile()) {
            fr.robie.yamllibrary.file.YamlConfiguration yaml =
                    fr.robie.yamllibrary.file.YamlConfiguration.loadConfiguration(config);

            // The global Configuration singleton, exactly as the plugin's onEnable does it. Without this every
            // Configuration.get(...) falls back to its enum default, which is not the same thing — and it is
            // load-bearing: with the singleton uninitialised this conversion emitted 23 items instead of 134.
            // The deleted end-to-end tests only ever saw the full output because another test in the same JVM had
            // initialised it first, which is a test-ordering dependency worth being rid of.
            fr.robie.craftengineconverter.api.configuration.Configuration.getInstance().load(yaml, config);

            // Folder overrides (bedrock.items-folder and friends), which are separate from the global config.
            converter.loadSettingsFromConfig(yaml);
        }

        // Said out loud because it silently changes the whole output: without a vanilla assets tree a missing
        // parent can only be rebuilt from its name, so anything that is not a cube or a cross — a fence gate, an
        // anvil, a button — falls back to a solid block. That looks exactly like "the texture did not convert".
        fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets assets =
                fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssetStore.existing(pluginFolder);
        Object configuredPath = fr.robie.craftengineconverter.api.configuration.Configuration
                .get(fr.robie.craftengineconverter.api.configuration.ConfigurationKey.VANILLA_ASSETS_PATH);
        System.out.println(assets.isAvailable()
                ? "Vanilla assets: " + assets.source()
                : "Vanilla assets: NONE - non-cube vanilla parents will fall back to a full block."
                        + " vanilla-assets.path resolved to [" + configuredPath + "]");

        converter.convert();

        System.out.println("Done in " + (System.currentTimeMillis() - started) + " ms");
        System.out.println("Output: " + new File(pluginFolder, "bedrock-converted/Geyser-Spigot").getAbsolutePath());
    }
}
