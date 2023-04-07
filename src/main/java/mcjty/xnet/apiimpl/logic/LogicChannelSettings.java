package mcjty.xnet.apiimpl.logic;

import com.google.gson.JsonObject;
import mcjty.lib.varia.WorldTools;
import mcjty.xnet.XNet;
import mcjty.xnet.api.channels.IChannelSettings;
import mcjty.xnet.api.channels.IConnectorSettings;
import mcjty.xnet.api.channels.IControllerContext;
import mcjty.xnet.api.gui.IEditorGui;
import mcjty.xnet.api.gui.IndicatorIcon;
import mcjty.xnet.api.helper.DefaultChannelSettings;
import mcjty.xnet.api.keys.SidedConsumer;
import mcjty.xnet.blocks.cables.ConnectorTileEntity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogicChannelSettings extends DefaultChannelSettings implements IChannelSettings {

    public static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    private int delay = 0;
    private int colors = 0;     // Colors for this channel
    private List<Pair<SidedConsumer, LogicConnectorSettings>> sensors = null;
    private List<Pair<SidedConsumer, LogicConnectorSettings>> outputs = null;

    @Override
    public JsonObject writeToJson() {
        JsonObject object = new JsonObject();
        return object;
    }

    @Override
    public void readFromJson(JsonObject data) {
    }


    @Override
    public void readFromNBT(NBTTagCompound tag) {
        delay = tag.getInteger("delay");
        colors = tag.getInteger("colors");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("delay", delay);
        tag.setInteger("colors", colors);
    }

    @Override
    public int getColors() {
        return colors;
    }

    @Override
    public void tick(int channel, IControllerContext context) {
        delay--;
        if (delay <= 0) {
            delay = 200 * 6;      // Multiply of the different speeds we have
        }
        if (delay % 5 != 0) {
            return;
        }
        int d = delay / 5;
        updateCache(channel, context);
        World world = context.getControllerWorld();

        colors = 0;
        for (Pair<SidedConsumer, LogicConnectorSettings> entry : sensors) {
            LogicConnectorSettings settings = entry.getValue();
            if (d % settings.getSpeed() != 0) {
                // Use the color settings from this connector as we last remembered it
                colors |= settings.getColorMask();
                continue;
            }
            int sensorColors = 0;
            BlockPos connectorPos = context.findConsumerPosition(entry.getKey().getConsumerId());
            if (connectorPos != null) {
                EnumFacing side = entry.getKey().getSide();
                BlockPos pos = connectorPos.offset(side);
                if (!WorldTools.chunkLoaded(world, pos)) {
                    // If it is not chunkloaded we just use the color settings as we last remembered it
                    colors |= settings.getColorMask();
                    continue;
                }

                boolean sense = true;

                sense = !checkRedstone(world, settings, connectorPos);
                if (sense && !context.matchColor(settings.getColorsMask())) {
                    sense = false;
                }

                // If sense is false the sensor is disabled which means the colors from it will also be disabled
                if (sense) {
                    TileEntity te = world.getTileEntity(pos);

                    for (Sensor sensor : settings.getSensors()) {
                        if (sensor.test(te, world, pos, settings)) {
                            sensorColors |= 1 << sensor.getOutputColor().ordinal();
                        }
                    }
                }
            }
            settings.setColorMask(sensorColors);
            colors |= sensorColors;
        }

        for (Pair<SidedConsumer, LogicConnectorSettings> entry : outputs) {
            LogicConnectorSettings settings = entry.getValue();
            if (d % settings.getSpeed() != 0) {
                continue;
            }

            BlockPos connectorPos = context.findConsumerPosition(entry.getKey().getConsumerId());
            if (connectorPos != null) {
                EnumFacing side = entry.getKey().getSide();
                if (!WorldTools.chunkLoaded(world, connectorPos)) {
                    continue;
                }

                TileEntity te = world.getTileEntity(connectorPos);
                if (te instanceof ConnectorTileEntity) {
                    ConnectorTileEntity connectorTE = (ConnectorTileEntity) te;
                    int powerOut;
                    if (checkRedstone(world, settings, connectorPos)) {
                        powerOut = 0;
                    } else if (!context.matchColor(settings.getColorsMask())) {
                        powerOut = 0;
                    } else {
                        powerOut = settings.getRedstoneOut() == null ? 0 : settings.getRedstoneOut();
                    }
                    connectorTE.setPowerOut(side, powerOut);
                }
            }
        }
    }

    private void updateCache(int channel, IControllerContext context) {
        if (sensors == null) {
            sensors = new ArrayList<>();
            outputs = new ArrayList<>();
            Map<SidedConsumer, IConnectorSettings> connectors = context.getConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                LogicConnectorSettings con = (LogicConnectorSettings) entry.getValue();
                if (con.getLogicMode() == LogicConnectorSettings.LogicMode.SENSOR) {
                    sensors.add(Pair.of(entry.getKey(), con));
                } else {
                    outputs.add(Pair.of(entry.getKey(), con));
                }
            }

            connectors = context.getRoutedConnectors(channel);
            for (Map.Entry<SidedConsumer, IConnectorSettings> entry : connectors.entrySet()) {
                LogicConnectorSettings con = (LogicConnectorSettings) entry.getValue();
                if (con.getLogicMode() == LogicConnectorSettings.LogicMode.OUTPUT) {
                    outputs.add(Pair.of(entry.getKey(), con));
                }
            }
        }
    }

    @Override
    public void cleanCache() {
        sensors = null;
    }

    @Nullable
    @Override
    public IndicatorIcon getIndicatorIcon() {
        return new IndicatorIcon(iconGuiElements, 11, 90, 11, 10);
    }

    @Nullable
    @Override
    public String getIndicator() {
        return null;
    }

    @Override
    public boolean isEnabled(String tag) {
        return true;
    }

    @Override
    public void createGui(IEditorGui gui) {

    }

    @Override
    public void update(Map<String, Object> data) {

    }
}
