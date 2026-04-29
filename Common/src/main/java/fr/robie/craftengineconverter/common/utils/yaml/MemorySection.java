package fr.robie.craftengineconverter.common.utils.yaml;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import fr.robie.craftengineconverter.api.yaml.Configuration;
import fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.bukkit.util.NumberConversions.*;


public class MemorySection implements ConfigurationSection {
    protected final Map<String, SectionPathData> map = new LinkedHashMap<String, SectionPathData>();
    private final Configuration root;
    private final ConfigurationSection parent;
    private final String path;
    private final String fullPath;


    protected MemorySection() {
        if (!(this instanceof Configuration)) {
            throw new IllegalStateException("Cannot construct a root MemorySection when not a Configuration");
        }

        this.path = "";
        this.fullPath = "";
        this.parent = null;
        this.root = (Configuration) this;
    }


    protected MemorySection(@NotNull ConfigurationSection parent, @NotNull String path) {
        Preconditions.checkNotNull(parent, "Parent cannot be null");
        Preconditions.checkNotNull(path, "Path cannot be null");

        this.path = path;
        this.parent = parent;
        this.root = parent.getRoot();

        Preconditions.checkArgument(this.root != null, "Path cannot be orphaned");

        this.fullPath = createPath(parent, path);
    }

    @Override
    public @NotNull Set<String> getKeys(boolean deep) {
        Set<String> result = new LinkedHashSet<String>();

        Configuration root = this.getRoot();
        if (root != null && root.options().copyDefaults()) {
            ConfigurationSection defaults = this.getDefaultSection();

            if (defaults != null) {
                result.addAll(defaults.getKeys(deep));
            }
        }

        this.mapChildrenKeys(result, this, deep);

        return result;
    }

    @Override
    @NotNull
    public Map<String, Object> getValues(boolean deep) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        Configuration root = this.getRoot();
        if (root != null && root.options().copyDefaults()) {
            ConfigurationSection defaults = this.getDefaultSection();

            if (defaults != null) {
                result.putAll(defaults.getValues(deep));
            }
        }

        this.mapChildrenValues(result, this, deep);

