package fr.robie.craftengineconverter.common.utils.yaml;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static org.bukkit.util.NumberConversions.*;


public class MemorySection implements fr.robie.craftengineconverter.api.yaml.ConfigurationSection {
    protected final Map<String, SectionPathData> map = new LinkedHashMap<String, SectionPathData>();
    private final fr.robie.craftengineconverter.api.yaml.Configuration root;
    private final fr.robie.craftengineconverter.api.yaml.ConfigurationSection parent;
    private final String path;
    private final String fullPath;


    protected MemorySection() {
        if (!(this instanceof fr.robie.craftengineconverter.api.yaml.Configuration)) {
            throw new IllegalStateException("Cannot construct a root MemorySection when not a Configuration");
        }

        this.path = "";
        this.fullPath = "";
        this.parent = null;
        this.root = (fr.robie.craftengineconverter.api.yaml.Configuration) this;
    }


    protected MemorySection(@NotNull fr.robie.craftengineconverter.api.yaml.ConfigurationSection parent, @NotNull String path) {
        Preconditions.checkArgument(parent != null, "Parent cannot be null");
        Preconditions.checkArgument(path != null, "Path cannot be null");

        this.path = path;
        this.parent = parent;
        this.root = parent.getRoot();

        Preconditions.checkArgument(this.root != null, "Path cannot be orphaned");

        this.fullPath = createPath(parent, path);
    }

