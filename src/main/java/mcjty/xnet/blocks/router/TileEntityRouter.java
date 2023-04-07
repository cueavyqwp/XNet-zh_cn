package mcjty.xnet.blocks.router;

import mcjty.lib.tileentity.GenericTileEntity;
import mcjty.lib.typed.Key;
import mcjty.lib.typed.Type;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.BlockPosTools;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TextStyleClass;
import mcjty.xnet.api.channels.IChannelType;
import mcjty.xnet.api.channels.IConnectorSettings;
import mcjty.xnet.api.keys.NetworkId;
import mcjty.xnet.api.keys.SidedConsumer;
import mcjty.xnet.blocks.generic.CableColor;
import mcjty.xnet.clientinfo.ControllerChannelClientInfo;
import mcjty.xnet.config.ConfigSetup;
import mcjty.xnet.logic.ChannelInfo;
import mcjty.xnet.logic.LogicTools;
import mcjty.xnet.multiblock.*;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.Optional;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static mcjty.xnet.logic.ChannelInfo.MAX_CHANNELS;

public final class TileEntityRouter extends GenericTileEntity {

    public static final String CMD_UPDATENAME = "router.updateName";
    public static final Key<BlockPos> PARAM_POS = new Key<>("pos", Type.BLOCKPOS);
    public static final Key<Integer> PARAM_CHANNEL = new Key<>("channel", Type.INTEGER);
    public static final Key<String> PARAM_NAME = new Key<>("name", Type.STRING);

    public static final String CMD_GETCHANNELS = "getChannelInfo";
    public static final String CLIENTCMD_CHANNELSREADY = "channelsReady";
    public static final String CMD_GETREMOTECHANNELS = "getRemoteChannelInfo";
    public static final String CLIENTCMD_CHANNELSREMOTEREADY = "channelsRemoteReady";

    public static final PropertyBool ERROR = PropertyBool.create("error");

    private Map<LocalChannelId, String> publishedChannels = new HashMap<>();
    private int channelCount = 0;

    public TileEntityRouter() {
    }

    public void addPublishedChannels(Set<String> channels) {
        channels.addAll(publishedChannels.values());
    }

    public int countPublishedChannelsOnNet() {
        Set<String> channels = new HashSet<>();
        NetworkId networkId = findRoutingNetwork();
        if (networkId != null) {
            LogicTools.routers(getWorld(), networkId)
                    .forEach(router -> router.addPublishedChannels(channels));
        }
        return channels.size();
    }

