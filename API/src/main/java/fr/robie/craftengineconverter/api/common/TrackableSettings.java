package fr.robie.craftengineconverter.api.common;

public abstract class TrackableSettings {
    private int constructionHashCode = Integer.MIN_VALUE;
    private boolean initialized = false;

    protected void markInitialized() {
        this.constructionHashCode = this.computeHashCode();
        this.initialized = true;
    }

    public boolean isUpdated() {
        if (!this.initialized) {
            return false;
        }
        return this.constructionHashCode != this.computeHashCode();
    }

    protected abstract int computeHashCode();
}