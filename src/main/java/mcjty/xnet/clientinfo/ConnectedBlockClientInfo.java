package mcjty.xnet.clientinfo;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.NetworkTools;
import mcjty.xnet.api.keys.SidedPos;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;

public class ConnectedBlockClientInfo {

    /// The position of the block we are connecting too
    @Nonnull
    private final SidedPos pos;
    /// The itemstack representing the block
    @Nonnull private final ItemStack connectedBlock;

    /// The name of the connector
    @Nonnull private final String name;

    /// The name of the block
    @Nonnull private final String blockName;

    public ConnectedBlockClientInfo(@Nonnull SidedPos pos, @Nonnull ItemStack connectedBlock, @Nonnull String name) {
        this.pos = pos;
        this.connectedBlock = connectedBlock;
        this.name = name;
        this.blockName = getStackUnlocalizedName(connectedBlock);
    }

    public ConnectedBlockClientInfo(@Nonnull ByteBuf buf) {
        pos = new SidedPos(NetworkTools.readPos(buf), EnumFacing.VALUES[buf.readByte()]);
        connectedBlock = NetworkTools.readItemStack(buf);
        name = NetworkTools.readStringUTF8(buf);
        blockName = NetworkTools.readStringUTF8(buf);
    }

    public void writeToBuf(@Nonnull ByteBuf buf) {
        NetworkTools.writePos(buf, pos.getPos());
        buf.writeByte(pos.getSide().ordinal());
        NetworkTools.writeItemStack(buf, connectedBlock);
        NetworkTools.writeStringUTF8(buf, name);
        NetworkTools.writeStringUTF8(buf, blockName);
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public String getBlockUnlocName() {
        return blockName;
    }

    @Nonnull
    public SidedPos getPos() {
        return pos;
    }

    @Nonnull
    public ItemStack getConnectedBlock() {
        return connectedBlock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectedBlockClientInfo that = (ConnectedBlockClientInfo) o;

        if (!pos.equals(that.pos)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    private static String getStackUnlocalizedName(ItemStack stack) {
        NBTTagCompound nbttagcompound = getSubCompound(stack, "display");

        if (nbttagcompound != null) {
            if (nbttagcompound.hasKey("Name", 8)) {
                return nbttagcompound.getString("Name");
            }

            if (nbttagcompound.hasKey("LocName", 8)) {
                return nbttagcompound.getString("LocName");
            }
        }

        return stack.getItem().getUnlocalizedName(stack) + ".name";
    }

    private static NBTTagCompound getSubCompound(ItemStack stack, String key) {
        if (stack.getTagCompound() != null && stack.getTagCompound().hasKey(key, 10)) {
            return stack.getTagCompound().getCompoundTag(key);
        } else {
            return null;
        }
    }

}
