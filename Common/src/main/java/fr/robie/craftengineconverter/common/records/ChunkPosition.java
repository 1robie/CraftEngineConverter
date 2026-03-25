package fr.robie.craftengineconverter.common.records;

public record ChunkPosition(String worldName, int x, int z) {
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        ChunkPosition that = (ChunkPosition) o;
        return this.x == that.x && this.z == that.z && this.worldName.equals(that.worldName);
    }

    @Override
    public int hashCode() {
        return this.worldName.hashCode() * 31 * 31 + this.x * 31 + this.z;
    }
}