package fr.robie.craftengineconverter.api.configuration.item.behavior.furniture;

import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.element.Element;
import fr.robie.craftengineconverter.api.configuration.item.behavior.furniture.hitbox.Hitbox;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Placement {
    private final FurniturePlacement type;
    private final float[] lootSpawnOffset = new float[3];
    private boolean entityCulling = false;

    private final Rules rules = new Rules();

    private final List<Element> elements = new ArrayList<>();
    private final List<Hitbox> hitboxes = new ArrayList<>();

    private String betterModel;
    private String modelEngine;

    public Placement(FurniturePlacement type) { this.type = type; }

    public FurniturePlacement getType() { return this.type; }

    public void setBetterModel(String betterModel) { this.betterModel = betterModel; }

    public void setModelEngine(String modelEngine) { this.modelEngine = modelEngine; }

    public void setLootSpawnOffset(float x, float y, float z) {
        this.lootSpawnOffset[0] = x;
        this.lootSpawnOffset[1] = y;
        this.lootSpawnOffset[2] = z;
    }

    public void addElement(Element element) { this.elements.add(element); }

    public void addHitbox(Hitbox hitbox) { this.hitboxes.add(hitbox); }

    public void setEntityCulling(boolean entityCulling) { this.entityCulling = entityCulling; }

    public Rules getRules() { return this.rules; }

    public void serialize(ConfigurationSection placementSection) {
        ConfigurationSection typeSection = placementSection.createSection(this.type.name().toLowerCase(Locale.ROOT));

        if (this.entityCulling)
            typeSection.set("entity-culling", true);

        if (this.betterModel != null) {
            typeSection.set("better-model", this.betterModel);
            return;
        }
        if (this.modelEngine != null) {
            typeSection.set("model-engine", this.modelEngine);
            return;
        }

        if (this.lootSpawnOffset[0] != 0 || this.lootSpawnOffset[1] != 0 || this.lootSpawnOffset[2] != 0)
            typeSection.set("loot-spawn-offset", this.lootSpawnOffset);

        if (this.rules.isModified()) {
            ConfigurationSection rulesSection = typeSection.createSection("rules");
            this.rules.serialize(rulesSection);
        }

        if (!this.elements.isEmpty()) {
            List<Map<String, Object>> serializedElements = new ArrayList<>();
            for (Element element : this.elements) {
                serializedElements.add(element.serialize());
            }
            typeSection.set("elements", serializedElements);
        }

        if (!this.hitboxes.isEmpty()) {
            List<Map<String, Object>> serializedHitboxes = new ArrayList<>();
            for (Hitbox hitbox : this.hitboxes) {
                serializedHitboxes.add(hitbox.serialize());
            }
            typeSection.set("hitboxes", serializedHitboxes);
        }
    }
}
