package fr.robie.craftengineconverter.api.configuration;

import com.google.gson.reflect.TypeToken;
import fr.robie.craftengineconverter.api.utils.ConfigurationDeserializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * One configuration setting: which file holds it, where in that file, what type it is, and what it defaults to.
 * <p>
 * A class rather than the enum this replaces, and the reason is the type parameter. An enum constant cannot carry
 * one, so the enum had to declare its type as a {@code TypeToken<?>} and {@code Configuration.get}
 * had to return {@code Object} through an unchecked cast — so every call site had to restate the type, some as a
 * cast and some as an explicit type argument, and a wrong one was caught only when a server ran it.
 * {@code Key<T>} makes {@code get} return {@code T}, and those 43 restatements are gone.
 * <p>
 * Keys register themselves as they are constructed, so {@link Configuration} can ask for the ones belonging to a
 * file rather than iterating {@code values()}. That means a key only exists once {@link Keys} has been loaded;
 * every caller reaches keys through that class, so the class initialises before anything can ask.
 */
public final class Key<T> {

    // Insertion-ordered, so a file's keys are written in the order they were declared and a generated file stays
    // diffable against the last one.
    private static final List<Key<?>> REGISTRY = new ArrayList<>();

    private final ConfigFile file;
    private final String path;
    private final TypeToken<T> type;
    private final Class<?> rawType;
    private final Supplier<T> defaultValueSupplier;
    private final ConfigurationDeserializer<T> deserializer;

    private Key(@NotNull ConfigFile file, @NotNull String path, @NotNull TypeToken<T> type,
                @NotNull Supplier<T> defaultValueSupplier, @NotNull ConfigurationDeserializer<T> deserializer) {
        this.file = file;
        this.path = path;
        this.type = type;
        this.rawType = type.getRawType();
        this.defaultValueSupplier = defaultValueSupplier;
        this.deserializer = deserializer;
        REGISTRY.add(this);
    }

    /** A key whose value needs no conversion beyond the standard coercion for its type. */
    @NotNull
    public static <T> Key<T> of(@NotNull ConfigFile file, @NotNull String path, @NotNull TypeToken<T> type,
                                @NotNull Supplier<T> defaultValueSupplier) {
        return new Key<>(file, path, type, defaultValueSupplier, buildDeserializer(type));
    }

    /** A key that reads its own shape out of the file — a section, a map, a value with several spellings. */
    @NotNull
    public static <T> Key<T> of(@NotNull ConfigFile file, @NotNull String path, @NotNull TypeToken<T> type,
                                @NotNull Supplier<T> defaultValueSupplier,
                                @NotNull ConfigurationDeserializer<T> deserializer) {
        return new Key<>(file, path, type, defaultValueSupplier, deserializer);
    }

    // The three shorthands that cover most of the table, so a plain setting reads as one line.
    @NotNull
    public static Key<Boolean> bool(@NotNull ConfigFile file, @NotNull String path, boolean defaultValue) {
        return of(file, path, new TypeToken<>() {}, () -> defaultValue);
    }

    @NotNull
    public static Key<Integer> integer(@NotNull ConfigFile file, @NotNull String path, int defaultValue) {
        return of(file, path, new TypeToken<>() {}, () -> defaultValue);
    }

    @NotNull
    public static Key<String> string(@NotNull ConfigFile file, @NotNull String path, @NotNull String defaultValue) {
        return of(file, path, new TypeToken<>() {}, () -> defaultValue);
    }

    /** Every key that has been declared, in declaration order. */
    @NotNull
    public static List<Key<?>> all() {
        ensureDeclarationsLoaded();
        return Collections.unmodifiableList(REGISTRY);
    }

    /** The keys belonging to one file, in declaration order. */
    @NotNull
    public static List<Key<?>> in(@NotNull ConfigFile file) {
        ensureDeclarationsLoaded();
        List<Key<?>> keys = new ArrayList<>();
        for (Key<?> key : REGISTRY) {
            if (key.file == file) keys.add(key);
        }
        return keys;
    }

    private static boolean declarationsLoaded;

