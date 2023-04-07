package mcjty.xnet.blocks.facade;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

public class MimicBlockSupport {

    @Nullable
    private IBlockState mimicBlock = null;

    @Nullable
    public IBlockState getMimicBlock() {
        return mimicBlock;
    }

    public void setMimicBlock(@Nullable IBlockState mimicBlock) {
        this.mimicBlock = mimicBlock;
    }


    public void readFromNBT(NBTTagCompound tagCompound) {
        if (tagCompound.hasKey("regName")) {
            String regName = tagCompound.getString("regName");
            int meta = tagCompound.getInteger("meta");
            Block value = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(regName));
            if (value == null) {
                mimicBlock = Blocks.COBBLESTONE.getDefaultState();
            } else {
                mimicBlock = value.getStateFromMeta(meta);
            }
        } else {
            mimicBlock = null;
        }
    }

    public void writeToNBT(NBTTagCompound tagCompound) {
        if (mimicBlock != null) {
            tagCompound.setString("regName", mimicBlock.getBlock().getRegistryName().toString());
            tagCompound.setInteger("meta", mimicBlock.getBlock().getMetaFromState(mimicBlock));
        }
    }
}
