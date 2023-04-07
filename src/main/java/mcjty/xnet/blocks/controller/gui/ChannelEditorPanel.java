package mcjty.xnet.blocks.controller.gui;

import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.Widget;
import mcjty.lib.typed.TypedMap;
import mcjty.xnet.api.channels.IChannelSettings;
import mcjty.xnet.blocks.controller.TileEntityController;
import net.minecraft.client.Minecraft;

import java.util.Map;

import static mcjty.xnet.blocks.controller.TileEntityController.PARAM_CHANNEL;

public class ChannelEditorPanel extends AbstractEditorPanel {

    private final int channel;

    @Override
    public boolean isAdvanced() {
        return false;
    }

    @Override
    protected void update(String tag, Object value) {
        data.put(tag, value);
        TypedMap.Builder builder = TypedMap.builder();
        int i = 0;
        builder.put(PARAM_CHANNEL, channel);
        performUpdate(builder, i, TileEntityController.CMD_UPDATECHANNEL);
    }

    public ChannelEditorPanel(Panel panel, Minecraft mc, GuiController gui, int channel) {
        super(panel, mc, gui);
        this.channel = channel;
    }

    public void setState(IChannelSettings settings) {
        for (Map.Entry<String, Widget<?>> entry : components.entrySet()) {
            entry.getValue().setEnabled(settings.isEnabled(entry.getKey()));
        }
    }
}
