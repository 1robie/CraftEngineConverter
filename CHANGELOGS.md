# 0.0.2

- Message now support multiples type (`TITLE`, `BOSS_BAR` (Required Paper Server), `TCHAT`, `ACTION_BAR`,
  `TCHAT_AND_ACTION_BAR`, `WITHOUT_PREFIX`, `NONE`).

  By default, a simple string or list is treated as `TCHAT`:

```yaml
  command:
    help: "Hello world!"
    # or
    help2:
      - "Line 1"
      - "Line 2"
```

To use a different type, wrap it in a map with a `type` key:

```yaml
  # Action bar
  command:
    help:
      type: ACTION_BAR
      message: "Hello world!"

    # Title
    help2:
      type: TITLE
      title: "Hello"
      subtitle: "World"
      fade-in: 10
      stay: 70
      fade-out: 20

    # Boss bar (Paper only)
    help3:
      type: BOSS_BAR
      title: "Hello world!"
      color: PINK
      overlay: PROGRESS
      duration: 5
      progress: 1.0

    # Multiple types at once
    help4:
      - type: TCHAT
        message: "Hello in chat!"
      - type: ACTION_BAR
        message: "Hello in action bar!"
      - type: TITLE
        title: "Hello"
        subtitle: "World"
        fade-in: 10
        stay: 70
        fade-out: 20
```

- `message.yml` was automatically cleaned up, removing all obsolete keys and creating a backup of the original file.
  Missing keys are now logged with improved clarity.
- Custom Tag API, can be obtaint with `getServer().getServicesManager().getRegistration(ITagResolver.class)`.
- New setting `auto-convert-on-startup-types`. All converter can be configured to only run specific conversion.

```yaml
# If empty, all options for all converters will be used
# Available options: ITEMS, EMOJIS, IMAGES, LANGUAGES, SOUNDS, RECIPES, PACKS, ALL
# auto-convert-on-startup-types:
#   nexo:
#     - ITEMS
#     - EMOJIS
#   itemsadder:
#     - ITEMS
#     - IMAGES
```

- Some internal refractor to remove code duplication to write CraftEngine item syntax

## Nexo

- Reworked parent model conversion to support all parent model types.
- Block/ furniture drops now converted
- Directional blocks conversion (logs, furnace, dropper)
- Support for `WOOD` minimal_type tool
- Fix error log when item is a children for a directional block
- Add support for `disabled_recipes` file

## ItemsAdder

- World converter now support ItemsAdder blocks/furniture
- Some fix for conversion stuff

# 0.0.1

