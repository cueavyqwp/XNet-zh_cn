package mcjty.xnet.logic;

import mcjty.xnet.XNet;
import mcjty.xnet.api.channels.IChannelSettings;
import mcjty.xnet.api.channels.IChannelType;
import mcjty.xnet.api.keys.ConsumerId;
import mcjty.xnet.api.keys.SidedConsumer;
import mcjty.xnet.clientinfo.ConnectorInfo;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class ChannelInfo {

    public static final int MAX_CHANNELS = 8;

    private final IChannelType type;
    private final IChannelSettings channelSettings;
    private String channelName;
    private boolean enabled = true;

    private final Map<SidedConsumer, ConnectorInfo> connectors = new HashMap<>();

    public ChannelInfo(IChannelType type) {
        this.type = type;
        channelSettings = type.createChannel();
    }

    @Nonnull
    public String getChannelName() {
        return channelName == null ? "" : channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public IChannelType getType() {
        return type;
    }

    public IChannelSettings getChannelSettings() {
        return channelSettings;
    }

    public Map<SidedConsumer, ConnectorInfo> getConnectors() {
        return connectors;
    }

    public ConnectorInfo createConnector(SidedConsumer id, boolean advanced) {
        ConnectorInfo info = new ConnectorInfo(type, id, advanced);
        connectors.put(id, info);
        return info;
    }

    public void writeToNBT(NBTTagCompound tag) {
        channelSettings.writeToNBT(tag);
        tag.setBoolean("enabled", enabled);
        if (channelName != null && !channelName.isEmpty()) {
            tag.setString("name", channelName);
        }
        NBTTagList conlist = new NBTTagList();
        for (Map.Entry<SidedConsumer, ConnectorInfo> entry : connectors.entrySet()) {
            NBTTagCompound tc = new NBTTagCompound();
            ConnectorInfo connectorInfo = entry.getValue();
            connectorInfo.writeToNBT(tc);
            tc.setInteger("consumerId", entry.getKey().getConsumerId().getId());
            tc.setInteger("side", entry.getKey().getSide().ordinal());
            tc.setString("type", connectorInfo.getType().getID());
            tc.setBoolean("advanced", connectorInfo.isAdvanced());
            conlist.appendTag(tc);
        }
        tag.setTag("connectors", conlist);
    }

    public void readFromNBT(NBTTagCompound tag) {
        channelSettings.readFromNBT(tag);
        enabled = tag.getBoolean("enabled");
        if (tag.hasKey("name")) {
            channelName = tag.getString("name");
        } else {
            channelName = null;
        }
        NBTTagList conlist = tag.getTagList("connectors", Constants.NBT.TAG_COMPOUND);
        for (int i = 0 ; i < conlist.tagCount() ; i++) {
            NBTTagCompound tc = conlist.getCompoundTagAt(i);
            String id = tc.getString("type");
            IChannelType type = XNet.xNetApi.findType(id);
            if (type == null) {
                XNet.setup.getLogger().warn("Unsupported type " + id + "!");
                continue;
            }
            if (!getType().equals(type)) {
                XNet.setup.getLogger().warn("Trying to load a connector with non-matching type " + type + "!");
                continue;
            }
            ConsumerId consumerId = new ConsumerId(tc.getInteger("consumerId"));
            EnumFacing side = EnumFacing.VALUES[tc.getInteger("side")];
            SidedConsumer key = new SidedConsumer(consumerId, side);
            boolean advanced = tc.getBoolean("advanced");
            ConnectorInfo connectorInfo = new ConnectorInfo(type, key, advanced);
            connectorInfo.readFromNBT(tc);
            connectors.put(key, connectorInfo);
        }
    }
}
