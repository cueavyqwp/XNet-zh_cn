package mcjty.xnet.blocks.controller.gui;

import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.Widget;
import mcjty.lib.typed.TypedMap;
import mcjty.xnet.api.channels.IConnectorSettings;
import mcjty.xnet.api.keys.SidedPos;
import mcjty.xnet.blocks.cables.ConnectorBlock;
import mcjty.xnet.blocks.controller.TileEntityController;
import net.minecraft.client.Minecraft;

import java.util.Map;

import static mcjty.xnet.blocks.controller.TileEntityController.*;

public class ConnectorEditorPanel extends AbstractEditorPanel {

    private final int channel;
    private final SidedPos sidedPos;
    private final boolean advanced;

    @Override
    protected void update(String tag, Object value) {
        data.put(tag, value);
        TypedMap.Builder builder = TypedMap.builder();
        int i = 0;
        builder.put(PARAM_POS, sidedPos.getPos())
            .put(PARAM_SIDE, sidedPos.getSide().ordinal())
            .put(PARAM_CHANNEL, channel);
        performUpdate(builder, i, TileEntityController.CMD_UPDATECONNECTOR);
    }

    public ConnectorEditorPanel(Panel panel, Minecraft mc, GuiController gui, int channel, SidedPos sidedPos) {
        super(panel, mc, gui);
        this.channel = channel;
        this.sidedPos = sidedPos;
        advanced = ConnectorBlock.isAdvancedConnector(mc.world, sidedPos.getPos().offset(sidedPos.getSide()));
    }

    @Override
    public boolean isAdvanced() {
        return advanced;
    }

    public void setState(IConnectorSettings settings) {
        for (Map.Entry<String, Widget<?>> entry : components.entrySet()) {
            entry.getValue().setEnabled(settings.isEnabled(entry.getKey()));
        }
    }
}
