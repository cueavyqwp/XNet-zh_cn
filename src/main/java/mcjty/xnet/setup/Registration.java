package mcjty.xnet.setup;


import mcjty.lib.McJtyRegister;
import mcjty.lib.datafix.fixes.TileEntityNamespace;
import mcjty.xnet.XNet;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.datafix.FixTypes;
import net.minecraftforge.common.util.ModFixs;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber
public class Registration {

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        ModFixs modFixs = FMLCommonHandler.instance().getDataFixer().init(XNet.MODID, 2);
        McJtyRegister.registerBlocks(XNet.instance, event.getRegistry(), modFixs, 1);

        // We used to accidentally register TEs with names like "minecraft:xnet_facade" instead of "xnet:facade".
        // Set up a DataFixer to map these incorrect names to the correct ones, so that we don't break old saved games.
        // @todo Remove all this if we ever break saved-game compatibility.
        Map<String, String> oldToNewIdMap = new HashMap<>();
        oldToNewIdMap.put(XNet.MODID + "_facade", XNet.MODID + ":facade");
        oldToNewIdMap.put("minecraft:" + XNet.MODID + "_facade", XNet.MODID + ":facade");
        oldToNewIdMap.put(XNet.MODID + "_connector", XNet.MODID + ":connector");
        oldToNewIdMap.put("minecraft:" + XNet.MODID + "_connector", XNet.MODID + ":connector");
        oldToNewIdMap.put(XNet.MODID + "_advanced_connector", XNet.MODID + ":advanced_connector");
        oldToNewIdMap.put("minecraft:" + XNet.MODID + "_advanced_connector", XNet.MODID + ":advanced_connector");
        modFixs.registerFix(FixTypes.BLOCK_ENTITY, new TileEntityNamespace(oldToNewIdMap, 2));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        McJtyRegister.registerItems(XNet.instance, event.getRegistry());
    }

}
