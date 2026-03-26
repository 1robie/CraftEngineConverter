# CraftEngineConverter

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21+-green.svg)](https://www.minecraft.net/)
[![Folia](https://img.shields.io/badge/Folia-✓-blue.svg)](https://papermc.io/software/folia)
[![License](https://img.shields.io/badge/License-GPL--3.0-red.svg)](LICENSE)

A powerful Minecraft plugin that converts configurations, items, and resources from other custom item plugins
to [CraftEngine](https://modrinth.com/plugin/craftengine) format.

## 📋 Table of Contents

- [Features](#-features)
- [Supported Plugins](#-supported-plugins)
- [Requirements](#-requirements)
- [Installation](#-installation)
- [Usage](#-usage)
- [Configuration](#-configuration)
- [Tag System](#-tag-system)
- [API Usage](#-api-usage)
- [Building](#-building)
- [Contributing](#-contributing)
- [License](#-license)

## ✨ Features

### Core Features

- **Multi-threaded Conversion**: Asynchronous conversion system for optimal performance
- **Folia Support**: Full compatibility with Paper's Folia multi-threaded server software
- **Modular Architecture**: Plugin hook system for easy extensibility
- **Resource Pack Management**: Automatic pack mapping and asset conversion

### Conversion Capabilities

- [x] **Items**: Custom items with models, textures, and properties
- [x] **Glyphs**: Font glyphs and custom character conversion
- [x] **Emojis**: Custom emoji conversion with font mapping
- [x] **Images**: Bitmap image conversion with proper font registration
- [x] **Languages**: Multi-language translation file conversion
- [x] **Sounds**: Custom sound definitions and jukebox songs
- [x] **Resource Packs**: Automatic texture and asset migration
- [x] **Equipment**: Armor layers and custom equipment textures (Component && Trim)

### Advanced Features

- **Path Blacklisting**: Exclude specific files or folders from conversion
- **Tag Processing**: Custom tag system for text formatting (glyphs, PlaceholderAPI)
- **Template System**: Pre-configured templates for common model types
- **Smart Namespacing**: Automatic namespace handling and conflict resolution
- **Debug Mode**: Detailed logging for troubleshooting

## 🔌 Supported Plugins

| Plugin         | Items | Glyphs | Emojis | Sounds | Packs | Recipes | Languages |
|----------------|-------|--------|--------|--------|-------|---------|-----------|
| **Nexo**       | ✅     | ✅      | ✅      | ✅      | ✅     | ✅       | ✅         |
| **ItemsAdder** | ✅     | ✅      | ✅      | ✅      | ✅     | ✅       | ✅         |
| **Oraxen**     | 🚧    | 🚧     | 🚧     | 🚧     | 🚧    | 🚧      | 🚧        |

✅ Fully Supported | 🚧 Work in Progress | ❌ Not Supported

## Translation Support

- English (default)
- French (fr)
- Deutsch (de)

## 📦 Requirements

### Server Requirements

- **Minecraft Version**: 1.21 or higher
- **Server Software**: Paper, Purpur, or Folia
- **Java Version**: 21 or higher

### Plugin Dependencies

- **CraftEngine** (required)
- **PacketEvents** (optional) - For advanced packet formatting
- **PlaceholderAPI** (optional) - For placeholder support in messages

## 🚀 Installation

1. **Download** the latest release from [Releases](https://github.com/1robie/CraftEngineConverter/releases)
2. **Place** the `.jar` file in your server's `plugins` folder
3. **Install** CraftEngine plugin if not already installed
4. **Restart** your server
5. **Configure** the plugin (see [Configuration](#-configuration))

## 📖 Usage

### Commands

```
/craftengineconverter convert [plugin] [type]
```

Convert items from another plugin to CraftEngine format.

**Arguments:**

- `plugin` - The source plugin name (e.g., `nexo`)
- `type` - The conversion type: `items`, `glyphs`, `emojis`, `images`, `languages`, `sounds`, `pack`, `recipes`, or
  `all`

**Examples:**

```
/cec convert nexo all          # Convert everything from Nexo
/cec convert nexo items        # Convert only items
/cec convert nexo pack         # Convert resource pack assets
```

---

```
/craftengineconverter reload
```

Reload the plugin configuration and messages.

### Permissions

- `craftengineconverter.command` - Access to all commands
- `craftengineconverter.command.convert` - Use the convert command
- `craftengineconverter.command.reload` - Use the reload command

## ⚙️ Configuration

### Main Configuration (`config.yml`)

```yaml
# Enable debug logging for troubleshooting
enable-debug: false

# Automatically convert items when the plugin enables
auto-convert-on-startup: false

# Default material for converted items
default-material: "PAPER"

# Add <!i> tag to disable italic formatting in item lore
disable-default-italic: true

# Blacklist specific paths from resource pack conversion
blacklisted-paths:
  - "shaders/*"                     # Blacklist all shader files
  - "minecraft:textures/shaders/*"  # Namespace-specific blacklist

# Enable/disable formatting for different message types
formatting:
  packet-events: true  # PacketEvents integration
  boss-bar: true       # Boss bar messages
  action-bar: true     # Action bar messages
  plugin-message: true # Plugin channel messages
  title: true          # Title/subtitle messages

# Tag system configuration
tag:
  glyph:
    enabled: true      # Enable glyph tag processing
  placeholderapi:
    enabled: true      # Enable PlaceholderAPI tag processing
```

### Path Blacklisting

The blacklist system supports:

- **Wildcards**: `shaders/*` matches all files in the shaders folder
- **Specific files**: `shaders/rendertype_text.fsh`
- **Namespaced paths**: `minecraft:textures/shaders/*`
- **Root-level matching**: `*/config.json` matches config.json in any folder

## 🏷️ Tag System

CraftEngineConverter includes a powerful tag processing system for text formatting.

### Glyph Tags

Convert custom glyphs to their CraftEngine font equivalents:

```yaml
# Nexo format
display_name: "<glyph:custom_icon>My Item"

# After conversion (CraftEngine format)
display_name: "<font:craftengine:default>⚔</font>My Item"
```

**Escaped Tags**: Use `\<glyph:...>` to display the tag literally without conversion.

### PlaceholderAPI Tags

Integrate PlaceholderAPI placeholders in messages:

```yaml
# Both formats supported
message: "<placeholderapi:player_name> joined!"
message: "<papi:player_name> joined!"
```

**Escaped Tags**: Use `\<papi:...>` to display the tag literally.

## 🔧 API Usage

### Adding CraftEngineConverter as a Dependency

No available for the moment.

**Maven:**

```xml

<dependency>
    <groupId>fr.robie.craftengineconverter</groupId>
    <artifactId>API</artifactId>
    <version>1.0</version>
    <scope>provided</scope>
</dependency>
```

**Gradle:**

```gradle
dependencies {
    compileOnly 'fr.robie.craftengineconverter:API:1.0'
}
```

### Creating a Custom Converter

```java
public class MyPluginConverter extends Converter {

    public MyPluginConverter(CraftEngineConverter plugin) {
        super(plugin, "MyPlugin");
    }

    @Override
    public CompletableFuture<Void> convertItems(boolean async) {
        return executeTask(async, () -> {
            // Your conversion logic here
        });
    }

    // Implement other conversion methods...
}
```

### Using the Tag System

You can register custom tags to be processed in any text (packets, world conversion, etc.).

**Registering a custom tag processor:**

```java
// Get the ITagResolver from Bukkit Services Manager
RegisteredServiceProvider<ITagResolver> rsp = Bukkit.getServicesManager().getRegistration(ITagResolver.class);
if(rsp !=null){
ITagResolver tagResolver = rsp.getProvider();

// Register your custom processor
    tagResolver.

registerTagProcessor(new TagProcessor() {
    @Override
    public String getTagName () {
        return "My Custom Tag";
    }

    @Override
    public Pattern getPattern () {
        return Pattern.compile("<my_tag:([^>]+)>");
    }

    @Override
    public boolean hasTag (String input){
        return input.contains("<my_tag:");
    }

    @Override
    public Optional<String> process (String input, Player player){
        // Replace <my_tag:hello> with "Hi!"
        return Optional.of(input.replace("<my_tag:hello>", "Hi!"));
    }
});
        }
```

**Resolving tags in a string:**

```java
ITagResolver tagResolver = ...; // Get from services
Optional<String> resolved = tagResolver.resolveTags("<glyph:heart> Hello!", player);
String output = this.resolved.orElse("<glyph:heart> Hello!");
```

### Using SnakeUtils for YAML Manipulation

```java
// Create a SnakeUtils instance
SnakeUtils utils = new SnakeUtils(yamlFile);

// Get and set values with path notation
utils.

setValue("items.my_item.display_name","My Item");

String name = this.utils.getString("items.my_item.display_name");

// Work with sections
SnakeUtils section = this.utils.getSection("items.my_item");
section.

setValue("material","DIAMOND_SWORD");

// Get typed values
int amount = this.utils.getInt("items.my_item.amount", 1);
List<String> lore = this.utils.getStringList("items.my_item.lore");
Map<String, Object> data = this.utils.getMap("items.my_item");

// Save changes
utils.

save();
```

## 🏗️ Building

### Prerequisites

- JDK 21 or higher
- Maven 3.6+
- Git

### Build Steps

```bash
# Clone the repository
git clone https://github.com/1robie/CraftEngineConverter.git
cd CraftEngineConverter

# Build with Maven
mvn clean package

# The compiled JAR will be in Plugin/target/
```

### Project Structure

```
CraftEngineConverter/
├── API/              # Public API for other plugins
├── Common/           # Shared utilities and core logic
├── Hooks/            # Plugin integration modules
│   ├── BOM/          # Bill of Materials
│   ├── PacketEvents/ # PacketEvents integration
│   └── PlaceholderAPI/ # PlaceholderAPI integration
└── Plugin/           # Main plugin implementation
```

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/AmazingFeature`)
3. **Commit** your changes (`git commit -m 'Add some AmazingFeature'`)
4. **Push** to the branch (`git push origin feature/AmazingFeature`)
5. **Open** a Pull Request

### Code Style

- Follow standard Java conventions
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Maintain consistent formatting (4 spaces indentation)

## 📝 License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **CraftEngine** - The target format plugin
- **Nexo** - For inspiring this conversion tool
- **Paper/Folia** - For the excellent server software
- **SnakeYAML** - For YAML processing capabilities

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/1robie/CraftEngineConverter/issues)
- **Discord**: [Join our community](https://discord.gg/your-invite)
- **Documentation**: [Wiki](https://1robie.gitbook.io/craftengineconverter)

---

Made with ❤️ by [1robie](https://github.com/1robie)
