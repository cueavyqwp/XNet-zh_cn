package mcjty.xnet.blocks.wireless;

import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.xnet.XNet;
import mcjty.xnet.setup.GuiProxy;
import mcjty.xnet.network.XNetMessages;
import net.minecraft.util.ResourceLocation;

public class GuiWirelessRouter extends GenericGuiContainer<TileEntityWirelessRouter> {

    public GuiWirelessRouter(TileEntityWirelessRouter router, GenericContainer container) {
        super(XNet.instance, XNetMessages.INSTANCE, router, container, GuiProxy.GUI_MANUAL_XNET, "wireless_router");
    }

    @Override
    public void initGui() {
        window = new Window(this, tileEntity, XNetMessages.INSTANCE, new ResourceLocation(XNet.MODID, "gui/wireless_router.gui"));
        super.initGui();
    }
}
