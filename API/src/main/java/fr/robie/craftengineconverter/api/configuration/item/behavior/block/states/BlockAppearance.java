package fr.robie.craftengineconverter.api.configuration.item.behavior.block.states;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities.BlockEntity;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities.BlockEntityConfiguration;
import fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities.ItemDisplayRenderer;
import fr.robie.craftengineconverter.api.configuration.item.models.ModelConfiguration;
import fr.robie.craftengineconverter.api.enums.CraftEngineBlockState;
import fr.robie.craftengineconverter.api.enums.Plugins;
import fr.robie.craftengineconverter.api.utils.BlockStateResult;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class BlockAppearance implements SectionProvider {
    private final @Nullable Plugins requiredPlugin;
    private final @Nullable CraftEngineBlockState autoState;
    private final @Nullable String visualState;
    private final @Nullable String itemId;
    private final ModelConfiguration model;
    private final @Nullable BlockEntity blockEntity;
    private final @Nullable Consumer<ConfigurationSection> preSerializationConsumer;
    private final @Nullable Consumer<ConfigurationSection> postSerializationConsumer;

    private BlockAppearance(Builder builder) {
        this.requiredPlugin = builder.requiredPlugin;
        this.autoState = builder.autoState;
        this.visualState = builder.visualState;
        this.itemId = builder.itemId;
        this.model = builder.model;
        this.preSerializationConsumer = builder.preSerializationConsumer;
        this.postSerializationConsumer = builder.postSerializationConsumer;
        this.blockEntity = builder.blockEntity;
    }

    public void serialize(@NotNull ConfigurationSection section) {
        if (this.preSerializationConsumer != null) {
            this.preSerializationConsumer.accept(section);
        }

        boolean blockEntityAdded = false;

        if (this.autoState != null) {
            BlockStateResult available = this.autoState.getAvailableAndIncrementNotLast(this.requiredPlugin);
            if (available != null) {
                if (available.isLast()) {
                    section.set("state", available.getBlockState().getLastBlockState());
                    if (this.blockEntity != null) {
                        this.blockEntity.serialize(section);
                    } else {
                        BlockEntityConfiguration blockEntityConfiguration = new BlockEntityConfiguration();
                        blockEntityConfiguration.addEntityRenderer(new ItemDisplayRenderer().setItem(this.itemId));
                        blockEntityConfiguration.serialize(section);
                    }
                    blockEntityAdded = true;
                } else {
                    section.set("auto-state", available.getName());
                }
            }
        } else if (this.visualState != null) {
            section.set("state", this.visualState);
        }
        ConfigurationSection modelSection = this.getOrCreateSection(section, "model");
        this.model.serialize(modelSection);

        if (!blockEntityAdded && this.blockEntity != null) {
            this.blockEntity.serialize(section);
        }

        if (this.postSerializationConsumer != null) {
            this.postSerializationConsumer.accept(section);
        }
    }

    public static Builder autoState(@NotNull Plugins requiredPlugin, @NotNull CraftEngineBlockState autoState, @NotNull String itemId, @NotNull ModelConfiguration model) {
        return new Builder(model).requiredPlugin(requiredPlugin).autoState(autoState).itemId(itemId);
    }

    public static Builder visualState(@NotNull String visualState, @NotNull ModelConfiguration model) {
        return new Builder(model).visualState(visualState);
    }

    public static class Builder {
        private @Nullable Plugins requiredPlugin;
        private @Nullable CraftEngineBlockState autoState;
        private @Nullable String visualState;
        private @Nullable String itemId;
        private final ModelConfiguration model;
        private @Nullable Consumer<ConfigurationSection> preSerializationConsumer;
        private @Nullable Consumer<ConfigurationSection> postSerializationConsumer;
        private @Nullable BlockEntity blockEntity;

        private Builder(@NotNull ModelConfiguration model) {
            this.model = model;
        }

        private Builder requiredPlugin(@NotNull Plugins requiredPlugin) {
            this.requiredPlugin = requiredPlugin;
            return this;
        }

        private Builder autoState(@NotNull CraftEngineBlockState autoState) {
            this.autoState = autoState;
            return this;
        }

        private Builder visualState(@NotNull String visualState) {
            this.visualState = visualState;
            return this;
        }

        private Builder itemId(@NotNull String itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder blockEntity(@NotNull BlockEntity blockEntity) {
            this.blockEntity = blockEntity;
            return this;
        }

        public Builder preProcessor(@NotNull Consumer<ConfigurationSection> preSerializationConsumer) {
            this.preSerializationConsumer = preSerializationConsumer;
            return this;
        }

        public Builder postProcessor(@NotNull Consumer<ConfigurationSection> postSerializationConsumer) {
            this.postSerializationConsumer = postSerializationConsumer;
            return this;
        }

        public BlockAppearance build() {
            return new BlockAppearance(this);
        }
    }
}