# 📝 Commands Reference

This page provides a comprehensive reference for all commands available in **CraftEngineConverter**. Each command is detailed with its syntax, description, and required permissions.

## Main Command

### `/craftengineconverter` or `/cec`
- **Description**: The primary command for accessing CraftEngineConverter functionalities.
- **Permissions**: `craftengineconverter.command.use`

#### Subcommands

1. `reload`
  - **Description**: Reloads the plugin configuration without restarting the server.
  - **Usage**: `/craftengineconverter reload` or `/cec reload`
  - **Permissions**: `craftengineconverter.command.reload`

2. `convert [<plugin_name>] [<type>] [--dryrun] [--threads=<number>]`
  - **Description**: Initiates the conversion process for supported plugin configuration files into the CraftEngine format.
  - **Usage**:
    Available plugin names:
      - `nexo`  
    Available types:
      - `ALL` Default - Converts all supported configuration files.
      - `ITEMS` - Converts only item configurations.
      - `EMOJIS` - Converts only emoji configurations.
      - `IMAGES` - Converts only image configurations.
      - `LANGUAGES` - Converts only language files.
      - `SOUNDS` - Converts only sound configurations.
      - `PACKS` - Converts only resource packs.
    Additional flags:
      - `--dryrun`: Simulates the conversion process without making any changes.
      - `--threads=<number>`: Specifies the number of threads to use for multi-threaded pack conversion. Default is 1.
    - **Usage Example**: `/craftengineconverter convert nexo ITEMS --dryrun --threads=4` or `/cec convert nexo ITEMS --dryrun --threads=4`
    - **Permissions**: `craftengineconverter.command.convert`