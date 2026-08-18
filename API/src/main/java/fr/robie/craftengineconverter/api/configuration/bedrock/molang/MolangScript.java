package fr.robie.craftengineconverter.api.configuration.bedrock.molang;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of Molang statements, as {@code scripts.pre_animation} and {@code scripts.parent_setup} take them.
 * <p>
 * {@link Molang} builds expressions, and an expression is not a statement: these slots hold assignments, each
 * terminated by a semicolon, evaluated in order. Bedrock accepts them as a JSON array of strings, one statement per
 * entry — several per entry also works, but one apiece is what vanilla writes and what a diff can read.
 * <p>
 * Only assignment to a {@code variable.} is offered. {@code temp.} exists but does not survive to the render
 * controller, which is the whole point of setting anything here.
 */
public final class MolangScript {

    private final List<String> statements = new ArrayList<>();

    /**
     * Appends {@code variable.<name> = <value>;}.
     *
     * @param name the bare variable name, without the {@code variable.} prefix
     */
    @NotNull
    public MolangScript set(@NotNull String name, @NotNull Molang value) {
        this.statements.add(Molang.variable(name) + " = " + value + ";");
        return this;
    }

    /** The statements, in the order they were added, ready to be handed to an attachable's scripts block. */
    @NotNull
    public List<String> statements() {
        return List.copyOf(this.statements);
    }

    public boolean isEmpty() {
        return this.statements.isEmpty();
    }
}
