package mcjty.xnet.blocks.router;

import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

class LocalChannelId {
    @Nonnull
    private final BlockPos controllerPos;
    private final int index;    // Channel index

    public LocalChannelId(@Nonnull BlockPos controllerPos, int index) {
        this.controllerPos = controllerPos;
        this.index = index;
    }

    @Nonnull
    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocalChannelId that = (LocalChannelId) o;

        if (index != that.index) return false;
        return controllerPos.equals(that.controllerPos);

    }

    @Override
    public int hashCode() {
        int result = controllerPos.hashCode();
        result = 31 * result + index;
        return result;
    }
}
