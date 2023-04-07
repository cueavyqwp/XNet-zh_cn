package mcjty.xnet.multiblock;

import mcjty.lib.varia.BlockPosTools;
import mcjty.lib.varia.GlobalCoordinate;
import mcjty.lib.worlddata.AbstractWorldData;
import mcjty.xnet.XNet;
import mcjty.xnet.api.channels.IChannelType;
import mcjty.xnet.api.keys.NetworkId;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class XNetWirelessChannels extends AbstractWorldData<XNetWirelessChannels> {

    private static final String NAME = "XNetWirelessChannels";

    private final Map<WirelessChannelKey, WirelessChannelInfo> channelToWireless = new HashMap<>();

    public XNetWirelessChannels(String name) {
        super(name);
    }

    @Override
    public void clear() {
        channelToWireless.clear();
    }

    private int cnt = 30;

    private int globalChannelVersion = 0;

    public void transmitChannel(String channel, @Nonnull IChannelType channelType, @Nullable UUID ownerUUID, int dimension, BlockPos wirelessRouterPos, NetworkId network) {
        WirelessChannelInfo channelInfo;
        WirelessChannelKey key = new WirelessChannelKey(channel, channelType, ownerUUID);
        if (channelToWireless.containsKey(key)) {
            channelInfo = channelToWireless.get(key);
        } else {
            channelInfo = new WirelessChannelInfo();
            channelToWireless.put(key, channelInfo);
        }

        GlobalCoordinate pos = new GlobalCoordinate(wirelessRouterPos, dimension);
        WirelessRouterInfo info = channelInfo.getRouter(pos);
        if (info == null) {
            info = new WirelessRouterInfo(pos);
            channelInfo.updateRouterInfo(pos, info);
            channelInfo.incVersion();

            // Global version increase to make sure all wireless routers can detect if there is a need
            // to make their local network dirty to ensure that all controllers connected to that network
            // get a chance to pick up the new transmitted channel
            channelUpdated();
        }
        info.setAge(0);
        info.setNetworkId(network);
        save();

//        cnt--;
//        if (cnt > 0) {
//            return;
//        }
//        cnt = 30;
//        dump();
    }

    private void channelUpdated() {
        globalChannelVersion++;
    }

    public int getGlobalChannelVersion() {
        return globalChannelVersion;
    }

    private void dump() {
        for (Map.Entry<WirelessChannelKey, WirelessChannelInfo> entry : channelToWireless.entrySet()) {
            System.out.println("Channel = " + entry.getKey());
            WirelessChannelInfo channelInfo = entry.getValue();
            for (Map.Entry<GlobalCoordinate, WirelessRouterInfo> infoEntry : channelInfo.getRouters().entrySet()) {
                GlobalCoordinate pos = infoEntry.getKey();
                WirelessRouterInfo info = infoEntry.getValue();
                System.out.println("    Pos = " + BlockPosTools.toString(pos.getCoordinate()) + " (age " + info.age + ", net " + info.networkId.getId() + ")");
            }
        }
    }

    public void tick(World world, int amount) {
        if (channelToWireless.isEmpty()) {
            return;
        }

        XNetBlobData blobData = XNetBlobData.getBlobData(world);

        Set<WirelessChannelKey> toDeleteChannel = new HashSet<>();
        for (Map.Entry<WirelessChannelKey, WirelessChannelInfo> entry : channelToWireless.entrySet()) {
            WirelessChannelInfo channelInfo = entry.getValue();
            Set<GlobalCoordinate> toDelete = new HashSet<>();
            for (Map.Entry<GlobalCoordinate, WirelessRouterInfo> infoEntry : channelInfo.getRouters().entrySet()) {
                WirelessRouterInfo info = infoEntry.getValue();
                int age = info.getAge();
                age += amount;
                info.setAge(age);
                if (age > 40) { // @todo configurable
                    toDelete.add(infoEntry.getKey());
                }
            }
            for (GlobalCoordinate pos : toDelete) {
                WorldBlob worldBlob = blobData.getWorldBlob(pos.getDimension());
                NetworkId networkId = channelInfo.getRouter(pos).getNetworkId();
//                System.out.println("Clean up wireless network = " + networkId + " (" + entry.getKey() + ")");
                worldBlob.markNetworkDirty(networkId);
                channelInfo.removeRouterInfo(pos);
                channelInfo.incVersion();
            }
            if (channelInfo.getRouters().isEmpty()) {
                toDeleteChannel.add(entry.getKey());
            }
        }

        if (!toDeleteChannel.isEmpty()) {
            for (WirelessChannelKey key : toDeleteChannel) {
                channelToWireless.remove(key);
            }
        }

        save();
    }

    @Nonnull
    public static XNetWirelessChannels getWirelessChannels(World world) {
        return getData(world, XNetWirelessChannels.class, NAME);
    }

    public WirelessChannelInfo findChannel(String name, @Nonnull IChannelType channelType, @Nullable UUID owner) {
        WirelessChannelKey key = new WirelessChannelKey(name, channelType, owner);
        return findChannel(key);
    }

    public WirelessChannelInfo findChannel(WirelessChannelKey key) {
        return channelToWireless.get(key);
    }

    public Stream<WirelessChannelInfo> findChannels(@Nullable UUID owner) {
        return channelToWireless.entrySet().stream().filter(pair -> {
            WirelessChannelKey key = pair.getKey();
            return (owner == null && key.getOwner() == null) || (owner != null && (key.getOwner() == null || owner.equals(key.getOwner())));
        }).map(pair -> pair.getValue());
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        channelToWireless.clear();
        NBTTagList tagList = compound.getTagList("channels", Constants.NBT.TAG_COMPOUND);
        for (int i = 0 ; i < tagList.tagCount() ; i++) {
            NBTTagCompound tc = tagList.getCompoundTagAt(i);
            WirelessChannelInfo channelInfo = new WirelessChannelInfo();
            readRouters(tc.getTagList("routers", Constants.NBT.TAG_COMPOUND), channelInfo);
            UUID owner = null;
            if (tc.hasUniqueId("owner")) {
                owner = tc.getUniqueId("owner");
            }
            String name = tc.getString("name");
            IChannelType type = XNet.xNetApi.findType(tc.getString("type"));
            channelToWireless.put(new WirelessChannelKey(name, type, owner), channelInfo);
        }
    }

    private void readRouters(NBTTagList tagList, WirelessChannelInfo channelInfo) {
        for (int i = 0 ; i < tagList.tagCount() ; i++) {
            NBTTagCompound tc = tagList.getCompoundTagAt(i);
            GlobalCoordinate pos = new GlobalCoordinate(new BlockPos(tc.getInteger("x"), tc.getInteger("y"), tc.getInteger("z")), tc.getInteger("dim"));
            WirelessRouterInfo info = new WirelessRouterInfo(pos);
            info.setAge(tc.getInteger("age"));
            info.setNetworkId(new NetworkId(tc.getInteger("network")));
            channelInfo.updateRouterInfo(pos, info);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList channelTagList = new NBTTagList();

        for (Map.Entry<WirelessChannelKey, WirelessChannelInfo> entry : channelToWireless.entrySet()) {
            NBTTagCompound channelTc = new NBTTagCompound();
            WirelessChannelInfo channelInfo = entry.getValue();
            WirelessChannelKey key = entry.getKey();
            channelTc.setString("name", key.getName());
            channelTc.setString("type", key.getChannelType().getID());
            if (key.getOwner() != null) {
                channelTc.setUniqueId("owner", key.getOwner());
            }
            channelTc.setTag("routers", writeRouters(channelInfo));
            channelTagList.appendTag(channelTc);
        }

        compound.setTag("channels", channelTagList);

        return compound;
    }

    private NBTTagList writeRouters(WirelessChannelInfo channelInfo) {
        NBTTagList tagList = new NBTTagList();

        for (Map.Entry<GlobalCoordinate, WirelessRouterInfo> infoEntry : channelInfo.getRouters().entrySet()) {
            NBTTagCompound tc = new NBTTagCompound();
            GlobalCoordinate pos = infoEntry.getKey();
            tc.setInteger("dim", pos.getDimension());
            tc.setInteger("x", pos.getCoordinate().getX());
            tc.setInteger("y", pos.getCoordinate().getY());
            tc.setInteger("z", pos.getCoordinate().getZ());
            WirelessRouterInfo info = infoEntry.getValue();
            tc.setInteger("age", info.getAge());
            tc.setInteger("network", info.getNetworkId().getId());
            tagList.appendTag(tc);
        }
        return tagList;
    }

    public static class WirelessChannelInfo {
        private final Map<GlobalCoordinate, WirelessRouterInfo> routers = new HashMap<>();
        private int version = 0;

        public void updateRouterInfo(GlobalCoordinate pos, WirelessRouterInfo info) {
            routers.put(pos, info);
        }

        public void removeRouterInfo(GlobalCoordinate pos) {
            routers.remove(pos);
        }

        public WirelessRouterInfo getRouter(GlobalCoordinate pos) {
            return routers.get(pos);
        }

        public Map<GlobalCoordinate, WirelessRouterInfo> getRouters() {
            return routers;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public void incVersion() {
            version++;
        }
    }

    public static class WirelessRouterInfo {
        private int age;
        private NetworkId networkId;
        private final GlobalCoordinate coordinate;

        public WirelessRouterInfo(GlobalCoordinate coordinate) {
            age = 0;
            this.coordinate = coordinate;
        }

        public NetworkId getNetworkId() {
            return networkId;
        }

        public void setNetworkId(NetworkId networkId) {
            this.networkId = networkId;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public GlobalCoordinate getCoordinate() {
            return coordinate;
        }
    }
}