        return result;
    }

    @Override
    public boolean contains(@NotNull String path) {
        return this.contains(path, false);
    }

    @Override
    public boolean contains(@NotNull String path, boolean ignoreDefault) {
        return ((ignoreDefault) ? this.get(path, null) : this.get(path)) != null;
    }

    @Override
    public boolean isSet(@NotNull String path) {
        Configuration root = this.getRoot();
        if (root == null) {
            return false;
        }
        if (root.options().copyDefaults()) {
            return this.contains(path);
        }
        return this.get(path, null) != null;
    }

    @Override
    @NotNull
    public String getCurrentPath() {
        return this.fullPath;
    }

    @Override
    @NotNull
    public String getName() {
        return this.path;
    }

    @Override
    @Nullable
    public Configuration getRoot() {
        return this.root;
    }

    @Override
    @Nullable
    public ConfigurationSection getParent() {
        return this.parent;
    }

    @Override
    public void addDefault(@NotNull String path, @Nullable Object value) {
        Preconditions.checkNotNull(path, "Path cannot be null");

        Configuration root = this.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot add default without root");
        }
        if (root == this) {
            throw new UnsupportedOperationException("Unsupported addDefault(String, Object) implementation");
        }
        root.addDefault(createPath(this, path), value);
    }

    @Override
    @Nullable
    public ConfigurationSection getDefaultSection() {
        Configuration root = this.getRoot();
        Configuration defaults = root == null ? null : root.getDefaults();

        if (defaults != null) {
            if (defaults.isConfigurationSection(this.getCurrentPath())) {
                return defaults.getConfigurationSection(this.getCurrentPath());
            }
        }

        return null;
    }

    @Override
    public void set(@NotNull String path, @Nullable Object value) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(path), "Cannot set to an empty path");

        Configuration root = this.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot use section without a root");
        }

        final char separator = root.options().pathSeparator();

        int i1 = -1, i2;
        Object current = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            String node = path.substring(i2, i1);
            Object sub = this.getPart(current, node);

            if (sub instanceof ConfigurationSection) {
                current = sub;
            } else if (sub instanceof List) {
                current = sub;
            } else {
                if (value == null) {
                    return;
                }
                if (current instanceof ConfigurationSection sec) {
                    current = sec.createSection(node);
                } else {
                    @SuppressWarnings("unchecked")
                    List<Object> l = (List<Object>) current;
                    int index = Integer.parseInt(node);
                    while (l.size() <= index) {
                        l.add(null);
                    }
                    ConfigurationSection newSection = new MemorySection(this.root, createPath(this, path.substring(0, i1)));
                    l.set(index, newSection);
                    current = newSection;
                }
            }
        }

        String key = path.substring(i2);
        if (current instanceof MemorySection sec) {
            if (value == null) {
                sec.map.remove(key);
            } else {
                SectionPathData entry = sec.map.get(key);
                if (entry == null) {
                    sec.map.put(key, new SectionPathData(value));
                } else {
                    entry.setData(value);
                }
            }
        } else if (current instanceof ConfigurationSection sec) {
            sec.set(key, value);
        } else {
            @SuppressWarnings("unchecked")
            List<Object> l = (List<Object>) current;
            try {
                int index = Integer.parseInt(key);
                while (l.size() <= index) {
                    l.add(null);
                }
                l.set(index, value);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    @Override
    @Nullable
    public Object get(@NotNull String path) {
        return this.get(path, this.getDefault(path));
    }

    @Override
    @Contract("_, !null -> !null")
    @Nullable
    public Object get(@NotNull String path, @Nullable Object def) {
        Preconditions.checkNotNull(path, "Path cannot be null");

        if (path.isEmpty()) {
            return this;
        }

        Configuration root = this.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot access section without a root");
        }

        final char separator = root.options().pathSeparator();

        int i1 = -1, i2;
        Object current = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            current = this.getPart(current, path.substring(i2, i1));
            if (current == null) {
                return def;
            }
        }

        String key = path.substring(i2);
        Object result = this.getPart(current, key);
        return (result == null) ? def : result;
    }

    @Nullable
    private Object getPart(@Nullable Object current, @NotNull String node) {
        if (current instanceof MemorySection sec) {
            SectionPathData result = sec.map.get(node);
            return (result == null) ? sec.getDefault(node) : result.getData();
        } else if (current instanceof ConfigurationSection sec) {
            return sec.get(node, null);
        } else if (current instanceof List<?> list) {
            try {
                int index = Integer.parseInt(node);
                if (index >= 0 && index < list.size()) {
                    return list.get(index);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    @Override
    @NotNull
    public ConfigurationSection createSection(@NotNull String path) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(path), "Cannot create section at empty path");
        Configuration root = this.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot create section without a root");
        }

        final char separator = root.options().pathSeparator();

        int i1 = -1, i2;
        Object current = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            String node = path.substring(i2, i1);
            Object sub = this.getPart(current, node);
            if (sub instanceof ConfigurationSection) {
                current = sub;
            } else if (sub instanceof List) {
                current = sub;
            } else if (current instanceof ConfigurationSection sec) {
                current = sec.createSection(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Object> l = (List<Object>) current;
                int index = Integer.parseInt(node);
                while (l.size() <= index) {
                    l.add(null);
                }
                Object existing = l.get(index);
                if (existing instanceof ConfigurationSection) {
                    current = existing;
                } else {
                    ConfigurationSection newSection = new MemorySection(this.root, createPath(this, path.substring(0, i1)));
                    l.set(index, newSection);
                    current = newSection;
                }
            }
        }

        String key = path.substring(i2);
        if (current instanceof MemorySection sec) {
            ConfigurationSection result = new MemorySection(sec, key);
            sec.map.put(key, new SectionPathData(result));
            return result;
        } else if (current instanceof ConfigurationSection sec) {
            return sec.createSection(key);
        } else {
            @SuppressWarnings("unchecked")
            List<Object> l = (List<Object>) current;
            int index = Integer.parseInt(key);
            while (l.size() <= index) {
                l.add(null);
            }
            ConfigurationSection newSection = new MemorySection(this.root, createPath(this, path));
            l.set(index, newSection);
            return newSection;
        }
    }

    @Override
    @NotNull
    public ConfigurationSection createSection(@NotNull String path, @NotNull Map<?, ?> map) {
        ConfigurationSection section = this.createSection(path);

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                section.createSection(entry.getKey().toString(), (Map<?, ?>) entry.getValue());
            } else {
                section.set(entry.getKey().toString(), entry.getValue());
            }
        }

        return section;
    }


    @Override
    @Nullable
    public String getString(@NotNull String path) {
        Object def = this.getDefault(path);
        return this.getString(path, def != null ? def.toString() : null);
    }

    @Override
    @Contract("_, !null -> !null")
    @Nullable
    public String getString(@NotNull String path, @Nullable String def) {
        Object val = this.get(path, def);
        return (val != null) ? val.toString() : def;
    }

    @Override
    public boolean isString(@NotNull String path) {
        Object val = this.get(path);
        return val instanceof String;
    }

    @Override
    public int getInt(@NotNull String path) {
        Object def = this.getDefault(path);
        return this.getInt(path, (def instanceof Number) ? toInt(def) : 0);
    }

    @Override
    public int getInt(@NotNull String path, int def) {
        Object val = this.get(path, def);
        return (val instanceof Number) ? toInt(val) : def;
    }

    @Override
    public boolean isInt(@NotNull String path) {
        Object val = this.get(path);
        return val instanceof Integer;
    }

    @Override
    public boolean getBoolean(@NotNull String path) {
        Object def = this.getDefault(path);
        return this.getBoolean(path, (def instanceof Boolean) ? (Boolean) def : false);
    }

    @Override
    public boolean getBoolean(@NotNull String path, boolean def) {
        Object val = this.get(path, def);
        return (val instanceof Boolean) ? (Boolean) val : def;
    }

    @Override
    public boolean isBoolean(@NotNull String path) {
        Object val = this.get(path);
        return val instanceof Boolean;
    }

    @Override
    public double getDouble(@NotNull String path) {
        Object def = this.getDefault(path);
        return this.getDouble(path, (def instanceof Number) ? toDouble(def) : 0);
    }

    @Override
    public double getDouble(@NotNull String path, double def) {
        Object val = this.get(path, def);
        return (val instanceof Number) ? toDouble(val) : def;
    }

    @Override
    public boolean isDouble(@NotNull String path) {
        Object val = this.get(path);
        return val instanceof Double;
    }

    @Override
    public long getLong(@NotNull String path) {
        Object def = this.getDefault(path);
        return this.getLong(path, (def instanceof Number) ? toLong(def) : 0);
    }

    @Override
    public long getLong(@NotNull String path, long def) {
        Object val = this.get(path, def);
        return (val instanceof Number) ? toLong(val) : def;
    }

    @Override
    public boolean isLong(@NotNull String path) {
        Object val = this.get(path);
        return val instanceof Long;
    }


    @Override
    @Nullable
    public List<?> getList(@NotNull String path) {
        Object def = this.getDefault(path);
        return this.getList(path, (def instanceof List) ? (List<?>) def : null);
    }

    @Override
    @Contract("_, !null -> !null")
    @Nullable
    public List<?> getList(@NotNull String path, @Nullable List<?> def) {
        Object val = this.get(path, def);
        return (List<?>) ((val instanceof List) ? val : def);
    }

    @Override
    public boolean isList(@NotNull String path) {
        Object val = this.get(path);
        return val instanceof List;
    }

    @Override
    @NotNull
    public List<String> getStringList(@NotNull String path) {
        List<?> list = this.getList(path);

        if (list == null) {
            return new ArrayList<String>(0);
        }

        List<String> result = new ArrayList<String>();

        for (Object object : list) {
            if ((object instanceof String) || (this.isPrimitiveWrapper(object))) {
                result.add(String.valueOf(object));
            }
        }

        return result;
    }

    @Override
    @NotNull
    public List<Integer> getIntegerList(@NotNull String path) {
        List<?> list = this.getList(path);

        if (list == null) {
            return new ArrayList<Integer>(0);
        }

        List<Integer> result = new ArrayList<Integer>();

        for (Object object : list) {
            if (object instanceof Integer) {
                result.add((Integer) object);
            } else if (object instanceof String) {
                try {
                    result.add(Integer.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((int) ((Character) object).charValue());
            } else if (object instanceof Number) {
                result.add(((Number) object).intValue());
            }
        }

        return result;
    }

    @Override
    @NotNull
    public List<Boolean> getBooleanList(@NotNull String path) {
        List<?> list = this.getList(path);

        if (list == null) {
            return new ArrayList<Boolean>(0);
        }

        List<Boolean> result = new ArrayList<Boolean>();

        for (Object object : list) {
            if (object instanceof Boolean) {
                result.add((Boolean) object);
            } else if (object instanceof String) {
                if (Boolean.TRUE.toString().equals(object)) {
                    result.add(true);
                } else if (Boolean.FALSE.toString().equals(object)) {
                    result.add(false);
                }
            }
        }

        return result;
    }

    @Override
    @NotNull
    public List<Double> getDoubleList(@NotNull String path) {
        List<?> list = this.getList(path);

        if (list == null) {
            return new ArrayList<Double>(0);
        }

        List<Double> result = new ArrayList<Double>();

        for (Object object : list) {
            if (object instanceof Double) {
                result.add((Double) object);
            } else if (object instanceof String) {
                try {
                    result.add(Double.valueOf((String) object));
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((double) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).doubleValue());
            }
        }

        return result;
    }

    @Override
    @NotNull
    public List<Float> getFloatList(@NotNull String path) {
        List<?> list = this.getList(path);

        if (list == null) {
            return new ArrayList<Float>(0);
        }

        List<Float> result = new ArrayList<Float>();

        for (Object object : list) {
            if (object instanceof Float) {
                result.add((Float) object);
            } else if (object instanceof String) {
                try {
                    result.add(Float.valueOf((String) object));
                } catch (Exception ignored) {
                }
            } else if (object instanceof Character) {
                result.add((float) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).floatValue());
            }
        }

        return result;
    }

    @Override
    @NotNull
    public List<Long> getLongList(@NotNull String path) {
        List<?> list = this.getList(path);

        if (list == null) {
            return new ArrayList<Long>(0);
        }

        List<Long> result = new ArrayList<Long>();

        for (Object object : list) {
            if (object instanceof Long) {
                result.add((Long) object);
            } else if (object instanceof String) {
                try {
                    result.add(Long.valueOf((String) object));
                } catch (Exception ignored) {
                }
            } else if (object instanceof Character) {
                result.add((long) (Character) object);
            } else if (object instanceof Number) {
                result.add(((Number) object).longValue());
            }
        }

        return result;
    }

    @Override
    @NotNull
    public List<Byte> getByteList(@NotNull String path) {
        List<?> list = this.getList(path);

        if (list == null) {
            return new ArrayList<Byte>(0);
        }

        List<Byte> result = new ArrayList<Byte>();

        for (Object object : list) {
            if (object instanceof Byte) {
                result.add((Byte) object);
            } else if (object instanceof String) {
                try {
                    result.add(Byte.valueOf((String) object));
                } catch (Exception ignored) {
                }
            } else if (object instanceof Character) {
                result.add((byte) ((Character) object).charValue());
            } else if (object instanceof Number) {
                result.add(((Number) object).byteValue());
            }
        }

        return result;
    }

    @Override
    @NotNull
    public List<Character> getCharacterList(@NotNull String path) {
        List<?> list = this.getList(path);

        if (list == null) {
            return new ArrayList<>(0);
        }

        List<Character> result = new ArrayList<Character>();

        for (Object object : list) {
            if (object instanceof Character) {
                result.add((Character) object);
            } else if (object instanceof String str) {

                if (str.length() == 1) {
                    result.add(str.charAt(0));
                }
            } else if (object instanceof Number) {
                result.add((char) ((Number) object).intValue());
            }
        }

        return result;
    }

    @Override
    @NotNull
    public List<Short> getShortList(@NotNull String path) {
        List<?> list = this.getList(path);

        if (list == null) {
            return new ArrayList<Short>(0);
        }

        List<Short> result = new ArrayList<Short>();

        for (Object object : list) {
            if (object instanceof Short) {
                result.add((Short) object);
            } else if (object instanceof String) {
                try {
                    result.add(Short.valueOf((String) object));
                } catch (Exception ignored) {
                }
            } else if (object instanceof Character) {
                result.add((short) ((Character) object).charValue());
            } else if (object instanceof Number) {
                result.add(((Number) object).shortValue());
            }
        }

        return result;
    }

    @Override
    @NotNull
    public List<Map<?, ?>> getMapList(@NotNull String path) {
        List<?> list = this.getList(path);
        List<Map<?, ?>> result = new ArrayList<Map<?, ?>>();

        if (list == null) {
            return result;
        }

        for (Object object : list) {
            if (object instanceof Map) {
                result.add((Map<?, ?>) object);
            } else if (object instanceof ConfigurationSection section) {
                result.add(section.getValues(false));
            }
        }

        return result;
    }

    @Override
    @NotNull
    public List<ConfigurationSection> getSectionList(@NotNull String path) {
        List<?> list = this.getList(path);
        List<ConfigurationSection> result = new ArrayList<>();

        if (list == null) {
            return result;
        }

        int index = 0;
        for (Object object : list) {
            if (object instanceof ConfigurationSection) {
                result.add((ConfigurationSection) object);
            } else if (object instanceof Map map) {
                result.add(this.createSection(path + "." + index, map));
            }
            index++;
        }

        return result;
    }


    @Nullable
    @Override
    public <T extends Object> T getObject(@NotNull String path, @NotNull Class<T> clazz) {
        Preconditions.checkNotNull(clazz, "Class cannot be null");
        Object def = this.getDefault(path);
        return this.getObject(path, clazz, (clazz.isInstance(def)) ? clazz.cast(def) : null);
    }

    @Contract("_, _, !null -> !null")
    @Nullable
    @Override
    public <T extends Object> T getObject(@NotNull String path, @NotNull Class<T> clazz, @Nullable T def) {
        Preconditions.checkNotNull(clazz, "Class cannot be null");
        Object val = this.get(path, def);
        return (clazz.isInstance(val)) ? clazz.cast(val) : def;
    }

    @Nullable
    @Override
    public <T extends ConfigurationSerializable> T getSerializable(@NotNull String path, @NotNull Class<T> clazz) {
        return this.getObject(path, clazz);
    }

    @Contract("_, _, !null -> !null")
    @Nullable
    @Override
    public <T extends ConfigurationSerializable> T getSerializable(@NotNull String path, @NotNull Class<T> clazz, @Nullable T def) {
        return this.getObject(path, clazz, def);
    }

    @Override
    @Nullable
    public ConfigurationSection getConfigurationSection(@NotNull String path) {
        Object val = this.get(path, null);
        if (val != null) {
            return (val instanceof ConfigurationSection) ? (ConfigurationSection) val : null;
        }

        val = this.get(path, this.getDefault(path));
        return (val instanceof ConfigurationSection) ? this.createSection(path) : null;
    }

    @Override
    public boolean isConfigurationSection(@NotNull String path) {
        Object val = this.get(path);
        return val instanceof ConfigurationSection;
    }

    protected boolean isPrimitiveWrapper(@Nullable Object input) {
        return input instanceof Integer || input instanceof Boolean
                || input instanceof Character || input instanceof Byte
                || input instanceof Short || input instanceof Double
                || input instanceof Long || input instanceof Float;
    }

    @Nullable
    protected Object getDefault(@NotNull String path) {
        Preconditions.checkArgument(path != null, "Path cannot be null");

        Configuration root = this.getRoot();
        Configuration defaults = root == null ? null : root.getDefaults();
        return (defaults == null) ? null : defaults.get(createPath(this, path));
    }

    protected void mapChildrenKeys(@NotNull Set<String> output, @NotNull ConfigurationSection section, boolean deep) {
        if (section instanceof MemorySection sec) {

            for (Map.Entry<String, SectionPathData> entry : sec.map.entrySet()) {
                output.add(createPath(section, entry.getKey(), this));

                if ((deep) && (entry.getValue().getData() instanceof ConfigurationSection subsection)) {
                    this.mapChildrenKeys(output, subsection, deep);
                }
            }
        } else {
            Set<String> keys = section.getKeys(deep);

            for (String key : keys) {
                output.add(createPath(section, key, this));
            }
        }
    }

    protected void mapChildrenValues(@NotNull Map<String, Object> output, @NotNull ConfigurationSection section, boolean deep) {
        if (section instanceof MemorySection sec) {

            for (Map.Entry<String, SectionPathData> entry : sec.map.entrySet()) {


                String childPath = createPath(section, entry.getKey(), this);
                output.remove(childPath);
                output.put(childPath, entry.getValue().getData());

                if (entry.getValue().getData() instanceof ConfigurationSection) {
                    if (deep) {
                        this.mapChildrenValues(output, (ConfigurationSection) entry.getValue().getData(), deep);
                    }
                }
            }
        } else {
            Map<String, Object> values = section.getValues(deep);

            for (Map.Entry<String, Object> entry : values.entrySet()) {
                output.put(createPath(section, entry.getKey(), this), entry.getValue());
            }
        }
    }


    @NotNull
    public static String createPath(@NotNull ConfigurationSection section, @Nullable String key) {
        return createPath(section, key, (section == null) ? null : section.getRoot());
    }


    @NotNull
    public static String createPath(@NotNull ConfigurationSection section, @Nullable String key, @Nullable ConfigurationSection relativeTo) {
        Preconditions.checkNotNull(section, "Cannot create path without a section");
        Configuration root = section.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot create path without a root");
        }
        char separator = root.options().pathSeparator();

        StringBuilder builder = new StringBuilder();
        for (ConfigurationSection parent = section; (parent != null) && (parent != relativeTo); parent = parent.getParent()) {
            if (!builder.isEmpty()) {
                builder.insert(0, separator);
            }
            builder.insert(0, parent.getName());
        }

        if ((key != null) && (!key.isEmpty())) {
            if (!builder.isEmpty()) {
                builder.append(separator);
            }

            builder.append(key);
        }

        return builder.toString();
    }

    @Override
    @NotNull
    public List<String> getComments(@NotNull final String path) {
        final SectionPathData pathData = this.getSectionPathData(path);
        return pathData == null ? Collections.emptyList() : pathData.getComments();
    }

    @Override
    @NotNull
    public List<String> getInlineComments(@NotNull final String path) {
        final SectionPathData pathData = this.getSectionPathData(path);
        return pathData == null ? Collections.emptyList() : pathData.getInlineComments();
    }

    @Override
    public void setComments(@NotNull final String path, @Nullable final List<String> comments) {
        final SectionPathData pathData = this.getSectionPathData(path);
        if (pathData != null) {
            pathData.setComments(comments);
        }
    }

    @Override
    public void setInlineComments(@NotNull final String path, @Nullable final List<String> comments) {
        final SectionPathData pathData = this.getSectionPathData(path);
        if (pathData != null) {
            pathData.setInlineComments(comments);
        }
    }

    @Nullable
    private SectionPathData getSectionPathData(@NotNull String path) {
        Preconditions.checkNotNull(path, "Path cannot be null");

        Configuration root = this.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot access section without a root");
        }

        final char separator = root.options().pathSeparator();


        int i1 = -1, i2;
        ConfigurationSection section = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            section = section.getConfigurationSection(path.substring(i2, i1));
            if (section == null) {
                return null;
            }
        }

        String key = path.substring(i2);
        if (section == this) {
            return this.map.get(key);
        } else if (section instanceof MemorySection) {
            return ((MemorySection) section).getSectionPathData(key);
        }
        return null;
    }

    @Override
    public String toString() {
        Configuration root = this.getRoot();
        return this.getClass().getSimpleName() +
                "[path='" +
                this.getCurrentPath() +
                "', root='" +
                (root == null ? null : root.getClass().getSimpleName()) +
                "']";
    }
}
