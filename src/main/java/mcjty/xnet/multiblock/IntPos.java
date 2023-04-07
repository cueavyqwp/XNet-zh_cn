package mcjty.xnet.multiblock;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

/**
 * A local position in a chunk can be represented with a single int like done
 * in this class.
 */
public class IntPos {

    private final int pos;

    public IntPos(int pos) {
        this.pos = pos;
    }

    public IntPos(BlockPos pos) {
        this.pos = toInt(pos);
    }

    public int getPos() {
        return pos;
    }

    public int[] getSidePositions() {
        return new int[] { posDown(), posUp(), posEast(), posWest(), posSouth(), posNorth() };
    }

    public boolean isBorder() {
        return getX() == 0 || getX() == 15 || getZ() == 0 || getZ() == 15;
    }

    public boolean isBorder(EnumFacing facing) {
        switch (facing) {
            case DOWN:
            case UP:
                return false;
            case NORTH:
                return getZ() == 0;
            case SOUTH:
                return getZ() == 15;
            case WEST:
                return getX() == 0;
            case EAST:
                return getX() == 15;
        }
        return false;
    }

    public IntPos otherSide(EnumFacing facing) {
        switch (facing) {
            case DOWN:
            case UP:
                return this;
            case NORTH:
                return new IntPos(pos + (15 << 12));
            case SOUTH:
                return new IntPos(pos - (15 << 12));
            case WEST:
                return new IntPos(pos + 15);
            case EAST:
                return new IntPos(pos - 15);
        }
        return this;
    }

    public int getX() {
        return pos & 0xf;
    }

    public int getY() {
        return (pos >> 4) & 0xff;
    }

    public int getZ() {
        return (pos >> 12) & 0xf;
    }

    public int posSouth() {
        if (getZ() >= 15) {
            return -1;
        }
        return pos + (1<<12);
    }

    public int posNorth() {
        if (getZ() < 1) {
            return -1;
        }
        return pos - (1<<12);
    }

    public int posEast() {
        if (getX() >= 15) {
            return -1;
        }
        return pos+1;
    }

    public int posWest() {
        if (getX() < 1) {
            return -1;
        }
        return pos-1;
    }

    public int posUp() {
        if (getY() >= 255) {
            return -1;
        }
        return pos + (1<<4);
    }

    public int posDown() {
        if (getY() < 1) {
            return -1;
        }
        return pos - (1<<4);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntPos intPos = (IntPos) o;

        if (pos != intPos.pos) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return pos;
    }

    private static int toInt(BlockPos pos) {
        int dx = pos.getX() & 0xf;
        int dy = pos.getY();
        int dz = pos.getZ() & 0xf;
        return dz << 12 | dy << 4 | dx;
    }

    public BlockPos toBlockPos(ChunkPos cpos) {
        int dx = getX();
        int dy = getY();
        int dz = getZ();
        return cpos.getBlock(dx, dy, dz);
    }
}
