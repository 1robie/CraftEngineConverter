# 0.0.2

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

## ItemsAdder

- World converter now support ItemsAdder blocks/furniture
- Some fix for conversion stuff

# 0.0.1

