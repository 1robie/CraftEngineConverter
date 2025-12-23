# Contributing to CraftEngineConverter

Thank you for your interest in contributing to CraftEngineConverter! We welcome contributions from the community to help make this project better.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Making Changes](#making-changes)
- [Testing](#testing)
- [Submitting Changes](#submitting-changes)
- [Adding New Features](#adding-new-features)
- [Reporting Bugs](#reporting-bugs)

## Code of Conduct

By participating in this project, you agree to:
- Be respectful and constructive in all interactions
- Welcome newcomers and help them get started
- Focus on what is best for the community
- Show empathy towards other community members

## Getting Started

### Prerequisites

Before you begin, ensure you have the following installed:
- **Java Development Kit (JDK) 21** or higher
- **Apache Maven 3.6+**
- **Git**
- **IntelliJ IDEA** (recommended) or another Java IDE

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/1robie/CraftEngineConverter.git
   cd CraftEngineConverter
   ```
3. Add the upstream repository:
   ```bash
   git remote add upstream https://github.com/1robie/CraftEngineConverter.git
   ```

## Development Setup

### Building the Project

1. Build the entire project:
   ```bash
   mvn clean install
   ```

2. Build without running tests (faster):
   ```bash
   mvn clean install -DskipTests
   ```

3. Build a specific module:
   ```bash
   cd Plugin
   mvn clean package
   ```

### Running Tests

Run all tests:
```bash
mvn test
```

Run tests for a specific module:
```bash
cd Common
mvn test
```

Run a specific test class:
```bash
mvn test -Dtest=SnakeUtilsTest
```

### IDE Setup (IntelliJ IDEA)

1. Open the project in IntelliJ IDEA
2. The IDE should automatically detect the Maven project and import it
3. Ensure the Project SDK is set to Java 21:
   - `File > Project Structure > Project > SDK`
4. Enable annotation processing if not already enabled:
   - `File > Settings > Build, Execution, Deployment > Compiler > Annotation Processors`

## Project Structure

The project follows a multi-module Maven structure:

```
CraftEngineConverter/
├── API/                    # Public API for plugin integration
├── Common/                 # Core conversion logic and utilities
│   ├── cache/             # Caching mechanisms
│   ├── configuration/     # Configuration handling
│   ├── format/            # Format converters
│   ├── packet/            # Packet handling
│   └── tag/               # Tag processing system
├── Hooks/                  # Plugin hook implementations
│   ├── BOM/               # Bill of Materials
│   ├── PacketEvent/       # Packet event integration
│   └── PlaceholderAPI/    # PlaceholderAPI integration
└── Plugin/                 # Main plugin implementation
    ├── command/           # Command implementations
    ├── converter/         # Converter implementations
    ├── loader/            # Plugin loaders
    └── utils/             # Utility classes
```

### Key Components

- **SnakeUtils**: YAML manipulation utility (Common module)
- **TagProcessor**: Custom tag parsing system (Common module)
- **Converter**: Base converter interface and implementations (Plugin module)
- **CraftEngineImageUtils**: Image and glyph conversion (Common module)
- **ConfigurationManager**: Configuration handling (Common module)

## Coding Standards

### Java Code Style

- **Java Version**: Use Java 21 features where appropriate
- **Braces**: Use K&R style (opening brace on same line)
- **Naming Conventions**:
  - Classes: `PascalCase`
  - Methods/Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase`

### Documentation

- Add Javadoc comments for all public classes and methods
- Use `@Contract` annotations for null-safety contracts
- Use `@NotNull` and `@Nullable` annotations appropriately
- Include examples in documentation where helpful

Example:
```java
/**
 * Converts a Nexo item configuration to CraftEngine format.
 *
 * @param itemId The unique identifier of the item
 * @param nexoConfig The source Nexo configuration section
 * @return The converted CraftEngine configuration
 * @throws IllegalArgumentException if itemId is null or empty
 */
@Contract("null, _ -> fail")
@NotNull
public ConfigurationSection convertItem(@NotNull String itemId, @NotNull ConfigurationSection nexoConfig) {
    // Implementation
}
```

### Code Quality

- Write clean, readable code
- Follow SOLID principles
- Avoid code duplication (DRY principle)
- Use meaningful variable and method names
- Handle exceptions appropriately
- Add logging for important operations

### Null Safety

- Always use `@NotNull` and `@Nullable` annotations
- Use `Optional<T>` for methods that may return null
- Validate input parameters early
- Use contracts (`@Contract`) to document behavior

## Making Changes

### Branch Naming

Create a descriptive branch name:
- `feature/add-oraxen-support` - for new features
- `fix/null-pointer-in-converter` - for bug fixes
- `docs/update-readme` - for documentation
- `refactor/simplify-tag-processor` - for refactoring

### Commit Messages

Follow the conventional commits format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples**:
```
feat(converter): add Oraxen item conversion support

- Implement OraxenConverter class
- Add item model conversion
- Add texture pack mapping

Closes #42
```

```
fix(tag): resolve null pointer in GlyphTagProcessor

Fixed issue where escaped glyphs caused NPE when
PlaceholderAPI was not installed.

Fixes #38
```

## Testing

### Writing Tests

- Write unit tests for all new functionality
- Use JUnit 5 for testing
- Place test files in `src/test/java` mirroring the main package structure
- Name test classes with `Test` suffix (e.g., `SnakeUtilsTest`)
- Use descriptive test method names (e.g., `testGetSectionReturnsCorrectValue`)

Example test:
```java
@Test
void testConvertItemWithValidInput() {
    // Arrange
    SnakeUtils config = SnakeUtils.createEmpty();
    config.set("material", "DIAMOND_SWORD");
    
    // Act
    ConfigurationSection result = converter.convertItem("test_sword", config.getRoot());
    
    // Assert
    assertNotNull(result);
    assertEquals("DIAMOND_SWORD", result.getString("item"));
}
```

### Test Coverage

- Aim for at least 70% code coverage for new code
- Test edge cases and error conditions
- Test null handling
- Test with various input types

## Submitting Changes

### Pull Request Process

1. **Update your branch**:
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

2. **Ensure all tests pass**:
   ```bash
   mvn clean test
   ```

3. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

4. **Create a Pull Request** on GitHub with:
   - Clear title describing the change
   - Detailed description of what was changed and why
   - Reference to any related issues (e.g., "Closes #123")
   - Screenshots for UI changes (if applicable)

5. **Wait for review**:
   - Address any feedback from reviewers
   - Make requested changes in new commits
   - Keep the PR updated with upstream changes

### Pull Request Checklist

- [ ] Code follows project coding standards
- [ ] All tests pass (`mvn test`)
- [ ] New tests added for new functionality
- [ ] Documentation updated (README, Javadoc, etc.)
- [ ] No compiler warnings introduced
- [ ] Commit messages follow conventional format
- [ ] Branch is up-to-date with main
- [ ] No merge conflicts

## Adding New Features

### Adding a New Converter

1. Create a new converter class in `Plugin/src/main/java/fr/robie/craftengineconverter/converter/`
2. Extend the `Converter` interface or appropriate base class
3. Implement required conversion methods
4. Add tests in `Plugin/src/test/java/`
5. Update documentation

### Adding a New Tag Processor

1. Create processor class in `Common/src/main/java/fr/robie/craftengineconverter/common/tag/processor/`
2. Implement `TagProcessor` interface
3. Register in `TagResolverUtils`
4. Add tests
5. Document the new tag syntax in README

### Adding a New Hook

1. Create module in `Hooks/` directory
2. Add dependency in `pom.xml`
3. Implement hook interface
4. Add to BOM required to load the hook into `Plugin` module
5. Test integration

## Reporting Bugs

### Before Submitting

- Check if the bug has already been reported in Issues
- Verify you're using the latest version
- Test with minimal configuration to isolate the issue

### Bug Report Template

When creating a bug report, include:

- **Description**: Clear description of the issue
- **Steps to Reproduce**:
  1. Step one
  2. Step two
  3. ...
- **Expected Behavior**: What should happen
- **Actual Behavior**: What actually happens
- **Environment**:
  - CraftEngineConverter version
  - Minecraft version
  - Server software (Paper/Folia)
  - Java version
  - Relevant plugin versions
- **Logs**: Relevant error logs or stack traces
- **Configuration**: Relevant config snippets (sanitized)

## Questions?

If you have questions about contributing:
- Open a discussion on GitHub
- Check existing issues and pull requests
- Review the project documentation

## License

By contributing to CraftEngineConverter, you agree that your contributions will be licensed under the GPL-3.0 License.

---

Thank you for contributing to CraftEngineConverter! 🎉

