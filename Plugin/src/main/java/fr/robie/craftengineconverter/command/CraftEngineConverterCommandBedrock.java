package fr.robie.craftengineconverter.command;

import fr.robie.craftengineconverter.CraftEngineConverter;
import fr.robie.craftengineconverter.api.builder.TimerBuilder;
import fr.robie.craftengineconverter.common.permission.Permission;
import fr.robie.craftengineconverter.converter.bedrock.BedrockConverter;
import fr.robie.messageflow.logger.Logger;
import fr.robie.paperdispatch.command.CommandDispatch;
import fr.robie.paperdispatch.command.CommandResultType;
import fr.robie.paperdispatch.command.SubCommand;
import fr.robie.yamllibrary.file.YamlConfiguration;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class CraftEngineConverterCommandBedrock extends SubCommand<CraftEngineConverter> {

    public CraftEngineConverterCommandBedrock(CraftEngineConverter plugin) {
        super(plugin, "bedrock", "be");
        this.setPermission(Permission.COMMAND_BEDROCK_CONVERT.asPermission());
    }

    @Override
    protected @NotNull CommandResultType perform(@NotNull CommandDispatch<CraftEngineConverter> commandDispatch) {
        CommandSender sender = commandDispatch.getSender();
        try {
            long startTime = System.currentTimeMillis();
            BedrockConverter converter = new BedrockConverter(this.plugin.getDataFolder());

            File configFile = new File(this.plugin.getDataFolder(), "config.yml");
            if (configFile.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                converter.loadSettingsFromConfig(config);
            }

            converter.convert();
            long endTime = System.currentTimeMillis();
            sender.sendMessage("§aBedrock conversion completed in " + TimerBuilder.formatTimeAuto(endTime - startTime));
            return CommandResultType.SUCCESS;
        } catch (Exception e) {
            Logger.error("An error occurred during Bedrock conversion.", e);
            sender.sendMessage("§cAn error occurred during Bedrock conversion. Check console for details.");
            return CommandResultType.FAILURE;
        }
    }
}
