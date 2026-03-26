package fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BushBlockBehavior implements BlockBehavior {
    private Boolean blacklist;
    private Boolean stackable;
    private Integer maxHeight;
    private Integer delay;
    private final List<String> bottomBlocks;
    private final List<String> bottomBlockTags;

    public BushBlockBehavior() {
        this.bottomBlocks = new ArrayList<>();
        this.bottomBlockTags = new ArrayList<>();
    }

    public BushBlockBehavior setBlacklist(boolean blacklist) {
        this.blacklist = blacklist;
        return this;
    }

    public BushBlockBehavior setStackable(boolean stackable) {
        this.stackable = stackable;
        return this;
    }

    public BushBlockBehavior setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        return this;
    }

    public BushBlockBehavior setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    public BushBlockBehavior addBottomBlock(String block) {
        this.bottomBlocks.add(block);
        return this;
    }

    public BushBlockBehavior addBottomBlockTag(String tag) {
        this.bottomBlockTags.add(tag);
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "bush_block");
        if (this.blacklist != null) {
            data.put("blacklist", this.blacklist);
        }
        if (this.stackable != null) {
            data.put("stackable", this.stackable);
        }
        if (this.maxHeight != null) {
            data.put("max-height", this.maxHeight);
        }
        if (this.delay != null) {
            data.put("delay", this.delay);
        }
        if (!this.bottomBlocks.isEmpty()) {
            data.put("bottom-blocks", this.bottomBlocks);
        }
        if (!this.bottomBlockTags.isEmpty()) {
            data.put("bottom-block-tags", this.bottomBlockTags);
        }
        return data;
    }
}
