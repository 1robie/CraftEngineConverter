# 🗺️ ROADMAP - CraftEngineConverter

> **Last Updated:** December 23, 2025
> **Project Status:** 🟢 Active Development
> **This file serves as the main project roadmap and development tracker.**

---

## 🚀 Roadmap

### Version 1.0.0 ()
- [ ] Complete Nexo conversion (items, glyphs, emojis, images, languages, sounds, equipment, furniture, custom blocks, mechanics)
- [ ] Full security audit passed
- [ ] 80%+ test coverage
- [ ] Documentation complete

### Version 1.1.0 (ItemsAdder Support)
- [ ] Items, blocks converter
- [ ] Resource pack migration
- [ ] Documentation & examples
 
### Version 1.2.0 (Oraxen Support)
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
- [ ] Fix all `mkdirs()`/`delete()` ignored warnings
- [ ] Add `try-with-resources` for SnakeUtils
- [ ] Remove unnecessary semicolons
- [ ] Refactor duplicate code in armor conversion

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
- [ ] Wiki pages
- [ ] Migration guides
- [ ] FAQ section
- [ ] API documentation & code examples
- [ ] Tag processor & extension guide

---

## 🎨 Features & Enhancements
- [x] Glyph tag processor
- [x] PlaceholderAPI tag processor
- [ ] Custom tag creation API
- [ ] Tag validation and sanitization
- [ ] Partial conversion (select items)
- [ ] Dry-run mode (preview)
- [ ] Backup/rollback system
- [ ] Conversion profiles (save/load)
- [ ] Better console output (colors, formatting)
- [ ] Progress bars for long operations
- [ ] Optimize async conversion (thread pools)
- [ ] Progress tracking for large conversions
- [ ] Cache frequently accessed configs
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
- Fixed NoSuchFileException in ZIP extraction
- Added URL decoding validation (..%2F)
- Added UNC path blocking (\\server\share)
- Created comprehensive security tests
- Updated README, CONTRIBUTING, SECURITY_TESTING
- Implemented Zip Slip protection
- Created SnakeUtils utility
- Tag processor system
- Multi-language support
- Equipment conversion (Component & Trim)
- Folia compatibility

---

**Legend:**
- 🚀 Roadmap | 🛡️ Security | 🧪 Testing | 📚 Docs | 🎨 Features | 🐛 Bug | 🔄 DevOps | ✅ Done

*For contribution guidelines, see [CONTRIBUTING.md](CONTRIBUTING.md)*
