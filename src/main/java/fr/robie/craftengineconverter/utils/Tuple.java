package fr.robie.craftengineconverter.utils;

public class Tuple<T> {
    public final T first;
    public final T second;

    public Tuple(T first, T second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public T getSecond() {
        return second;
    }

    public static <T> Tuple<T> of(T first, T second) {
        return new Tuple<>(first, second);
    }
}
