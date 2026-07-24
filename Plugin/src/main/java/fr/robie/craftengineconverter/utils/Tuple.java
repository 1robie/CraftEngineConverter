package fr.robie.craftengineconverter.utils;

public record Tuple<T>(T first, T second) {

    public static <T> Tuple<T> of(T first, T second) {
        return new Tuple<>(first, second);
    }
}
