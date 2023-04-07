package mcjty.xnet.api.channels;

import mcjty.xnet.api.keys.NetworkId;
import mcjty.xnet.api.net.IWorldBlob;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Implement this interface if you want to implement your own consumer provider.
 * The controller will use this to allow you to add additional consumers (connectors)
 * Register this with IXNet.
 */
public interface IConsumerProvider {

    /**
     * The controller will call this whenever it needs to update its list of consumers. Note that
     * the controller will cache this so make sure to call IWorldBlob.markNetworkDirty() on the
     * given network
     */
    @Nonnull
    Set<BlockPos> getConsumers(World world, IWorldBlob worldBlob, NetworkId networkId);
}
