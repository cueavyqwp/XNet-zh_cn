package mcjty.xnet.logic;

import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.xnet.blocks.router.TileEntityRouter;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class RouterIterator<T extends GenericTileEntity> implements Iterator<T> {

    @Nonnull private final World world;
    @Nonnull private final BlockPos pos;
    @Nonnull private final Class<T> clazz;

    private int facingIdx = 0;
    private T foundRouter = null;

    Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliterator(this, EnumFacing.VALUES.length, Spliterator.ORDERED), false);
    }

    RouterIterator(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull Class<T> clazz) {
        this.world = world;
        this.pos = pos;
        this.clazz = clazz;
        findNext();
    }

    private void findNext() {
        foundRouter = null;
        while (facingIdx != -1) {
            BlockPos routerPos = pos.offset(EnumFacing.VALUES[facingIdx]);
            facingIdx++;
            if (facingIdx >= EnumFacing.VALUES.length) {
                facingIdx = -1;
            }
            TileEntity te = world.getTileEntity(routerPos);
            if (clazz.isInstance(te)) {
                foundRouter = (T) te;
                return;
            }
        }
    }

    @Override
    public boolean hasNext() {
        return foundRouter != null;
    }

    @Override
    public T next() {
        T f = foundRouter;
        findNext();
        return f;
    }
}

