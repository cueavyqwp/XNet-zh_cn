package mcjty.xnet.logic;

import mcjty.xnet.blocks.cables.ConnectorBlock;
import mcjty.xnet.blocks.generic.CableColor;
import mcjty.xnet.blocks.generic.GenericCableBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ConnectorIterator implements Iterator<BlockPos> {

    @Nonnull private final World world;
    @Nonnull private final BlockPos pos;
    private final boolean routing;

    private int facingIdx = 0;
    private BlockPos foundPos = null;

    Stream<BlockPos> stream() {
        return StreamSupport.stream(Spliterators.spliterator(this, EnumFacing.VALUES.length, Spliterator.ORDERED), false);
    }

    ConnectorIterator(@Nonnull World world, @Nonnull BlockPos pos, boolean routing) {
        this.world = world;
        this.pos = pos;
        this.routing = routing;
        findNext();
    }

    private void findNext() {
        foundPos = null;
        while (facingIdx != -1) {
            BlockPos connectorPos = pos.offset(EnumFacing.VALUES[facingIdx]);
            facingIdx++;
            if (facingIdx >= EnumFacing.VALUES.length) {
                facingIdx = -1;
            }
            IBlockState state = world.getBlockState(connectorPos);
            if (state.getBlock() instanceof ConnectorBlock) {
                CableColor color = state.getValue(GenericCableBlock.COLOR);
                if ((color == CableColor.ROUTING) == routing) {
                    foundPos = connectorPos;
                    return;
                }
            }
        }
    }

    @Override
    public boolean hasNext() {
        return foundPos != null;
    }

    @Override
    public BlockPos next() {
        BlockPos f = foundPos;
        findNext();
        return f;
    }
}

