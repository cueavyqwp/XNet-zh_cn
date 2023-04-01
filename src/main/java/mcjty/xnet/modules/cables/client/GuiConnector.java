package mcjty.xnet.modules.cables.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.widgets.Panel;
import mcjty.lib.gui.widgets.TextField;
import mcjty.lib.gui.widgets.ToggleButton;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.OrientationTools;
import mcjty.xnet.modules.cables.CableModule;
import mcjty.xnet.modules.cables.blocks.ConnectorTileEntity;
import mcjty.xnet.setup.XNetMessages;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Direction;

import javax.annotation.Nonnull;

import static mcjty.lib.gui.widgets.Widgets.*;
import static mcjty.xnet.modules.cables.blocks.ConnectorTileEntity.*;

public class GuiConnector extends GenericGuiContainer<ConnectorTileEntity, GenericContainer> {

    public static final int WIDTH = 220;
    public static final int HEIGHT = 50;

    private ToggleButton toggleButtons[] = new ToggleButton[6];

//    public GuiConnector(AdvancedConnectorTileEntity te, EmptyContainer container, PlayerInventory inventory) {
//        this((ConnectorTileEntity) te, container, inventory);
//    }

    public GuiConnector(ConnectorTileEntity tileEntity, GenericContainer container, PlayerInventory inventory) {
        super(tileEntity, container, inventory, CableModule.CONNECTOR.get().getManualEntry());

        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    public static void register() {
        register(CableModule.CONTAINER_CONNECTOR.get(), GuiConnector::new);
    }

    @Override
    public void init() {
        super.init();

        Panel toplevel = vertical().filledRectThickness(2);

        TextField nameField = new TextField().name("name").tooltips("为连接器取名");

        Panel namePanel = horizontal().children(label("名称:"), nameField);
        toplevel.children(namePanel);

        Panel togglePanel = horizontal().
                children(label("位置:"));
        for (Direction facing : OrientationTools.DIRECTION_VALUES) {
            toggleButtons[facing.ordinal()] = new ToggleButton().text(facing.getSerializedName().substring(0, 1).toUpperCase())
                .event(() -> {
                    sendServerCommandTyped(XNetMessages.INSTANCE, CMD_ENABLE,
                            TypedMap.builder()
                                    .put(PARAM_FACING, facing.ordinal())
                                    .put(PARAM_ENABLED, toggleButtons[facing.ordinal()].isPressed())
                                    .build());
                });
            toggleButtons[facing.ordinal()].pressed(tileEntity.isEnabled(facing));
            togglePanel.children(toggleButtons[facing.ordinal()]);
        }
        toplevel.children(togglePanel);

        toplevel.bounds(leftPos, topPos, WIDTH, HEIGHT);
        window = new Window(this, toplevel);

        window.bind(XNetMessages.INSTANCE, "name", tileEntity, "name");
    }

    @Override
    protected void renderBg(@Nonnull MatrixStack matrixStack, float partialTicks, int mouseX, int mouseY) {
        drawWindow(matrixStack);
    }
}
