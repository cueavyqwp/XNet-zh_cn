package mcjty.xnet.multiblock;

import mcjty.lib.worlddata.AbstractWorldData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class XNetBlobData extends AbstractWorldData<XNetBlobData> {

    private static final String NAME = "XNetBlobData";

    private final Map<Integer, WorldBlob> worldBlobMap = new HashMap<>();

    public XNetBlobData(String name) {
        super(name);
    }

    @Override
    public void clear() {
        worldBlobMap.clear();
    }

    @Nonnull
    public static XNetBlobData getBlobData(World world) {
        return getData(world, XNetBlobData.class, NAME);
    }

    public WorldBlob getWorldBlob(World world) {
        return getWorldBlob(world.provider.getDimension());
    }

    public WorldBlob getWorldBlob(int dimId) {
        if (!worldBlobMap.containsKey(dimId)) {
            worldBlobMap.put(dimId, new WorldBlob(dimId));
        }
        return worldBlobMap.get(dimId);
    }


    @Override
    public void readFromNBT(NBTTagCompound compound) {
        worldBlobMap.clear();
        if (compound.hasKey("worlds")) {
            NBTTagList worlds = (NBTTagList) compound.getTag("worlds");
            for (int i = 0 ; i < worlds.tagCount() ; i++) {
                NBTTagCompound tc = (NBTTagCompound) worlds.get(i);
                int id = tc.getInteger("dimid");
                WorldBlob blob = new WorldBlob(id);
                blob.readFromNBT(tc);
                worldBlobMap.put(id, blob);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Integer, WorldBlob> entry : worldBlobMap.entrySet()) {
            WorldBlob blob = entry.getValue();
            NBTTagCompound tc = new NBTTagCompound();
            tc.setInteger("dimid", blob.getDimId());
            blob.writeToNBT(tc);
            list.appendTag(tc);
        }
        compound.setTag("worlds", list);

        return compound;
    }
}
