package mcjty.xnet.blocks.redstoneproxy;

import mcjty.lib.McJtyRegister;
import mcjty.xnet.XNet;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RedstoneProxyUBlock extends RedstoneProxyBlock {

    public RedstoneProxyUBlock() {
        super(Material.IRON);
        setUnlocalizedName(XNet.MODID + ".redstone_proxy_upd");
        setRegistryName("redstone_proxy_upd");
        McJtyRegister.registerLater(this, XNet.instance, null);
        McJtyRegister.registerLater(new ItemBlock(this).setRegistryName(getRegistryName()), XNet.instance);
        setCreativeTab(XNet.setup.getTab());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, ITooltipFlag advanced) {
        tooltip.add("Acts as a proxy block for");
        tooltip.add("redstone. XNet can connect to this");
        tooltip.add(TextFormatting.YELLOW + "This version does a block update!");
    }

    private Set<BlockPos> loopDetector = new HashSet<>();

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if(loopDetector.add(pos)) {
            try {
                worldIn.notifyNeighborsOfStateChange(pos, this, true);
            } finally {
                loopDetector.remove(pos);
            }
        }
    }
}
