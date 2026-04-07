package fr.robie.craftengineconverter.common.utils.yaml.file;

import com.google.common.base.Preconditions;
import fr.robie.craftengineconverter.api.logger.Logger;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import fr.robie.craftengineconverter.common.utils.yaml.constructor.SmartConstructor;
import fr.robie.craftengineconverter.common.utils.yaml.serialization.ConfigurationSerialization;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class YamlConfiguration extends FileConfiguration {
    private final DumperOptions yamlDumperOptions;
    private final LoaderOptions yamlLoaderOptions;
    private final SmartConstructor constructor;
    private final YamlRepresenter representer;
    private final Yaml yaml;

    public YamlConfiguration() {
        this.yamlDumperOptions = new DumperOptions();
        this.yamlDumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yamlLoaderOptions = new LoaderOptions();
        this.yamlLoaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
        this.yamlLoaderOptions.setCodePointLimit(Integer.MAX_VALUE);
        this.yamlLoaderOptions.setNestingDepthLimit(100);

        this.constructor = new SmartConstructor(this.yamlLoaderOptions);
        this.representer = new YamlRepresenter(this.yamlDumperOptions);
        this.representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        this.yaml = new Yaml(this.constructor, this.representer, this.yamlDumperOptions, this.yamlLoaderOptions);
    }

    @NotNull
    @Override
    public String saveToString() {
        this.yamlDumperOptions.setIndent(this.options().indent());
        this.yamlDumperOptions.setWidth(this.options().width());
        this.yamlDumperOptions.setProcessComments(this.options().parseComments());

        MappingNode node = this.toNodeTree(this);

        node.setBlockComments(this.getCommentLines(this.saveHeader(this.options().getHeader()), CommentType.BLOCK));
        node.setEndComments(this.getCommentLines(this.options().getFooter(), CommentType.BLOCK));

        StringWriter writer = new StringWriter();
        if (node.getBlockComments().isEmpty() && node.getEndComments().isEmpty() && node.getValue().isEmpty()) {
            writer.write("");
        } else {
            if (node.getValue().isEmpty()) {
                node.setFlowStyle(DumperOptions.FlowStyle.FLOW);
            }
            this.yaml.serialize(node, writer);
        }
        return writer.toString();
    }

    @Override
    public void loadFromString(@NotNull String contents) throws InvalidConfigurationException {
        Preconditions.checkNotNull(contents, "Contents cannot be null");
        this.yamlLoaderOptions.setProcessComments(this.options().parseComments());
        this.yamlLoaderOptions.setCodePointLimit(this.options().codePointLimit());

        MappingNode node;
        try (Reader reader = new UnicodeReader(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)))) {
            Node rawNode = this.yaml.compose(reader);
            try {
                node = (MappingNode) rawNode;
            } catch (ClassCastException e) {
                throw new InvalidConfigurationException("Top level is not a Map.");
            }
        } catch (YAMLException | IOException | ClassCastException e) {
            throw new InvalidConfigurationException(e);
        }

        this.map.clear();

        if (node != null) {
            this.adjustNodeComments(node);
            this.options().setHeader(this.loadHeader(this.getCommentLines(node.getBlockComments())));
            this.options().setFooter(this.getCommentLines(node.getEndComments()));
            this.fromNodeTree(node, this);
        }
    }

    private void adjustNodeComments(final MappingNode node) {
        if (node.getBlockComments() == null && !node.getValue().isEmpty()) {
            Node firstNode = node.getValue().getFirst().getKeyNode();
            List<CommentLine> lines = firstNode.getBlockComments();
            if (lines != null) {
                int index = -1;
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).getCommentType() == CommentType.BLANK_LINE) {
                        index = i;
                    }
                }
                if (index != -1) {
                    node.setBlockComments(lines.subList(0, index + 1));
                    firstNode.setBlockComments(lines.subList(index + 1, lines.size()));
                }
            }
        }
    }

    private void fromNodeTree(@NotNull MappingNode input, @NotNull ConfigurationSection section) {
        this.constructor.flattenMapping(input);
        for (NodeTuple nodeTuple : input.getValue()) {
            Node key = nodeTuple.getKeyNode();
            String keyString = String.valueOf(this.constructor.construct(key));
            Node value = nodeTuple.getValueNode();

            while (value instanceof AnchorNode) {
                value = ((AnchorNode) value).getRealNode();
            }

            if (value instanceof MappingNode && !this.hasSerializedTypeKey((MappingNode) value)) {
                this.fromNodeTree((MappingNode) value, section.createSection(keyString));
            } else {
                section.set(keyString, this.constructor.construct(value));
            }

            section.setComments(keyString, this.getCommentLines(key.getBlockComments()));
            if (value instanceof MappingNode || value instanceof SequenceNode) {
                section.setInlineComments(keyString, this.getCommentLines(key.getInLineComments()));
            } else {
                section.setInlineComments(keyString, this.getCommentLines(value.getInLineComments()));
            }
        }
    }

    private boolean hasSerializedTypeKey(MappingNode node) {
        for (NodeTuple nodeTuple : node.getValue()) {
            Node keyNode = nodeTuple.getKeyNode();
            if (!(keyNode instanceof ScalarNode)) {
                continue;
            }
            String key = ((ScalarNode) keyNode).getValue();
            if (key.equals(ConfigurationSerialization.SERIALIZED_TYPE_KEY)) {
                return true;
            }
        }
        return false;
    }

    private MappingNode toNodeTree(@NotNull ConfigurationSection section) {
        List<NodeTuple> nodeTuples = new ArrayList<>();
        for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
            Node key = this.representer.represent(entry.getKey());
            Node value;
            if (entry.getValue() instanceof ConfigurationSection) {
                value = this.toNodeTree((ConfigurationSection) entry.getValue());
            } else {
                value = this.representer.represent(entry.getValue());
            }
            key.setBlockComments(this.getCommentLines(section.getComments(entry.getKey()), CommentType.BLOCK));
            if (value instanceof MappingNode || value instanceof SequenceNode) {
                key.setInLineComments(this.getCommentLines(section.getInlineComments(entry.getKey()), CommentType.IN_LINE));
            } else {
                value.setInLineComments(this.getCommentLines(section.getInlineComments(entry.getKey()), CommentType.IN_LINE));
            }

            nodeTuples.add(new NodeTuple(key, value));
        }

        return new MappingNode(Tag.MAP, nodeTuples, DumperOptions.FlowStyle.BLOCK);
    }

    private List<String> getCommentLines(List<CommentLine> comments) {
        List<String> lines = new ArrayList<>();
        if (comments != null) {
            for (CommentLine comment : comments) {
                if (comment.getCommentType() == CommentType.BLANK_LINE) {
                    lines.add(null);
                } else {
                    String line = comment.getValue();
                    line = line.startsWith(" ") ? line.substring(1) : line;
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    private List<CommentLine> getCommentLines(List<String> comments, CommentType commentType) {
        List<CommentLine> lines = new ArrayList<CommentLine>();
        for (String comment : comments) {
            if (comment == null) {
                lines.add(new CommentLine(null, null, "", CommentType.BLANK_LINE));
            } else {
                String line = comment;
                line = line.isEmpty() ? line : " " + line;
                lines.add(new CommentLine(null, null, line, commentType));
            }
        }
        return lines;
    }

    private List<String> loadHeader(List<String> header) {
        LinkedList<String> list = new LinkedList<>(header);

        if (!list.isEmpty()) {
            list.removeLast();
        }

        while (!list.isEmpty() && list.peek() == null) {
            list.remove();
        }

        return list;
    }

    private List<String> saveHeader(List<String> header) {
        LinkedList<String> list = new LinkedList<>(header);

        if (!list.isEmpty()) {
            list.add(null);
        }

        return list;
    }

    @NotNull
    @Override
    public YamlConfigurationOptions options() {
        if (this.options == null) {
            this.options = new YamlConfigurationOptions(this);
        }

        return (YamlConfigurationOptions) this.options;
    }

    @NotNull
    public static YamlConfiguration loadConfiguration(@NotNull File file) {
        Preconditions.checkNotNull(file, "File cannot be null");

        YamlConfiguration config = new YamlConfiguration();

        try {
            config.load(file);
        } catch (FileNotFoundException ignored) {
        } catch (IOException | InvalidConfigurationException ex) {
            Logger.showException("Cannot load configuration from file: " + file, ex);
        }

        return config;
    }

    @NotNull
    public static YamlConfiguration loadConfiguration(@NotNull Reader reader) {
        Preconditions.checkNotNull(reader, "Reader cannot be null");

        YamlConfiguration config = new YamlConfiguration();

        try {
            config.load(reader);
        } catch (IOException | InvalidConfigurationException ex) {
            Logger.showException("Cannot load configuration from stream.", ex);
        }

        return config;
    }
}