    public boolean inError() {
        return channelCount > ConfigSetup.maxPublishedChannels.get();
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int cnt) {
        if (channelCount == cnt) {
            return;
        }
        channelCount = cnt;
        markDirtyClient();
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        boolean oldError = inError();

        super.onDataPacket(net, packet);

        if (getWorld().isRemote) {
            // If needed send a render update.
            if (oldError != inError()) {
                getWorld().markBlockRangeForRenderUpdate(getPos(), getPos());
            }
        }
    }


    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
        return super.writeToNBT(tagCompound);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        tagCompound.setInteger("chancnt", channelCount);
        NBTTagList published = new NBTTagList();
        for (Map.Entry<LocalChannelId, String> entry : publishedChannels.entrySet()) {
            NBTTagCompound tc = new NBTTagCompound();
            BlockPosTools.writeToNBT(tc, "pos", entry.getKey().getControllerPos());
            tc.setInteger("index", entry.getKey().getIndex());
            tc.setString("name", entry.getValue());
            published.appendTag(tc);
        }
        tagCompound.setTag("published", published);
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        channelCount = tagCompound.getInteger("chancnt");
        NBTTagList published = tagCompound.getTagList("published", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < published.tagCount(); i++) {
            NBTTagCompound tc = published.getCompoundTagAt(i);
            LocalChannelId id = new LocalChannelId(BlockPosTools.readFromNBT(tc, "pos"), tc.getInteger("index"));
            String name = tc.getString("name");
            publishedChannels.put(id, name);
        }
    }

    public Stream<Pair<String, IChannelType>> publishedChannelStream() {
        return LogicTools.connectors(world, pos)
                .map(connectorPos -> LogicTools.getControllerForConnector(getWorld(), connectorPos))
                .filter(Objects::nonNull)
                .flatMap(controller -> IntStream.range(0, MAX_CHANNELS)
                        .mapToObj(i -> {
                            ChannelInfo channelInfo = controller.getChannels()[i];
                            if (channelInfo != null && !channelInfo.getChannelName().isEmpty()) {
                                LocalChannelId id = new LocalChannelId(controller.getPos(), i);
                                String publishedName = publishedChannels.get(id);
                                if (publishedName != null && !publishedName.isEmpty()) {
                                    return Pair.of(publishedName, channelInfo.getType());
                                }
                            }
                            return null;
                        })
                        .filter(Objects::nonNull));
    }

    public void findLocalChannelInfo(List<ControllerChannelClientInfo> list, boolean onlyPublished, boolean remote) {
        LogicTools.connectors(getWorld(), getPos())
                .map(connectorPos -> LogicTools.getControllerForConnector(getWorld(), connectorPos))
                .filter(Objects::nonNull)
                .forEach(controller -> {
                    for (int i = 0; i < MAX_CHANNELS; i++) {
                        ChannelInfo channelInfo = controller.getChannels()[i];
                        if (channelInfo != null && !channelInfo.getChannelName().isEmpty()) {
                            LocalChannelId id = new LocalChannelId(controller.getPos(), i);
                            String publishedName = publishedChannels.get(id);
                            if (publishedName == null) {
                                publishedName = "";
                            }
                            if ((!onlyPublished) || !publishedName.isEmpty()) {
                                ControllerChannelClientInfo ci = new ControllerChannelClientInfo(channelInfo.getChannelName(), publishedName, controller.getPos(), channelInfo.getType(), remote, i);
                                if (list.stream().noneMatch(ii -> Objects.equals(ii.getPublishedName(), ci.getPublishedName())
                                        && Objects.equals(ii.getChannelName(), ci.getChannelName())
                                        && Objects.equals(ii.getChannelType(), ci.getChannelType())
                                        && Objects.equals(ii.getPos(), ci.getPos()))) {
                                    list.add(ci);
                                }
                            }
                        }
                    }
                });
    }

    private void findRemoteChannelInfo(List<ControllerChannelClientInfo> list) {
        NetworkId networkId = findRoutingNetwork();
        if (networkId != null) {
            // For each consumer on this network:
            LogicTools.consumers(world, networkId)
                    .forEach(consumerPos -> {
                        // Find all routers connected to this network and add their published local channels
                        LogicTools.routers(world, consumerPos)
                                .filter(r -> r != this)
                                .forEach(router -> router.findLocalChannelInfo(list, true, false));
                        // Find all wireless routers connected to this network and add the public or private
                        // channels that can be reached by them
                        LogicTools.wirelessRouters(world, consumerPos)
                                .filter(router -> !router.inError())
                                .forEach(router -> router.findRemoteChannelInfo(list));
                    });
        }
    }

    @Nullable
    public NetworkId findRoutingNetwork() {
        WorldBlob worldBlob = XNetBlobData.getBlobData(getWorld()).getWorldBlob(getWorld());
        return LogicTools.routingConnectors(getWorld(), getPos())
                .findFirst()
                .map(worldBlob::getNetworkAt)
                .orElse(null);
    }

    public void addRoutedConnectors(Map<SidedConsumer, IConnectorSettings> connectors, @Nonnull BlockPos controllerPos, int channel, IChannelType type,
                                    Map<WirelessChannelKey, Integer> wirelessVersions) {
        if (inError()) {
            // We are in error. Don't do anything
            return;
        }
        LocalChannelId id = new LocalChannelId(controllerPos, channel);
        String publishedName = publishedChannels.get(id);
        if (publishedName != null && !publishedName.isEmpty()) {
            NetworkId networkId = findRoutingNetwork();
            if (networkId != null) {
                LogicTools.consumers(getWorld(), networkId)
                        .forEach(consumerPos -> {
                            LogicTools.routers(world, consumerPos)
                                    .forEach(router -> router.addConnectorsFromConnectedNetworks(connectors, publishedName, type));
                            // First public channels
                            LogicTools.wirelessRouters(world, consumerPos)
                                    .filter(router -> !router.inError())
                                    .forEach(router -> {
                                        // First public
                                        router.addWirelessConnectors(connectors, publishedName, type, null, wirelessVersions);
                                        // Now private
                                        router.addWirelessConnectors(connectors, publishedName, type, getOwnerUUID(), wirelessVersions);
                                    });
                        });
            } else {
                // If there is no routing network that means we have a local network only
                addConnectorsFromConnectedNetworks(connectors, publishedName, type);
            }
        }
    }

    public boolean addConnectorsFromConnectedNetworks(Map<SidedConsumer, IConnectorSettings> connectors, String channelName, IChannelType type) {
        AtomicBoolean rc = new AtomicBoolean(false);
        LogicTools.connectors(getWorld(), getPos())
                .map(connectorPos -> LogicTools.getControllerForConnector(getWorld(), connectorPos))
                .filter(Objects::nonNull)
                .forEach(controller -> {
                    for (int i = 0; i < MAX_CHANNELS; i++) {
                        ChannelInfo info = controller.getChannels()[i];
                        if (info != null) {
                            String publishedName = publishedChannels.get(new LocalChannelId(controller.getPos(), i));
                            if (publishedName != null && !publishedName.isEmpty()) {
                                if (channelName.equals(publishedName) && type.equals(info.getType())) {
                                    connectors.putAll(controller.getConnectors(i));
                                    rc.set(true);
                                }
                            }
                        }
                    }
                });
        return rc.get();
    }

    private void updatePublishName(@Nonnull BlockPos controllerPos, int channel, String name) {
        LocalChannelId id = new LocalChannelId(controllerPos, channel);
        if (name == null || name.isEmpty()) {
            publishedChannels.remove(id);
        } else {
            publishedChannels.put(id, name);
        }
        int number = countPublishedChannelsOnNet();
        WorldBlob worldBlob = XNetBlobData.getBlobData(getWorld()).getWorldBlob(getWorld());
        NetworkId networkId = findRoutingNetwork();
        if (networkId != null) {
            if (number != channelCount) {
                LogicTools.routers(getWorld(), networkId)
                        .forEach(router -> router.setChannelCount(number));
            }
            worldBlob.markNetworkDirty(networkId); // Force a recalc of affected networks
        }
        for (NetworkId net : worldBlob.getNetworksAt(pos)) {
            worldBlob.markNetworkDirty(net);
        }
        for (EnumFacing facing : EnumFacing.VALUES) {
            for (NetworkId net : worldBlob.getNetworksAt(pos.offset(facing))) {
                worldBlob.markNetworkDirty(net);
            }
        }


        markDirtyQuick();
    }

    @Override
    public boolean execute(EntityPlayerMP playerMP, String command, TypedMap params) {
        boolean rc = super.execute(playerMP, command, params);
        if (rc) {
            return true;
        }
        if (CMD_UPDATENAME.equals(command)) {
            BlockPos controllerPos = params.get(PARAM_POS);
            int channel = params.get(PARAM_CHANNEL);
            String name = params.get(PARAM_NAME);
            updatePublishName(controllerPos, channel, name);
            return true;
        }

        return false;
    }

    @Nonnull
    @Override
    public <T> List<T> executeWithResultList(String command, TypedMap args, Type<T> type) {
        List<T> rc = super.executeWithResultList(command, args, type);
        if (!rc.isEmpty()) {
            return rc;
        }
        if (CMD_GETCHANNELS.equals(command)) {
            List<ControllerChannelClientInfo> list = new ArrayList<>();
            findLocalChannelInfo(list, false, false);
            return type.convert(list);
        } else if (CMD_GETREMOTECHANNELS.equals(command)) {
            List<ControllerChannelClientInfo> list = new ArrayList<>();
            findRemoteChannelInfo(list);
            return type.convert(list);
        }
        return Collections.emptyList();
    }


    @Override
    public <T> boolean receiveListFromServer(String command, List<T> list, Type<T> type) {
        boolean rc = super.receiveListFromServer(command, list, type);
        if (rc) {
            return true;
        }
        if (CLIENTCMD_CHANNELSREADY.equals(command)) {
            GuiRouter.fromServer_localChannels = new ArrayList<>(Type.create(ControllerChannelClientInfo.class).convert(list));
            return true;
        } else if (CLIENTCMD_CHANNELSREMOTEREADY.equals(command)) {
            GuiRouter.fromServer_remoteChannels = new ArrayList<>(Type.create(ControllerChannelClientInfo.class).convert(list));
            return true;
        }
        return false;
    }

    @Override
    @Optional.Method(modid = "theoneprobe")
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        super.addProbeInfo(mode, probeInfo, player, world, blockState, data);
        XNetBlobData blobData = XNetBlobData.getBlobData(world);
        WorldBlob worldBlob = blobData.getWorldBlob(world);
        Set<NetworkId> networks = worldBlob.getNetworksAt(data.getPos());
        for (NetworkId networkId : networks) {
            probeInfo.text(TextStyleClass.LABEL + "Network: " + TextStyleClass.INFO + networkId.getId());
            if (mode != ProbeMode.EXTENDED) {
                break;
            }
        }
        if (inError()) {
            probeInfo.text(TextStyleClass.ERROR + "Too many channels on router!");
        } else {
            probeInfo.text(TextStyleClass.LABEL + "Channels: " + TextStyleClass.INFO + getChannelCount());
        }

        if (mode == ProbeMode.DEBUG) {
            BlobId blobId = worldBlob.getBlobAt(data.getPos());
            if (blobId != null) {
                probeInfo.text(TextStyleClass.LABEL + "Blob: " + TextStyleClass.INFO + blobId.getId());
            }
            ColorId colorId = worldBlob.getColorAt(data.getPos());
            if (colorId != null) {
                probeInfo.text(TextStyleClass.LABEL + "Color: " + TextStyleClass.INFO + colorId.getId());
            }
        }
    }

    @Override
    public void onBlockBreak(World world, BlockPos pos, IBlockState state) {
        super.onBlockBreak(world, pos, state);
        if (!this.world.isRemote) {
            XNetBlobData blobData = XNetBlobData.getBlobData(this.world);
            WorldBlob worldBlob = blobData.getWorldBlob(this.world);
            worldBlob.removeCableSegment(pos);
            blobData.save();
        }
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote) {
            XNetBlobData blobData = XNetBlobData.getBlobData(world);
            WorldBlob worldBlob = blobData.getWorldBlob(world);
            NetworkId networkId = worldBlob.newNetwork();
            worldBlob.createNetworkProvider(pos, new ColorId(CableColor.ROUTING.ordinal() + 1), networkId);
            blobData.save();
        }
    }

    @Override
    public IBlockState getActualState(IBlockState state) {
        return state.withProperty(ERROR, inError());
    }
}
