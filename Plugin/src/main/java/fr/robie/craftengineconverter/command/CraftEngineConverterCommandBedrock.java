package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.converter.bedrock.BedrockConverter;
import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssetStore;
import fr.robie.craftengineconverter.converter.bedrock.asset.VanillaAssets;
import fr.robie.messageflow.logger.Logger;
import fr.robie.paperdispatch.command.CommandDispatch;
import fr.robie.paperdispatch.command.CommandResultType;
import fr.robie.paperdispatch.command.SubCommand;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class CraftEngineConverterCommandBedrock extends SubCommand<CraftEngineConverter> {

    public CraftEngineConverterCommandBedrock(CraftEngineConverter plugin) {
        super(plugin, "bedrock", "be");
        this.setPermission(Permission.COMMAND_BEDROCK_CONVERT.asPermission());
        this.addSubCommand(new CraftEngineConverterCommandBedrockVanillaAssets(plugin));
    }

    @Override
    protected @NotNull CommandResultType perform(@NotNull CommandDispatch<CraftEngineConverter> commandDispatch) {
        CommandSender sender = commandDispatch.getSender();

        CompletableFuture.runAsync(() -> this.convert(sender))
                .exceptionally(error -> {
                    Logger.error("An error occurred during Bedrock conversion.", error);
                    sender.sendMessage("§cAn error occurred during Bedrock conversion. Check console for details.");
                    return null;
                });

        sender.sendMessage("§7Bedrock conversion started...");
        return CommandResultType.SUCCESS;
    }

    private void convert(CommandSender sender) {
        long startTime = System.currentTimeMillis();

        VanillaAssets assets = VanillaAssetStore.prepare(this.plugin.getDataFolder());
        if (assets.isAvailable()) {
            Logger.info("Vanilla assets ready: " + assets.source());
        }

        BedrockConverter converter = new BedrockConverter(this.plugin.getDataFolder());
        this.loadConverterSettings(converter);
        converter.convert();

        long endTime = System.currentTimeMillis();
        sender.sendMessage("§aBedrock conversion completed in "
                + TimerBuilder.formatTimeAuto(endTime - startTime));
    }

    private void loadConverterSettings(BedrockConverter converter) {
        File configFile = new File(this.plugin.getDataFolder(), "config.yml");
        if (configFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            converter.loadSettingsFromConfig(config);
        }
    }
}
