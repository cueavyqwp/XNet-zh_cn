package mcjty.xnet.api.keys;

import mcjty.lib.varia.BlockPosTools;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;

public class SidedPos implements Comparable<SidedPos> {
    private final BlockPos pos;
    private final EnumFacing side;

    /**
     * A position of a connected block and the side relative
     * from this block where the connection is. Basically
     * pos.offset(side) will be the consumer/connector
     */
    public SidedPos(@Nonnull BlockPos pos, @Nonnull EnumFacing side) {
        this.pos = pos;
        this.side = side;
    }

    @Nonnull
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Get the side relative to this position for the connector.
     */
    @Nonnull
    public EnumFacing getSide() {
        return side;
    }

    @Override
    public String toString() {
        return "SidedPos{" + BlockPosTools.toString(pos) + "/" + side.getName() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SidedPos sidedPos = (SidedPos) o;

        return side == sidedPos.side && pos.equals(sidedPos.pos);
    }

    @Override
    public int hashCode() {
        return 31 * pos.hashCode() + side.hashCode();
    }

    @Override
    public int compareTo(SidedPos o) {
        int result = pos.compareTo(o.pos);
        if(result == 0) result = side.compareTo(o.side);
        return result;
    }
}
