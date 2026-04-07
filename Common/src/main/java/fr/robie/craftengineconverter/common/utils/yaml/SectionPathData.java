package fr.robie.craftengineconverter.common.utils.yaml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

final class SectionPathData {

    private Object data;
    private List<String> comments;
    private List<String> inlineComments;

    public SectionPathData(@Nullable Object data) {
        this.data = data;
        this.comments = Collections.emptyList();
        this.inlineComments = Collections.emptyList();
    }

    @Nullable
    public Object getData() {
        return this.data;
    }

    public void setData(@Nullable final Object data) {
        this.data = data;
    }


    @NotNull
    public List<String> getComments() {
        return this.comments;
    }


    public void setComments(@Nullable final List<String> comments) {
        this.comments = (comments == null) ? Collections.emptyList() : Collections.unmodifiableList(comments);
    }


    @NotNull
    public List<String> getInlineComments() {
        return this.inlineComments;
    }


    public void setInlineComments(@Nullable final List<String> inlineComments) {
        this.inlineComments = (inlineComments == null) ? Collections.emptyList() : Collections.unmodifiableList(inlineComments);
    }
}
