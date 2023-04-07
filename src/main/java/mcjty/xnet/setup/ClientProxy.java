package mcjty.xnet.setup;

import mcjty.lib.font.TrueTypeFont;
import mcjty.lib.setup.DefaultClientProxy;
import mcjty.xnet.RenderWorldLastEventHandler;
import mcjty.xnet.XNet;
import mcjty.xnet.blocks.generic.BakedModelLoader;
import mcjty.xnet.init.ModBlocks;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClientProxy extends DefaultClientProxy {

    public static TrueTypeFont font;

    @Override
    public void preInit(FMLPreInitializationEvent e) {
        super.preInit(e);
        MinecraftForge.EVENT_BUS.register(this);
        OBJLoader.INSTANCE.addDomain(XNet.MODID);
        ModelLoaderRegistry.registerLoader(new BakedModelLoader());
    }

    @SubscribeEvent
    public void colorHandlerEventBlock(ColorHandlerEvent.Block event) {
        ModBlocks.initColorHandlers(event.getBlockColors());
    }

    @Override
    public void postInit(FMLPostInitializationEvent e) {
        super.postInit(e);
        ModBlocks.initItemModels();
    }

    @SubscribeEvent
    public void renderWorldLastEvent(RenderWorldLastEvent evt) {
        RenderWorldLastEventHandler.tick(evt);
    }
}
