package fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HangingBlockBehavior implements BlockBehavior {
    private Boolean stackable;
    private Boolean blacklist;
    private final List<String> aboveBlockTags;
    private final List<String> aboveBlocks;
    private Integer delay;

    public HangingBlockBehavior() {
        this.aboveBlockTags = new ArrayList<>();
        this.aboveBlocks = new ArrayList<>();
    }

    public HangingBlockBehavior setStackable(boolean stackable) {
        this.stackable = stackable;
        return this;
    }

    public HangingBlockBehavior setBlacklist(boolean blacklist) {
        this.blacklist = blacklist;
        return this;
    }

    public HangingBlockBehavior addAboveBlockTag(String tag) {
        this.aboveBlockTags.add(tag);
        return this;
    }

    public HangingBlockBehavior addAboveBlock(String block) {
        this.aboveBlocks.add(block);
        return this;
    }

    public HangingBlockBehavior setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "hanging_block");
        if (this.stackable != null) {
            data.put("stackable", this.stackable);
        }
        if (this.blacklist != null) {
            data.put("blacklist", this.blacklist);
        }
        if (!this.aboveBlockTags.isEmpty()) {
            data.put("above-block-tags", this.aboveBlockTags);
        }
        if (!this.aboveBlocks.isEmpty()) {
            data.put("above-blocks", this.aboveBlocks);
        }
        if (this.delay != null) {
            data.put("delay", this.delay);
        }
        return data;
    }
}
