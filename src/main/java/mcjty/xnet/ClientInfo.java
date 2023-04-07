package mcjty.xnet;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * This class holds information on client-side only which are global to the mod.
 */
public class ClientInfo {
    private BlockPos hilightedBlock = null;
    private long expireHilight = 0;

    public void hilightBlock(BlockPos c, long expireHilight) {
        hilightedBlock = c;
        this.expireHilight = expireHilight;
    }

    public BlockPos getHilightedBlock() {
        return hilightedBlock;
    }

    public long getExpireHilight() {
        return expireHilight;
    }

    @SideOnly(Side.CLIENT)
    public static World getWorld() {
        return Minecraft.getMinecraft().world;
    }
}
