package mcjty.xnet.init;

import mcjty.lib.blocks.BaseBlock;
import mcjty.lib.blocks.GenericBlock;
import mcjty.lib.builder.BaseBlockBuilder;
import mcjty.lib.builder.BlockFlags;
import mcjty.lib.builder.GenericBlockBuilderFactory;
import mcjty.lib.container.GenericContainer;
import mcjty.xnet.XNet;
import mcjty.xnet.blocks.cables.NetCableSetup;
import mcjty.xnet.blocks.controller.TileEntityController;
import mcjty.xnet.blocks.controller.gui.GuiController;
import mcjty.xnet.blocks.facade.FacadeBlock;
import mcjty.xnet.blocks.redstoneproxy.RedstoneProxyBlock;
import mcjty.xnet.blocks.redstoneproxy.RedstoneProxyUBlock;
import mcjty.xnet.blocks.router.GuiRouter;
import mcjty.xnet.blocks.router.TileEntityRouter;
import mcjty.xnet.blocks.wireless.GuiWirelessRouter;
import mcjty.xnet.blocks.wireless.TileEntityWirelessRouter;
import mcjty.xnet.config.ConfigSetup;
import mcjty.xnet.setup.GuiProxy;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ModBlocks {

    public static GenericBlock<TileEntityController, GenericContainer> controllerBlock;
    public static GenericBlock<TileEntityRouter, GenericContainer> routerBlock;
    public static GenericBlock<TileEntityWirelessRouter, GenericContainer> wirelessRouterBlock;

    public static BaseBlock antennaBlock;
    public static BaseBlock antennaBaseBlock;
    public static BaseBlock antennaDishBlock;

    public static FacadeBlock facadeBlock;
    public static RedstoneProxyBlock redstoneProxyBlock;
    public static RedstoneProxyUBlock redstoneProxyUBlock;

    public static GenericBlockBuilderFactory builderFactory;

    public static void init() {
        builderFactory = new GenericBlockBuilderFactory(XNet.instance).creativeTabs(XNet.setup.getTab());

        facadeBlock = new FacadeBlock();
        redstoneProxyBlock = new RedstoneProxyBlock();
        redstoneProxyUBlock = new RedstoneProxyUBlock();

        controllerBlock = builderFactory.<TileEntityController> builder("controller")
                .tileEntityClass(TileEntityController.class)
                .container(TileEntityController.CONTAINER_FACTORY)
                .flags(BlockFlags.REDSTONE_CHECK)   // Not really for redstone check but to have TE.checkRedstone() being called
                .guiId(GuiProxy.GUI_CONTROLLER)
                .property(TileEntityController.ERROR)
                .info("message.xnet.shiftmessage")
                .infoExtended("message.xnet.controller")
                .build();
        routerBlock = builderFactory.<TileEntityRouter> builder("router")
                .tileEntityClass(TileEntityRouter.class)
                .emptyContainer()
                .guiId(GuiProxy.GUI_ROUTER)
                .property(TileEntityRouter.ERROR)
                .info("message.xnet.shiftmessage")
                .infoExtended("message.xnet.router")
                .build();
        wirelessRouterBlock = builderFactory.<TileEntityWirelessRouter> builder("wireless_router")
                .tileEntityClass(TileEntityWirelessRouter.class)
                .emptyContainer()
                .guiId(GuiProxy.GUI_WIRELESS_ROUTER)
                .property(TileEntityWirelessRouter.ERROR)
                .info("message.xnet.shiftmessage")
                .infoExtended("message.xnet.wireless_router")
                .build();

        antennaBlock = new BaseBlockBuilder<>(XNet.instance, "antenna")
                .rotationType(BaseBlock.RotationType.HORIZROTATION)
                .flags(BlockFlags.NON_OPAQUE)
                .creativeTabs(XNet.setup.getTab())
                .info("message.xnet.shiftmessage")
                .infoExtended("message.xnet.antenna")
                .infoExtendedParameter(stack -> Integer.toString(ConfigSetup.antennaTier1Range.get()))
                .infoExtendedParameter(stack -> Integer.toString(ConfigSetup.wirelessRouterRfPerChannel[TileEntityWirelessRouter.TIER_1].get()))
                .infoExtendedParameter(stack -> Integer.toString(ConfigSetup.antennaTier2Range.get()))
                .infoExtendedParameter(stack -> Integer.toString(ConfigSetup.wirelessRouterRfPerChannel[TileEntityWirelessRouter.TIER_2].get()))
                .build();
        antennaBaseBlock = new BaseBlockBuilder<>(XNet.instance, "antenna_base")
                .rotationType(BaseBlock.RotationType.NONE)
                .flags(BlockFlags.NON_OPAQUE)
                .creativeTabs(XNet.setup.getTab())
                .info("message.xnet.shiftmessage")
                .infoExtended("message.xnet.antenna_base")
                .build();
        antennaDishBlock = new BaseBlockBuilder<>(XNet.instance, "antenna_dish")
                .rotationType(BaseBlock.RotationType.HORIZROTATION)
                .flags(BlockFlags.NON_OPAQUE)
                .creativeTabs(XNet.setup.getTab())
                .info("message.xnet.shiftmessage")
                .infoExtended("message.xnet.antenna_dish")
                .infoExtendedParameter(stack -> Integer.toString(ConfigSetup.wirelessRouterRfPerChannel[TileEntityWirelessRouter.TIER_INF].get()))
                .build();

        NetCableSetup.init();
    }

    @SideOnly(Side.CLIENT)
    public static void initModels() {
        controllerBlock.initModel();
        controllerBlock.setGuiFactory(GuiController::new);

        routerBlock.initModel();
        routerBlock.setGuiFactory(GuiRouter::new);

        wirelessRouterBlock.initModel();
        wirelessRouterBlock.setGuiFactory(GuiWirelessRouter::new);

        antennaBlock.initModel();
        antennaBaseBlock.initModel();
        antennaDishBlock.initModel();

        facadeBlock.initModel();
        redstoneProxyBlock.initModel();
        redstoneProxyUBlock.initModel();
        NetCableSetup.initClient();
    }

    @SideOnly(Side.CLIENT)
    public static void initItemModels() {
        facadeBlock.initItemModel();
        NetCableSetup.initItemModels();
    }

    @SideOnly(Side.CLIENT)
    public static void initColorHandlers(BlockColors blockColors) {
        facadeBlock.initColorHandler(blockColors);
        NetCableSetup.initColorHandlers(blockColors);
    }
}
