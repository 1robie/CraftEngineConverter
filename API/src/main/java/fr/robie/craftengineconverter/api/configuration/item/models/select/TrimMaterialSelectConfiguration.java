package fr.robie.craftengineconverter.api.configuration.item.models.select;

/**
 * Selects a model by the armour trim material applied to the item.
 * <p>
 * Case values are resource locations (for example {@code minecraft:gold}) rather than a closed enum,
 * so they stay {@link String}s. Geyser can represent this directly as a {@code match} predicate on
 * {@code trim_material}.
 */
public class TrimMaterialSelectConfiguration extends SelectModelConfiguration<String> {

    public TrimMaterialSelectConfiguration() {
        super("minecraft:trim_material");
    }
}
