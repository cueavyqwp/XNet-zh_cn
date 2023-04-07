package mcjty.xnet.api.channels;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IChannelType {

    String getID();

    String getName();

    /**
     * Return true if the block at that specific side (can be null) supports this channel type
     */
    boolean supportsBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nullable EnumFacing side);

    /**
     * The 'advanced' parameter is no longer used and will always be 'false'. Don't depend on this
     */
    @Nonnull
    IConnectorSettings createConnector(@Nonnull EnumFacing side);

    @Nonnull
    IChannelSettings createChannel();
}
