package fr.robie.craftengineconverter.common.utils.yaml.constructor;

import fr.robie.craftengineconverter.common.utils.yaml.directive.KeyDirective;
import fr.robie.craftengineconverter.common.utils.yaml.directive.KeyDirectiveRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    private static final String FALLBACK_STRIPPED = "fallback";

    public SmartConstructor(@NotNull LoaderOptions options) {
        super(options);
    }

    @Override
    public void flattenMapping(@NotNull final MappingNode node) {
        super.flattenMapping(node);
    }

    @Nullable
    public Object construct(@NotNull Node node) {
        return this.constructObject(node);
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
                String strippedKey = key.substring(PREFIX.length());
                KeyDirective directive = KeyDirectiveRegistry.findMatch(strippedKey);
                if (directive != null) {
                    directive.handleBlockMerge(map, strippedKey, valueNode, this);
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
    private void processDeepKey(Map<Object, Object> rootMap, String fullKey, Node valueNode) {
        String[] parts = fullKey.split("::");
        Object current = rootMap;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            String nextPart = parts[i + 1];
            boolean isNextIndex = this.isInteger(nextPart);

            if (current instanceof Map) {
                Map<Object, Object> map = (Map<Object, Object>) current;
                Object existing = map.get(part);

                if (existing != null) {
                    current = existing;
                } else {
                    Object next;
                    if (isNextIndex) {
                        next = new java.util.ArrayList<>();
                    } else {
                        next = new LinkedHashMap<>();
                    }
                    map.put(part, next);
                    current = next;
                }
            } else if (current instanceof java.util.List) {
                java.util.List<Object> list = (java.util.List<Object>) current;
                int index = Integer.parseInt(part);

                while (list.size() <= index) {
                    list.add(null);
                }

                Object existing = list.get(index);
                if (existing != null) {
                    current = existing;
                } else {
                    Object next;
                    if (isNextIndex) {
                        next = new java.util.ArrayList<>();
                    } else {
                        next = new LinkedHashMap<>();
                    }
                    list.set(index, next);
                    current = next;
                }
            }
        }

        String finalPart = parts[parts.length - 1];
        Object value = this.constructObjectPublic(valueNode);

        if (current instanceof Map) {
            this.setWithMerge((Map<Object, Object>) current, finalPart, value, fullKey);
        } else if (current instanceof java.util.List) {
            java.util.List<Object> list = (java.util.List<Object>) current;
            int index = Integer.parseInt(finalPart);
            while (list.size() <= index) {
                list.add(null);
            }
            list.set(index, value);
        }
    }

    private boolean isInteger(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private Object constructValueSelector(MappingNode node) {
        Object fallback = null;
        Object matched = null;

        for (NodeTuple tuple : node.getValue()) {
            String key = this.constructScalar((ScalarNode) tuple.getKeyNode());
            String strippedKey = key.startsWith(PREFIX) ? key.substring(PREFIX.length()) : key;

            if (FALLBACK_STRIPPED.equals(strippedKey)) {
                fallback = this.constructObjectPublic(tuple.getValueNode());
                continue;
            }

            KeyDirective directive = KeyDirectiveRegistry.findMatch(strippedKey);
            if (directive != null) {
                Object candidate = directive.handleValueSelect(strippedKey, tuple.getValueNode(), this);
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