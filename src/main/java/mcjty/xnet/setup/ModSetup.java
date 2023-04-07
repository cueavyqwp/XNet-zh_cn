package mcjty.xnet.setup;

import mcjty.lib.McJtyLib;
import mcjty.lib.compat.MainCompatHandler;
import mcjty.lib.setup.DefaultModSetup;
import mcjty.lib.varia.Logging;
import mcjty.xnet.CommandHandler;
import mcjty.xnet.ForgeEventHandlers;
import mcjty.xnet.XNet;
import mcjty.xnet.apiimpl.energy.EnergyChannelType;
import mcjty.xnet.apiimpl.fluids.FluidChannelType;
import mcjty.xnet.apiimpl.items.ItemChannelType;
import mcjty.xnet.apiimpl.logic.LogicChannelType;
import mcjty.xnet.config.ConfigSetup;
import mcjty.xnet.init.ModBlocks;
import mcjty.xnet.init.ModItems;
import mcjty.xnet.network.XNetMessages;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class ModSetup extends DefaultModSetup {

    public static boolean rftools = false;
    public static boolean rftoolsControl = false;

    @Override
    public void preInit(FMLPreInitializationEvent e) {
        super.preInit(e);

        McJtyLib.registerMod(XNet.instance);    // @todo why only xnet?

        MinecraftForge.EVENT_BUS.register(new ForgeEventHandlers());
        NetworkRegistry.INSTANCE.registerGuiHandler(XNet.instance, new GuiProxy());

        CommandHandler.registerCommands();

        XNetMessages.registerMessages("xnet");

        ModItems.init();
        ModBlocks.init();

        XNet.xNetApi.registerConsumerProvider((world, blob, net) -> blob.getConsumers(net));
        XNet.xNetApi.registerChannelType(new ItemChannelType());
        XNet.xNetApi.registerChannelType(new EnergyChannelType());
        XNet.xNetApi.registerChannelType(new FluidChannelType());
        XNet.xNetApi.registerChannelType(new LogicChannelType());
    }

    @Override
    protected void setupModCompat() {
        rftools = Loader.isModLoaded("rftools");
        rftoolsControl = Loader.isModLoaded("rftoolscontrol");

        if (rftoolsControl) {
            Logging.log("XNet Detected RFTools Control: enabling support");
            FMLInterModComms.sendFunctionMessage("rftoolscontrol", "getOpcodeRegistry", "mcjty.xnet.compat.rftoolscontrol.RFToolsControlSupport$GetOpcodeRegistry");
        }

        MainCompatHandler.registerWaila();
        MainCompatHandler.registerTOP();
    }

    @Override
    protected void setupConfig() {
        ConfigSetup.init();
    }

    @Override
    public void createTabs() {
        createTab("XNet", () -> new ItemStack(Item.getItemFromBlock(Blocks.ANVIL)));
    }

    @Override
    public void postInit(FMLPostInitializationEvent e) {
        super.postInit(e);
        ConfigSetup.postInit();
    }
}
