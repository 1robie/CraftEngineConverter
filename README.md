# CraftEngineConverter

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Paper](https://img.shields.io/badge/Paper-1.21.11-green.svg)](https://papermc.io/)
[![Folia](https://img.shields.io/badge/Folia-✓-blue.svg)](https://papermc.io/software/folia)
[![Docs](https://img.shields.io/badge/docs-1robie.github.io-185adb.svg)](https://1robie.github.io/plugins/craftengineconverter)
[![License](https://img.shields.io/badge/License-GPL--3.0-red.svg)](LICENSE)

Moves a Paper or Folia server from **Nexo** or **ItemsAdder** to
[**CraftEngine**](https://modrinth.com/plugin/craftengine): it converts items, blocks, furniture, emojis, images,
sounds, recipes, translations and resource packs, then the blocks and furniture already placed in your worlds, with
a record of every change so it can be restored.

## 📖 Documentation

**[1robie.github.io/plugins/craftengineconverter](https://1robie.github.io/plugins/craftengineconverter)**:
requirements, installing and updating, every configuration key, commands, permissions, placeholders, a migration
guide for Nexo and ItemsAdder, troubleshooting and FAQ.

## 📥 Download

- **Releases**: [GitHub releases](https://github.com/1robie/CraftEngineConverter/releases)
- **Dev builds** of the next version, built from `develop` on every push (unstable):
  [the `dev` pre-release](https://github.com/1robie/CraftEngineConverter/releases/tag/dev)

## 🚀 In short

1. Put the jar in `plugins/` next to CraftEngine, and start the server.
2. `/cec convert nexo` or `/cec convert itemsadder`.
3. Copy `plugins/CraftEngineConverter/converted/<Plugin>/CraftEngine/resources/craftengineconverter` into
   `plugins/CraftEngine/resources/<name>/`, then `/ce reload all`.
4. Convert the world: players using old blocks, chunks as they load, or `/cec worldconverter start`.

The [migration guide](https://1robie.github.io/plugins/craftengineconverter/integrations) has the details.

## 🏗️ Building

```bash
git clone https://github.com/1robie/CraftEngineConverter.git
cd CraftEngineConverter
./gradlew build
```

The jar is `Plugin/build/libs/CraftEngineConverter-<version>.jar`. [DEVELOPMENT.md](DEVELOPMENT.md) covers the
development server, debugging and HotSwap.

## 🤝 Contributing

Pull requests target **`develop`**; `master` only takes releases. See [CONTRIBUTING.md](CONTRIBUTING.md).

## 📝 License

GNU General Public License v3.0, see [LICENSE](LICENSE).

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/1robie/CraftEngineConverter/issues)
- **Security**: see [SECURITY.md](SECURITY.md)

---

Made with ❤️ by [1robie](https://github.com/1robie)
