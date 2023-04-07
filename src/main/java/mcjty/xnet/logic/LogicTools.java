package mcjty.xnet.logic;

import mcjty.lib.varia.WorldTools;
import mcjty.xnet.api.keys.ConsumerId;
import mcjty.xnet.api.keys.NetworkId;
import mcjty.xnet.api.keys.SidedConsumer;
import mcjty.xnet.blocks.cables.ConnectorBlock;
import mcjty.xnet.blocks.controller.TileEntityController;
import mcjty.xnet.blocks.router.TileEntityRouter;
import mcjty.xnet.blocks.wireless.TileEntityWirelessRouter;
import mcjty.xnet.multiblock.WorldBlob;
import mcjty.xnet.multiblock.XNetBlobData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class LogicTools {

    // Find the controller attached to this connector.
    @Nullable
    public static TileEntityController getControllerForConnector(@Nonnull World world, @Nonnull BlockPos connectorPos) {
        BlockPos controllerPos = getControllerPosForConnector(world, connectorPos);
        if (controllerPos == null) {
            return null;
        }
        if (!WorldTools.chunkLoaded(world, controllerPos)) {
            return null;
        }
        TileEntity te = world.getTileEntity(controllerPos);
        if (te instanceof TileEntityController) {
            return (TileEntityController) te;
        } else {
            return null;
        }
    }

    @Nullable
    public static BlockPos getControllerPosForConnector(@Nonnull World world, @Nonnull BlockPos connectorPos) {
        WorldBlob worldBlob = XNetBlobData.getBlobData(world).getWorldBlob(world);
        NetworkId networkId = worldBlob.getNetworkAt(connectorPos);
        if (networkId == null) {
            return null;
        }
        return worldBlob.getProviderPosition(networkId);
    }

    // All consumers for a given network
    public static Stream<BlockPos> consumers(@Nonnull World world, @Nonnull NetworkId networkId) {
        WorldBlob worldBlob = XNetBlobData.getBlobData(world).getWorldBlob(world);
        return worldBlob.getConsumers(networkId).stream();
    }

    // All normal connectors for a given position
    public static Stream<BlockPos> connectors(@Nonnull World world, @Nonnull BlockPos pos) {
        return new ConnectorIterator(world, pos, false).stream();
    }

    // All routing connectors for a given position
    public static Stream<BlockPos> routingConnectors(@Nonnull World world, @Nonnull BlockPos pos) {
        return new ConnectorIterator(world, pos, true).stream();
    }

    // All routers from a given position
    public static Stream<TileEntityRouter> routers(@Nonnull World world, @Nonnull BlockPos pos) {
        return new RouterIterator<>(world, pos, TileEntityRouter.class).stream();
    }

    // All wireless routers from a given position
    public static Stream<TileEntityWirelessRouter> wirelessRouters(@Nonnull World world, @Nonnull BlockPos pos) {
        return new RouterIterator<>(world, pos, TileEntityWirelessRouter.class).stream();
    }

    // Return all connected blocks that have an actual connector defined in a channel
    public static Stream<BlockPos> connectedBlocks(@Nonnull World world, @Nonnull NetworkId networkId, @Nonnull Set<SidedConsumer> consumers) {
        WorldBlob worldBlob = XNetBlobData.getBlobData(world).getWorldBlob(world);
        return consumers.stream()
                .map(sidedConsumer -> {
                    BlockPos consumerPos = findConsumerPosition(networkId, worldBlob, sidedConsumer.getConsumerId());
                    if (consumerPos != null) {
                        return consumerPos.offset(sidedConsumer.getSide());
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull);
    }

    // Return all controllers connected to a network
    public static Stream<TileEntityController> controllers(@Nonnull World world, @Nonnull NetworkId networkId) {
        return connectedBlocks(world, networkId)
                .filter(pos -> world.getTileEntity(pos) instanceof TileEntityController)
                .map(pos -> (TileEntityController) world.getTileEntity(pos));
    }

    // Return all routers connected to a network
    public static Stream<TileEntityRouter> routers(@Nonnull World world, @Nonnull NetworkId networkId) {
        return connectedBlocks(world, networkId)
                .filter(pos -> world.getTileEntity(pos) instanceof TileEntityRouter)
                .map(pos -> (TileEntityRouter) world.getTileEntity(pos));
    }

    // Return all potential connected blocks (with or an actual connector defined in the channel)
    public static Stream<BlockPos> connectedBlocks(@Nonnull World world, @Nonnull NetworkId networkId) {
        return consumers(world, networkId)
                .flatMap(blockPos -> Arrays.stream(EnumFacing.VALUES)
                        .filter(facing -> ConnectorBlock.isConnectable(world, blockPos, facing))
                        .map(blockPos::offset));
    }

    @Nullable
    public static BlockPos findConsumerPosition(@Nonnull NetworkId networkId, @Nonnull WorldBlob worldBlob, @Nonnull ConsumerId consumerId) {
        Set<BlockPos> consumers = worldBlob.getConsumers(networkId);
        for (BlockPos pos : consumers) {
            ConsumerId c = worldBlob.getConsumerAt(pos);
            if (consumerId.equals(c)) {
                return pos;
            }
        }
        return null;
    }


}
