package mcjty.xnet.clientinfo;

import mcjty.xnet.api.channels.IChannelType;
import mcjty.xnet.api.channels.IConnectorSettings;
import mcjty.xnet.api.keys.SidedConsumer;
import net.minecraft.nbt.NBTTagCompound;

public class ConnectorInfo {

    private final IChannelType type;
    private final SidedConsumer id;
    private final IConnectorSettings connectorSettings;
    private final boolean advanced;

    public ConnectorInfo(IChannelType type, SidedConsumer id, boolean advanced) {
        this.type = type;
        this.id = id;
        this.advanced = advanced;
        connectorSettings = type.createConnector(id.getSide().getOpposite());
    }

    public IChannelType getType() {
        return type;
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public IConnectorSettings getConnectorSettings() {
        return connectorSettings;
    }

    public SidedConsumer getId() {
        return id;
    }

    public void writeToNBT(NBTTagCompound tag) {
        connectorSettings.writeToNBT(tag);
    }

    public void readFromNBT(NBTTagCompound tag) {
        connectorSettings.readFromNBT(tag);
    }
}
