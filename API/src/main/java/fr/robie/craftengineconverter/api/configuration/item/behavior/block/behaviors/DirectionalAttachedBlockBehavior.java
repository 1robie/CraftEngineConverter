package fr.robie.craftengineconverter.api.configuration.item.behavior.block.behaviors;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.BlockBehavior;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DirectionalAttachedBlockBehavior implements BlockBehavior {
    private Boolean blacklist;
    private final List<String> attachedBlocks;
    private final List<String> attachedBlockTags;

    public DirectionalAttachedBlockBehavior() {
        this.attachedBlocks = new ArrayList<>();
        this.attachedBlockTags = new ArrayList<>();
    }

    public DirectionalAttachedBlockBehavior setBlacklist(boolean blacklist) {
        this.blacklist = blacklist;
        return this;
    }

    public DirectionalAttachedBlockBehavior addAttachedBlock(String block) {
        this.attachedBlocks.add(block);
        return this;
    }

    public DirectionalAttachedBlockBehavior addAttachedBlockTag(String tag) {
        this.attachedBlockTags.add(tag);
        return this;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "directional_attached_block");
        if (this.blacklist != null) {
            data.put("blacklist", this.blacklist);
        }
        if (!this.attachedBlocks.isEmpty()) {
            data.put("attached-blocks", this.attachedBlocks);
        }
        if (!this.attachedBlockTags.isEmpty()) {
            data.put("attached-block-tags", this.attachedBlockTags);
        }
        return data;
    }
}
