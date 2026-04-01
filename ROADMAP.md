# 🗺️ ROADMAP - CraftEngineConverter

> **Last Updated:** April 1, 2026
> **Project Status:** 🟢 Active Development
> **This file serves as the main project roadmap and development tracker.**

---

## 🚀 Roadmap

### Version 1.0.0 ()

- [x] Complete Nexo conversion (items, glyphs, emojis, images, languages, sounds, equipment, furniture, custom blocks,
  mechanics, new components: attack_charge, use effects, damage type, kinetic, swing animation, minimum attack charge,
  painting variant, piercing)
- [x] Replace Nexo block/ furniture with their CraftEngine equivalents
- [x] Replace ItemsAdder block/ furniture with their CraftEngine equivalents
- [ ] Documentation complete

### Version 1.1.0 (Oraxen Support)

- [ ] Items, blocks, furniture converter
- [ ] Resource pack migration
- [ ] Documentation & examples

---

## 🛡️ Security & Quality

- [x] Fix NoSuchFileException during ZIP extraction
- [x] Zip Slip vulnerability protection (CWE-22)
- [x] URL decoding validation (`..%2F..%2F`)
- [x] Block UNC paths (`\\server\share`)
- [x] Add comprehensive security tests
- [x] Fix all `mkdirs()`/`delete()` ignored warnings
- [x] Add `try-with-resources` for SnakeUtils
- [x] Refactor duplicate code in armor conversion

---

## 🧪 Testing

- [x] Security tests (Zip Traversal)
- [ ] SnakeUtils tests (full coverage)
- [ ] ConfigPath tests
- [ ] Converter tests (each type)
- [ ] Integration: Nexo pipeline, resource pack, multi-threading, Folia compatibility
- [ ] Manual: Real Nexo/Oraxen/ItemsAdder packs, performance benchmarks

---

## 📚 Documentation

- [x] README.md
- [x] CONTRIBUTING.md
- [x] SECURITY_TESTING.md
- [x] [Wiki pages](https://1robie.gitbook.io/craftengineconverter)
- [ ] Migration guides
- [ ] FAQ section
- [ ] API documentation & code examples
- [ ] Tag processor & extension guide

---

## 🎨 Features & Enhancements

- [x] Glyph tag processor
- [x] PlaceholderAPI tag processor
- [x] Custom tag creation API
- [ ] Tag validation and sanitization
- [ ] Partial conversion (select items)
- [x] Dry-run mode
- [ ] Backup/rollback system
- [ ] Conversion profiles (save/load)
- [ ] Better console output (colors, formatting)
- [x] Progress bars for long operations
- [x] Optimize async conversion (thread pools)
- [x] Progress tracking for large conversions
- [x] Cache frequently accessed configs (FileCacheManager)
- [ ] Batch file operations

---

## 🐛 Known Issues

---

## 🔄 DevOps & Community

- [ ] GitHub Actions (CI)
- [ ] Automated PR testing
- [ ] Code quality (SonarQube)
- [ ] Security scanning (Dependabot)
- [ ] Automatic releases
- [ ] Auto-publish: Maven Central, SpigotMC, Modrinth
- [ ] GitHub Discussions, issue/PR templates
- [ ] Publish on SpigotMC, Modrinth

---

## ✅ Recently Completed

- **MessageLoader edition**: Now automatically delete obsolete keys from the file (they create a backup first) +
  improved logging of missing keys inside the file.
- **Reworked Nexo parent model conversion**: Added support for all parent model types.
- **Custom Tag API**: Implementation of a public API to allow other plugins to register their own tag processors.
- **Auto-convert on startup**: Configurable selected types and options for automated migration on plugin enable.
- **Chunk load conversion**: Automated conversion of Nexo and ItemsAdder blocks/furniture when chunks are loaded.
- **Loot table support**: Comprehensive loot table configuration with conditions, functions, and support for Nexo
  furniture drops.
- **ItemsAdder Block States**: Added support for Mushroom blocks, Chorus Plants, Tripwire, and Note Block state
  conversions.
- **File Caching System**: Implemented `FileCacheManager` for efficient YAML and JSON file caching and validation.
- **Enhanced Item Components**: Added support for kinetic, piercing, attack range, swing animation, and damage type
  components (Nexo).
- **German Translation**: Added full support for the German language.
- **Internal Refactoring**: Reorganized package structure, updated block system with builder patterns, and improved
  furniture class handling.
- **Improved Logging & Error Handling**: Enhanced message clarity, added configuration load time logging, and detailed
  error messages for configuration failures.
- **Block State Mapping Scanner**: Automated scanning of CraftEngine configuration files for block state limits.
- **World conversion engine** (chunk & entity processing, async, progress tracking)
- **Block restoration feature** (with batch, error handling, and DB migration)
- **Entity restoration in world converter**
- **Handheld item model & improved texture conversion**
- **Progress bar improvements & tracking for large conversions**
- **Elytra default slot fix**
- **ItemsAdder interaction conversion** (blocks/furniture)
- **ItemsAdder image placeholder support**
- **ItemsAdder basic furniture conversion**
- **ItemsAdder pack conversion**
- **ItemsAdder recipe conversion**
- **ItemsAdder song conversion**
- **ItemsAdder font conversion** (no emojis yet)
- **Multiple Translation support** (English, French, German)
- **Multi-threaded pack conversion**: Support for `--threads=<number>` argument.
- **Dry-run mode**: Support for `--dryrun` argument.
- **Folia compatibility**

---

**Legend:**

- 🚀 Roadmap | 🛡️ Security | 🧪 Testing | 📚 Docs | 🎨 Features | 🐛 Bug | 🔄 DevOps | ✅ Done

*For contribution guidelines, see [CONTRIBUTING.md](CONTRIBUTING.md)*
