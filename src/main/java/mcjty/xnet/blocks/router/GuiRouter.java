package mcjty.xnet.blocks.router;

import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.layout.HorizontalLayout;
import mcjty.lib.gui.layout.PositionalLayout;
import mcjty.lib.gui.widgets.*;
import mcjty.lib.typed.TypedMap;
import mcjty.lib.varia.BlockPosTools;
import mcjty.xnet.XNet;
import mcjty.xnet.api.channels.IChannelType;
import mcjty.xnet.clientinfo.ControllerChannelClientInfo;
import mcjty.xnet.setup.GuiProxy;
import mcjty.xnet.network.PacketGetLocalChannelsRouter;
import mcjty.xnet.network.PacketGetRemoteChannelsRouter;
import mcjty.xnet.network.XNetMessages;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.List;

import static mcjty.xnet.blocks.router.TileEntityRouter.*;

public class GuiRouter extends GenericGuiContainer<TileEntityRouter> {

    private WidgetList localChannelList;
    private WidgetList remoteChannelList;
    public static List<ControllerChannelClientInfo> fromServer_localChannels = null;
    public static List<ControllerChannelClientInfo> fromServer_remoteChannels = null;
    private boolean needsRefresh = true;
    private int listDirty;

    private static final ResourceLocation iconGuiElements = new ResourceLocation(XNet.MODID, "textures/gui/guielements.png");

    public GuiRouter(TileEntityRouter router, GenericContainer container) {
        super(XNet.instance, XNetMessages.INSTANCE, router, container, GuiProxy.GUI_MANUAL_XNET, "router");
    }

    @Override
    public void initGui() {
        window = new Window(this, tileEntity, XNetMessages.INSTANCE, new ResourceLocation(XNet.MODID, "gui/router.gui"));
        super.initGui();

        localChannelList = window.findChild("localchannels");
        remoteChannelList = window.findChild("remotechannels");

        refresh();
        listDirty = 0;
    }

    private void updatePublish(BlockPos pos, int index, String name) {
        sendServerCommand(XNetMessages.INSTANCE, TileEntityRouter.CMD_UPDATENAME,
                TypedMap.builder()
                        .put(PARAM_POS, pos)
                        .put(PARAM_CHANNEL, index)
                        .put(PARAM_NAME, name)
                        .build());
    }

    private void refresh() {
        fromServer_localChannels = null;
        fromServer_remoteChannels = null;
        needsRefresh = true;
        listDirty = 3;
        requestListsIfNeeded();
    }


    private boolean listsReady() {
        return fromServer_localChannels != null && fromServer_remoteChannels != null;
    }

    private void populateList() {
        if (!listsReady()) {
            return;
        }
        if (!needsRefresh) {
            return;
        }
        needsRefresh = false;

        localChannelList.removeChildren();
        localChannelList.setRowheight(40);
        int sel = localChannelList.getSelected();

        for (ControllerChannelClientInfo channel : fromServer_localChannels) {
            localChannelList.addChild(makeChannelLine(channel, true));
        }

        localChannelList.setSelected(sel);

        remoteChannelList.removeChildren();
        remoteChannelList.setRowheight(40);
        sel = remoteChannelList.getSelected();

        for (ControllerChannelClientInfo channel : fromServer_remoteChannels) {
            remoteChannelList.addChild(makeChannelLine(channel, false));
        }

        remoteChannelList.setSelected(sel);
    }

    private Panel makeChannelLine(ControllerChannelClientInfo channel, boolean local) {
        String name = channel.getChannelName();
        String publishedName = channel.getPublishedName();
        BlockPos controllerPos = channel.getPos();
        IChannelType type = channel.getChannelType();
        int index = channel.getIndex();

        Panel panel = new Panel(mc, this).setLayout(new PositionalLayout()).setDesiredHeight(30);
        Panel panel1 = new Panel(mc, this).setLayout(new HorizontalLayout().setHorizontalMargin(0).setSpacing(0)).setLayoutHint(new PositionalLayout.PositionalHint(0, 0, 160, 13));
        int labelColor = 0xff2244aa;
        // @todo, better way to show remote channels
        if (channel.isRemote()) {
            labelColor = 0xffaa1133;
        }
        panel1.addChild(new Label(mc, this).setText("Ch").setColor(labelColor));
        panel1.addChild(new Label(mc, this).setText(name));
        panel1.addChild(new Label(mc, this).setText(">").setColor(labelColor));
        if (channel.isRemote()) {
            panel1.addChild(new ImageLabel(mc, this).setImage(iconGuiElements, 48, 80).setDesiredWidth(16));
        }
        if (local) {
            TextField pubName = new TextField(mc, this).setText(publishedName).setDesiredWidth(50).setDesiredHeight(13)
                    .addTextEvent((parent, newText) -> updatePublish(controllerPos, index, newText));
            panel1.addChild(pubName);
        } else {
            panel1.addChild(new Label(mc, this).setText(publishedName).setColor(0xff33ff00));
        }

        Panel panel2 = new Panel(mc, this).setLayout(new HorizontalLayout().setHorizontalMargin(0).setSpacing(0)).setLayoutHint(new PositionalLayout.PositionalHint(0, 13, 160, 13));
        panel2.addChild(new Label(mc, this).setText("Pos").setColor(labelColor));
        panel2.addChild(new Label(mc, this).setText(BlockPosTools.toString(controllerPos)));

        Panel panel3 = new Panel(mc, this).setLayout(new HorizontalLayout().setHorizontalMargin(0).setSpacing(0)).setLayoutHint(new PositionalLayout.PositionalHint(0, 26, 160, 13));
        panel3.addChild(new Label(mc, this).setText("Index").setColor(labelColor));
        panel3.addChild(new Label(mc, this).setText(index + " (" + type.getName() + ")"));

        panel.addChild(panel1).addChild(panel2).addChild(panel3);
        return panel;
    }

    private void requestListsIfNeeded() {
        if (fromServer_localChannels != null && fromServer_remoteChannels != null) {
            return;
        }
        listDirty--;
        if (listDirty <= 0) {
            XNetMessages.INSTANCE.sendToServer(new PacketGetLocalChannelsRouter(tileEntity.getPos()));
            XNetMessages.INSTANCE.sendToServer(new PacketGetRemoteChannelsRouter(tileEntity.getPos()));
            listDirty = 10;
        }
    }


    @Override
    protected void drawGuiContainerBackgroundLayer(float v, int x1, int x2) {
        requestListsIfNeeded();
        populateList();
        drawWindow();
    }
}
