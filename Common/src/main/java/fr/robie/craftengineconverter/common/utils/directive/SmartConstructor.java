package fr.robie.craftengineconverter.common.utils.directive;

import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.util.LinkedHashMap;
import java.util.Map;

public class SmartConstructor extends SafeConstructor {

    private static final String PREFIX = "$$";
    private static final String FALLBACK = "$$fallback";

    public SmartConstructor(@NotNull LoaderOptions options) {
        super(options);
    }


    @Override
    public Object constructObject(Node node) {
        if (node instanceof MappingNode mn && this.isValueSelectorNode(mn)) {
            return this.constructValueSelector(mn);
        }
        return super.constructObject(node);
    }

    @Override
    protected Map<Object, Object> constructMapping(MappingNode node) {
        Map<Object, Object> map = new LinkedHashMap<>();

        for (NodeTuple tuple : node.getValue()) {
            if (!(tuple.getKeyNode() instanceof ScalarNode scalarNode)) {
                continue;
            }

            String key = this.constructScalar(scalarNode);
            Node valueNode = tuple.getValueNode();

            if (key.startsWith(PREFIX)) {
                KeyDirective directive = KeyDirectiveRegistry.findMatch(key);
                if (directive != null) {
                    directive.handleBlockMerge(map, key, valueNode, this);
                }
            } else if (key.contains("::")) {
                this.processDeepKey(map, key, valueNode);
            } else {
                Object value = this.constructObjectPublic(valueNode);
                this.setWithMerge(map, key, value, key);
            }
        }

        return map;
    }

    private boolean isValueSelectorNode(MappingNode node) {
        if (node.getValue().isEmpty()) {
            return false;
        }
        for (NodeTuple t : node.getValue()) {
            if (!(t.getKeyNode() instanceof ScalarNode sn)) {
                return false;
            }
            if (!sn.getValue().startsWith(PREFIX)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void processDeepKey(Map<Object, Object> map, String fullKey, Node valueNode) {
        String[] parts = fullKey.split("::");
        Map<Object, Object> current = map;

        for (int i = 0; i < parts.length - 1; i++) {
            Object existing = current.get(parts[i]);
            if (existing instanceof Map) {
                current = (Map<Object, Object>) existing;
            } else {
                Map<Object, Object> nested = new LinkedHashMap<>();
                current.put(parts[i], nested);
                current = nested;
            }
        }

        String finalKey = parts[parts.length - 1];
        Object value = this.constructObjectPublic(valueNode);
        this.setWithMerge(current, finalKey, value, fullKey);
    }

    private Object constructValueSelector(MappingNode node) {
        Object fallback = null;
        Object matched = null;

        for (NodeTuple tuple : node.getValue()) {
            String key = this.constructScalar((ScalarNode) tuple.getKeyNode());

            if (FALLBACK.equals(key)) {
                fallback = this.constructObjectPublic(tuple.getValueNode());
                continue;
            }

            KeyDirective directive = KeyDirectiveRegistry.findMatch(key);
            if (directive != null) {
                Object candidate = directive.handleValueSelect(key, tuple.getValueNode(), this);
                if (candidate != null) {
                    matched = candidate;
                }
            }
        }

        return matched != null ? matched : fallback;
    }

    public Object constructObjectPublic(@NotNull Node node) {
        return this.constructObject(node);
    }

    @SuppressWarnings("unchecked")
    public void mergeInto(@NotNull Map<Object, Object> target, @NotNull Map<Object, Object> source, @NotNull String parentPath) {
        for (Map.Entry<Object, Object> e : source.entrySet()) {
            String key = e.getKey().toString();
            Object srcVal = e.getValue();
            Object tgtVal = target.get(key);

            String path = parentPath.isEmpty() ? key : parentPath + "." + key;

            if (tgtVal == null) {
                target.put(key, srcVal);
            } else if (tgtVal instanceof Map && srcVal instanceof Map) {
                this.mergeInto((Map<Object, Object>) tgtVal, (Map<Object, Object>) srcVal, path);
            } else {
                target.put(key, srcVal);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void setWithMerge(Map<Object, Object> map, String key, Object value, String path) {
        Object existing = map.get(key);
        if (existing instanceof Map && value instanceof Map) {
            this.mergeInto((Map<Object, Object>) existing, (Map<Object, Object>) value, path);
        } else {
            map.put(key, value);
        }
    }
}