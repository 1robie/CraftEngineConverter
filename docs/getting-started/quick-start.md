# 🚀 Quick Start

This guide will help you get **CraftEngineConverter** up and running in just a few minutes.

## 📋 Prerequisites

Please check the [📋 Requirements](requirements.md) page to ensure your server meets all necessary requirements.

## 📥 Download

Download the latest version of **CraftEngineConverter** from one of the following sources:
- [Modrinth](https://modrinth.com/plugin/craftengineconverter) :x:
- [Polymart](https://polymart.org/product/XXXX/craftengineconverter) :x:
- [SpigotMC](https://www.spigotmc.org/resources/craftengineconverter.XXXX/) :x:
- [GitHub Releases](https://github.com/1robie/CraftEngineConverter/releases) :white_check_mark:

:x: Note: The plugin is currently only available on GitHub Releases. Other platforms will be supported in future updates.

## 🔧 Installation

### Step 1: Install Plugins

1. **Stop your Minecraft server** if it is running.
2. **Download the CraftEngineConverter.jar** file from the link above.
3. **Place the {{space.vars.SINGLE_BACKTICK}}CraftEngineConverter.<code class="expression">space.vars.PLUGIN_VERSION</code>.jar{{space.vars.SINGLE_BACKTICK}} file** into your server's `plugins` directory.
4. **Ensure that the required dependency, CraftEngine**, is also installed in the `plugins` directory.
5. **Start your Minecraft server.**

### Step 2: Verification

After starting your server, check the console logs for messages indicating that **CraftEngineConverter** has been enabled successfully. You should see output similar to the following:
{{space.vars.CODE_BLOCK_TRIPLEBACKTICKS}} bash
[xx:xx:xx INFO]: [CraftEngineConverter v<code class="expression">space.vars.PLUGIN_VERSION</code>] Enabling plugin ...
[xx:xx:xx INFO]: [CraftEngineConverter v<code class="expression">space.vars.PLUGIN_VERSION</code>] Loading 1 commands
[xx:xx:xx INFO]: [CraftEngineConverter v<code class="expression">space.vars.PLUGIN_VERSION</code>] Auto-conversion is enabled, starting conversion...
[xx:xx:xx INFO]: [CraftEngineConverter v<code class="expression">space.vars.PLUGIN_VERSION</code>] Plugin enabled !
{{space.vars.CODE_BLOCK_TRIPLEBACKTICKS}}

If you see any errors, please refer to the [🐛 Common Issues](../troubleshooting/common-issues.md) page for troubleshooting tips.

## 🎉 You're All Set!

You have successfully installed and set up **CraftEngineConverter**! You can now start converting configuration files from supported plugins into the CraftEngine format.
For more information on how to use the plugin, please refer to the [🔧 Main Configuration](../configuration/main-config.md) and [⌨️ Commands](../commands-permissions/commands.md) pages.