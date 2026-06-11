package fr.robie.craftengineconverter.api.configuration.item.behavior.furniture;

import fr.robie.yamllibrary.ConfigurationSection;
import net.momirealms.craftengine.core.entity.furniture.AlignmentRule;
import org.jetbrains.annotations.NotNull;

public class Rules {
    private boolean isModified = false;

    private FurnitureRotation rotation;
    private AlignmentRule alignment = AlignmentRule.ANY;

    public void setRotation(FurnitureRotation furnitureRotation) {
        this.rotation = furnitureRotation;
        this.isModified = true;
    }

    public void setAlignment(@NotNull AlignmentRule alignment) {
        this.alignment = alignment;
        this.isModified = true;
    }

    public void serialize(ConfigurationSection rulesSection) {
        rulesSection.set("alignment", this.alignment.name());

        if (this.rotation != null)
            rulesSection.set("rotation", this.rotation.name());
    }

    public boolean isModified() {
        return this.isModified;
    }
}
