package fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities;

import fr.robie.craftengineconverter.api.configuration.item.behavior.block.states.SectionProvider;
import fr.robie.yamllibrary.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockEntityConfiguration implements BlockEntity, SectionProvider {
    private final List<EntityRenderer> entityRenderers = new ArrayList<>();
    private EntityCulling entityCulling;

    public void addEntityRenderer(@NotNull EntityRenderer renderer) {
        this.entityRenderers.add(renderer);
    }

    public void setEntityCulling(@NotNull EntityCulling culling) {
        this.entityCulling = culling;
    }

    @Override
    public void serialize(@NotNull ConfigurationSection section) {
        if (!this.entityRenderers.isEmpty()) {
            if (this.entityRenderers.size() == 1) {
                section.set("entity-renderer", this.entityRenderers.getFirst().serialize());
            } else {
                List<Map<String, Object>> renderers = new ArrayList<>();
                for (EntityRenderer renderer : this.entityRenderers) {
                    renderers.add(renderer.serialize());
                }
                section.set("entity-renderer", renderers);
            }
        }
        if (this.entityCulling != null) {
            section.set("entity-culling", this.entityCulling.serialize());
        }
    }
}