    /**
     * Loads {@link Keys} if it has not been loaded, because a key does not exist until its declaration has run.
     * <p>
     * The registry fills as a side effect of constructing each constant in {@code Keys}, so asking for the keys
     * before that class has initialised would quietly return an empty list — a configuration that loads nothing and
     * reports no error. Forcing it here means no caller has to know that, which is the whole point: the previous
     * shape required every caller to remember to prime the class first, and a forgotten call would have failed
     * silently.
     * <p>
     * The flag is set before the call, so a re-entrant path recurses no further than once.
     */
    private static void ensureDeclarationsLoaded() {
        if (declarationsLoaded) return;
        declarationsLoaded = true;
        Keys.ensureLoaded();
    }

    @NotNull
    public ConfigFile file() {
        return this.file;
    }

    @NotNull
    public String path() {
        return this.path;
    }

    /**
     * Where this setting lived in the old single {@code config.yml}, when that differs from where it lives now.
     * <p>
     * Needed because moving a key into its converter's own file makes its old prefix redundant: {@code nexo.yml}
     * should hold {@code enable-hook} at its root, not {@code nexo.enable-hook}. That reads better but stops the
     * migration being a straight path lookup, so the old spelling is recorded here instead of being derivable.
     *
     * @return the old path, or the current one when the move did not rename it
     */
    @NotNull
    public String legacyPath() {
        return this.legacyPath == null ? this.path : this.legacyPath;
    }

    /** Declares the old {@code config.yml} path, for {@link #legacyPath()}. Chained at declaration in {@link Keys}. */
    @NotNull
    public Key<T> legacy(@NotNull String oldPath) {
        this.legacyPath = oldPath;
        return this;
    }

    private String legacyPath;

    /**
     * The comment written above this setting when it is added to a file that lacks it.
     * <p>
     * The YAML library keeps the comments on a key that already exists, but a key it has never seen arrives bare and
     * appended to the end of its section — so a setting introduced by an upgrade would show up in a server's file as
     * an unexplained line. This is the text that stops that.
     *
     * @return the lines, without their leading {@code #}, or empty when the key carries none
     */
    @NotNull
    public List<String> doc() {
        return this.doc == null ? List.of() : this.doc;
    }

    /** Declares the comment for {@link #doc()}. Chained at declaration in {@link Keys}. */
    @NotNull
    public Key<T> doc(@NotNull String... lines) {
        this.doc = List.of(lines);
        return this;
    }

    private List<String> doc;

    @NotNull
    public TypeToken<T> type() {
        return this.type;
    }

    @NotNull
    public Class<?> rawType() {
        return this.rawType;
    }

    @NotNull
    public T defaultValue() {
        return this.defaultValueSupplier.get();
    }

    @NotNull
    public T deserialize(@NotNull Object rawValue) {
        return this.deserializer.deserialize(rawValue, this.defaultValueSupplier);
    }

    @Override
    public String toString() {
        return this.file.fileName() + ":" + this.path;
    }

    /**
     * The coercion a value gets on the way out of YAML, carried over unchanged from the enum this replaces.
     * <p>
     * It exists because the YAML library hands back whatever the file happened to contain — an {@code Integer} for a
     * long, a {@code String} for an enum — and a key's declared type is the authority on what that should become.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ConfigurationDeserializer<T> buildDeserializer(@NotNull TypeToken<T> type) {
        Class<?> raw = type.getRawType();

        if (raw == Boolean.class) return (o, d) -> (T) o;
        if (raw == Integer.class)
            return (o, d) -> (T) (Integer) (o instanceof Number n ? n.intValue() : Integer.parseInt(o.toString()));
        if (raw == Long.class)
            return (o, d) -> (T) (Long) (o instanceof Number n ? n.longValue() : Long.parseLong(o.toString()));
        if (raw == String.class) return (o, d) -> (T) o.toString();
        if (raw == List.class) return (o, d) -> (T) (o instanceof List<?> l ? new ArrayList<>(l) : new ArrayList<>());

        if (raw.isEnum()) {
            return (o, d) -> {
                try {
                    return (T) Enum.valueOf((Class<Enum>) raw, o.toString().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    return d.get();
                }
            };
        }

        return (o, d) -> (T) o;
    }
}
