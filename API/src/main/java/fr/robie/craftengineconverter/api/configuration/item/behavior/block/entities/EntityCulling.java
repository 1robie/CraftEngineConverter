package fr.robie.craftengineconverter.api.configuration.item.behavior.block.entities;

import java.util.LinkedHashMap;
import java.util.Map;

public class EntityCulling {
    public Boolean enabled;
    public String aabb;
    public Integer viewDistance;
    public Boolean rayTracing;

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (this.enabled != null) {
            map.put("enabled", this.enabled);
        }
        if (this.aabb != null) {
            map.put("aabb", this.aabb);
        }
        if (this.viewDistance != null) {
            map.put("view-distance", this.viewDistance);
        }
        if (this.rayTracing != null) {
            map.put("ray-tracing", this.rayTracing);
        }
        return map;
    }
}