    @Override
    @NotNull
    public Set<String> getKeys(boolean deep) {
        Set<String> result = new LinkedHashSet<String>();

        fr.robie.craftengineconverter.api.yaml.Configuration root = this.getRoot();
        if (root != null && root.options().copyDefaults()) {
            fr.robie.craftengineconverter.api.yaml.ConfigurationSection defaults = this.getDefaultSection();

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

        fr.robie.craftengineconverter.api.yaml.Configuration root = this.getRoot();
        if (root != null && root.options().copyDefaults()) {
            fr.robie.craftengineconverter.api.yaml.ConfigurationSection defaults = this.getDefaultSection();

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
        fr.robie.craftengineconverter.api.yaml.Configuration root = this.getRoot();
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
    public fr.robie.craftengineconverter.api.yaml.Configuration getRoot() {
        return this.root;
    }

    @Override
    @Nullable
    public fr.robie.craftengineconverter.api.yaml.ConfigurationSection getParent() {
        return this.parent;
    }

    @Override
    public void addDefault(@NotNull String path, @Nullable Object value) {
        Preconditions.checkArgument(path != null, "Path cannot be null");

        fr.robie.craftengineconverter.api.yaml.Configuration root = this.getRoot();
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
    public fr.robie.craftengineconverter.api.yaml.ConfigurationSection getDefaultSection() {
        fr.robie.craftengineconverter.api.yaml.Configuration root = this.getRoot();
        fr.robie.craftengineconverter.api.yaml.Configuration defaults = root == null ? null : root.getDefaults();

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

        fr.robie.craftengineconverter.api.yaml.Configuration root = this.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot use section without a root");
        }

        final char separator = root.options().pathSeparator();


        int i1 = -1, i2;
        fr.robie.craftengineconverter.api.yaml.ConfigurationSection section = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            String node = path.substring(i2, i1);
            fr.robie.craftengineconverter.api.yaml.ConfigurationSection subSection = section.getConfigurationSection(node);
            if (subSection == null) {
                if (value == null) {

                    return;
                }
                section = section.createSection(node);
            } else {
                section = subSection;
            }
        }

        String key = path.substring(i2);
        if (section == this) {
            if (value == null) {
                this.map.remove(key);
            } else {
                SectionPathData entry = this.map.get(key);
                if (entry == null) {
                    this.map.put(key, new SectionPathData(value));
                } else {
                    entry.setData(value);
                }
            }
        } else {
            section.set(key, value);
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
        Preconditions.checkArgument(path != null, "Path cannot be null");

        if (path.length() == 0) {
            return this;
        }

        fr.robie.craftengineconverter.api.yaml.Configuration root = this.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot access section without a root");
        }

        final char separator = root.options().pathSeparator();


        int i1 = -1, i2;
        fr.robie.craftengineconverter.api.yaml.ConfigurationSection section = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            final String currentPath = path.substring(i2, i1);
            if (!section.contains(currentPath, true)) {
                return def;
            }
            section = section.getConfigurationSection(currentPath);
            if (section == null) {
                return def;
            }
        }

        String key = path.substring(i2);
        if (section == this) {
            SectionPathData result = this.map.get(key);
            return (result == null) ? def : result.getData();
        }
        return section.get(key, def);
    }

    @Override
    @NotNull
    public fr.robie.craftengineconverter.api.yaml.ConfigurationSection createSection(@NotNull String path) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(path), "Cannot create section at empty path");
        fr.robie.craftengineconverter.api.yaml.Configuration root = this.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot create section without a root");
        }

        final char separator = root.options().pathSeparator();


        int i1 = -1, i2;
        fr.robie.craftengineconverter.api.yaml.ConfigurationSection section = this;
        while ((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            String node = path.substring(i2, i1);
            fr.robie.craftengineconverter.api.yaml.ConfigurationSection subSection = section.getConfigurationSection(node);
            if (subSection == null) {
                section = section.createSection(node);
            } else {
                section = subSection;
            }
        }

        String key = path.substring(i2);
        if (section == this) {
            fr.robie.craftengineconverter.api.yaml.ConfigurationSection result = new MemorySection(this, key);
            this.map.put(key, new SectionPathData(result));
            return result;
        }
        return section.createSection(key);
    }

    @Override
    @NotNull
    public fr.robie.craftengineconverter.api.yaml.ConfigurationSection createSection(@NotNull String path, @NotNull Map<?, ?> map) {
        fr.robie.craftengineconverter.api.yaml.ConfigurationSection section = this.createSection(path);

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
                result.add((double) ((Character) object).charValue());
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
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((float) ((Character) object).charValue());
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
                } catch (Exception ex) {
                }
            } else if (object instanceof Character) {
                result.add((long) ((Character) object).charValue());
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
                } catch (Exception ex) {
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
            return new ArrayList<Character>(0);
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
                } catch (Exception ex) {
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
            }
        }

        return result;
    }


    @Nullable
    @Override
    public <T extends Object> T getObject(@NotNull String path, @NotNull Class<T> clazz) {
        Preconditions.checkArgument(clazz != null, "Class cannot be null");
        Object def = this.getDefault(path);
        return this.getObject(path, clazz, (clazz.isInstance(def)) ? clazz.cast(def) : null);
    }

    @Contract("_, _, !null -> !null")
    @Nullable
    @Override
    public <T extends Object> T getObject(@NotNull String path, @NotNull Class<T> clazz, @Nullable T def) {
        Preconditions.checkArgument(clazz != null, "Class cannot be null");
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
    public fr.robie.craftengineconverter.api.yaml.ConfigurationSection getConfigurationSection(@NotNull String path) {
        Object val = this.get(path, null);
        if (val != null) {
            return (val instanceof fr.robie.craftengineconverter.api.yaml.ConfigurationSection) ? (fr.robie.craftengineconverter.api.yaml.ConfigurationSection) val : null;
        }

        val = this.get(path, this.getDefault(path));
        return (val instanceof fr.robie.craftengineconverter.api.yaml.ConfigurationSection) ? this.createSection(path) : null;
    }

    @Override
    public boolean isConfigurationSection(@NotNull String path) {
        Object val = this.get(path);
        return val instanceof fr.robie.craftengineconverter.api.yaml.ConfigurationSection;
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

        fr.robie.craftengineconverter.api.yaml.Configuration root = this.getRoot();
        fr.robie.craftengineconverter.api.yaml.Configuration defaults = root == null ? null : root.getDefaults();
        return (defaults == null) ? null : defaults.get(createPath(this, path));
    }

    protected void mapChildrenKeys(@NotNull Set<String> output, @NotNull fr.robie.craftengineconverter.api.yaml.ConfigurationSection section, boolean deep) {
        if (section instanceof MemorySection sec) {

            for (Map.Entry<String, SectionPathData> entry : sec.map.entrySet()) {
                output.add(createPath(section, entry.getKey(), this));

                if ((deep) && (entry.getValue().getData() instanceof fr.robie.craftengineconverter.api.yaml.ConfigurationSection subsection)) {
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

    protected void mapChildrenValues(@NotNull Map<String, Object> output, @NotNull fr.robie.craftengineconverter.api.yaml.ConfigurationSection section, boolean deep) {
        if (section instanceof MemorySection sec) {

            for (Map.Entry<String, SectionPathData> entry : sec.map.entrySet()) {


                String childPath = createPath(section, entry.getKey(), this);
                output.remove(childPath);
                output.put(childPath, entry.getValue().getData());

                if (entry.getValue().getData() instanceof fr.robie.craftengineconverter.api.yaml.ConfigurationSection) {
                    if (deep) {
                        this.mapChildrenValues(output, (fr.robie.craftengineconverter.api.yaml.ConfigurationSection) entry.getValue().getData(), deep);
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
    public static String createPath(@NotNull fr.robie.craftengineconverter.api.yaml.ConfigurationSection section, @Nullable String key) {
        return createPath(section, key, (section == null) ? null : section.getRoot());
    }


    @NotNull
    public static String createPath(@NotNull fr.robie.craftengineconverter.api.yaml.ConfigurationSection section, @Nullable String key, @Nullable fr.robie.craftengineconverter.api.yaml.ConfigurationSection relativeTo) {
        Preconditions.checkArgument(section != null, "Cannot create path without a section");
        fr.robie.craftengineconverter.api.yaml.Configuration root = section.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot create path without a root");
        }
        char separator = root.options().pathSeparator();

        StringBuilder builder = new StringBuilder();
        for (fr.robie.craftengineconverter.api.yaml.ConfigurationSection parent = section; (parent != null) && (parent != relativeTo); parent = parent.getParent()) {
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
        Preconditions.checkArgument(path != null, "Path cannot be null");

        fr.robie.craftengineconverter.api.yaml.Configuration root = this.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot access section without a root");
        }

        final char separator = root.options().pathSeparator();


        int i1 = -1, i2;
        fr.robie.craftengineconverter.api.yaml.ConfigurationSection section = this;
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
        fr.robie.craftengineconverter.api.yaml.Configuration root = this.getRoot();
        return this.getClass().getSimpleName() +
                "[path='" +
                this.getCurrentPath() +
                "', root='" +
                (root == null ? null : root.getClass().getSimpleName()) +
                "']";
    }
}
