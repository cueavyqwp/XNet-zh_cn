package mcjty.xnet.blocks.cables;

import mcjty.lib.container.EmptyContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.layout.HorizontalLayout;
import mcjty.lib.gui.layout.VerticalLayout;
import mcjty.lib.gui.widgets.Label;
import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.TextField;
import mcjty.lib.gui.widgets.ToggleButton;
import mcjty.lib.typed.TypedMap;
import mcjty.xnet.XNet;
import mcjty.xnet.setup.GuiProxy;
import mcjty.xnet.network.XNetMessages;
import net.minecraft.util.EnumFacing;

import java.awt.*;

import static mcjty.xnet.blocks.cables.ConnectorTileEntity.*;

public class GuiConnector extends GenericGuiContainer<ConnectorTileEntity> {

    public static final int WIDTH = 220;
    public static final int HEIGHT = 50;

    private ToggleButton toggleButtons[] = new ToggleButton[6];

    public GuiConnector(AdvancedConnectorTileEntity te, EmptyContainer container) {
        this((ConnectorTileEntity) te, container);
    }

    public GuiConnector(ConnectorTileEntity tileEntity, EmptyContainer container) {
        super(XNet.instance, XNetMessages.INSTANCE, tileEntity, container, GuiProxy.GUI_MANUAL_XNET, "connector");

        xSize = WIDTH;
        ySize = HEIGHT;
    }

    @Override
    public void initGui() {
        super.initGui();

        Panel toplevel = new Panel(mc, this).setFilledRectThickness(2).setLayout(new VerticalLayout());

        TextField nameField = new TextField(mc, this).setName("name").setTooltips("Set the name of this connector");

        Panel namePanel = new Panel(mc, this).setLayout(new HorizontalLayout()).
                addChild(new Label(mc, this).setText("Name:")).addChild(nameField);
        toplevel.addChild(namePanel);

        Panel togglePanel = new Panel(mc, this).setLayout(new HorizontalLayout()).
                addChild(new Label(mc, this).setText("Directions:"));
        for (EnumFacing facing : EnumFacing.VALUES) {
            toggleButtons[facing.ordinal()] = new ToggleButton(mc, this).setText(facing.getName().substring(0, 1).toUpperCase())
                .addButtonEvent(parent -> {
                    sendServerCommand(XNetMessages.INSTANCE, ConnectorTileEntity.CMD_ENABLE,
                            TypedMap.builder()
                                    .put(PARAM_FACING, facing.ordinal())
                                    .put(PARAM_ENABLED, toggleButtons[facing.ordinal()].isPressed())
                                    .build());
                });
            toggleButtons[facing.ordinal()].setPressed(tileEntity.isEnabled(facing));
            togglePanel.addChild(toggleButtons[facing.ordinal()]);
        }
        toplevel.addChild(togglePanel);

        toplevel.setBounds(new Rectangle(guiLeft, guiTop, WIDTH, HEIGHT));
        window = new Window(this, toplevel);

        window.bind(XNetMessages.INSTANCE, "name", tileEntity, VALUE_NAME.getName());
    }
}
