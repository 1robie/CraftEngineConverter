package fr.robie.craftengineconverter.api.configuration.bedrock.mapping.item.option;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Set;

public class BedrockOptions {
    private String icon;
    private boolean allowOffhand = true;
    private boolean displayHandheld = false;
    private int protectionValue = 0;
    private CreativeCategory creativeCategory = CreativeCategory.NONE;
    //TODO: creative_group
    private final Set<String> tags = new HashSet<>();

    public JsonObject serialize() {
        JsonObject jsonObject = new JsonObject();
        if (this.icon != null) {
            jsonObject.addProperty("icon", this.icon);
        }
        if (!this.allowOffhand) {
            jsonObject.addProperty("allow_offhand", false);
        }
        if (this.displayHandheld) {
            jsonObject.addProperty("display_handheld", true);
        }
        if (this.protectionValue != 0) {
            jsonObject.addProperty("protection_value", this.protectionValue);
        }
        if (this.creativeCategory != CreativeCategory.NONE) {
            jsonObject.addProperty("creative_category", this.creativeCategory.name().toLowerCase());
        }
        if (!this.tags.isEmpty()) {
            JsonArray tagsArray = new JsonArray();
            for (String tag : this.tags) {
                tagsArray.add(tag);
            }
            jsonObject.add("tags", tagsArray);
        }
        return jsonObject;
    }

    public BedrockOptions setIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public String getIcon() {
        return this.icon;
    }

    public BedrockOptions setAllowOffhand(boolean allowOffhand) {
        this.allowOffhand = allowOffhand;
        return this;
    }

    public boolean isAllowOffhand() {
        return this.allowOffhand;
    }

    public BedrockOptions setDisplayHandheld(boolean displayHandheld) {
        this.displayHandheld = displayHandheld;
        return this;
    }

    public boolean isDisplayHandheld() {
        return this.displayHandheld;
    }

    public BedrockOptions setProtectionValue(int protectionValue) {
        this.protectionValue = protectionValue;
        return this;
    }

    public int getProtectionValue() {
        return this.protectionValue;
    }

    public BedrockOptions setCreativeCategory(CreativeCategory creativeCategory) {
        this.creativeCategory = creativeCategory;
        return this;
    }

    public CreativeCategory getCreativeCategory() {
        return this.creativeCategory;
    }

    public BedrockOptions addTag(String tag) {
        if (tag != null && !tag.isEmpty()) {
            this.tags.add(tag);
        }
        return this;
    }

    public BedrockOptions setTags(Set<String> tags) {
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
        return this;
    }

    public Set<String> getTags() {
        return new HashSet<>(this.tags);
    }

    public enum CreativeCategory {
        NONE,
        CONSTRUCTION,
        NATURE,
        EQUIPMENT,
        ITEMS
    }
}
