package mcjty.xnet.blocks.redstoneproxy;

import mcjty.lib.McJtyRegister;
import mcjty.xnet.XNet;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class RedstoneProxyBlock extends Block {

    public RedstoneProxyBlock() {
        this(Material.IRON);
        setUnlocalizedName(XNet.MODID + ".redstone_proxy");
        setRegistryName("redstone_proxy");
        McJtyRegister.registerLater(this, XNet.instance, null);
        McJtyRegister.registerLater(new ItemBlock(this).setRegistryName(getRegistryName()), XNet.instance);
        setCreativeTab(XNet.setup.getTab());
    }

    public RedstoneProxyBlock(Material materialIn) {
        super(materialIn);
        setHardness(2.0f);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 0);
    }

    @SideOnly(Side.CLIENT)
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
    }


    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        tooltip.add("Acts as a proxy block for");
        tooltip.add("redstone. XNet can connect to this");
        tooltip.add(TextFormatting.YELLOW + "This version does no block update!");
    }

}
