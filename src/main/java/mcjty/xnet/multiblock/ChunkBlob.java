package mcjty.xnet.multiblock;

import mcjty.lib.varia.BlockPosTools;
import mcjty.xnet.api.keys.ConsumerId;
import mcjty.xnet.api.keys.NetworkId;
import mcjty.xnet.blocks.cables.NetCableSetup;
import mcjty.xnet.init.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * All blobs in a single chunk are represented here as well
 * as how blob id's are mapped to global network id's.
 */
public class ChunkBlob {

    private final ChunkPos chunkPos;
    private final long chunkNum;
    private int lastBlobId = 0;             // Local chunk blob ID

    // Every local (chunk) blob id can be allocated to multiple global network id's
    private final Map<BlobId, Set<NetworkId>> networkMappings = new HashMap<>();

    // Every position in a chunk can be allocated to one local chunk blob id
    private final Map<IntPos, BlobId> blobAllocations = new HashMap<>();

    // These positions represent network ID providers
    private final Map<IntPos, NetworkId> networkProviders = new HashMap<>();

    // These positions represent consumers
    private final Map<IntPos, ConsumerId> networkConsumers = new HashMap<>();
    private final Map<ConsumerId, IntPos> consumerPositions = new HashMap<>();

    // Blob id are mapped to colors
    private final Map<BlobId, ColorId> blobColors = new HashMap<>();

    // Transient datastructure that caches where positions at the border
    private final Set<IntPos> cachedBorderPositions = new HashSet<>();

    // Transient datastructure that caches which consumer positions are coupled to a network
    private Map<NetworkId, Set<IntPos>> cachedConsumers = null;

    // Transient datastructure that contains all networks actually used in this chunk
    private Set<NetworkId> cachedNetworks = null;

    // Transient datastructure mapping networks to their network provider position
    private Map<NetworkId, IntPos> cachedProviders = null;

    public ChunkBlob(ChunkPos chunkPos) {
        this.chunkPos = chunkPos;
        this.chunkNum = ChunkPos.asLong(chunkPos.x, chunkPos.z);
    }

    public long getChunkNum() {
        return chunkNum;
    }

    public ChunkPos getChunkPos() {
        return chunkPos;
    }

    public BlockPos getPosition(IntPos pos) {
        return pos.toBlockPos(chunkPos);
    }

    @Nullable
    public IntPos getProviderPosition(@Nonnull NetworkId networkId) {
        if (cachedProviders == null) {
            cachedProviders = new HashMap<>();
            for (Map.Entry<IntPos, NetworkId> entry : networkProviders.entrySet()) {
                cachedProviders.put(entry.getValue(), entry.getKey());
            }
        }
        return cachedProviders.get(networkId);
    }

    @Nonnull
    public Set<NetworkId> getNetworksForPosition(IntPos pos) {
        BlobId blobId = blobAllocations.get(pos);
        if (networkMappings.containsKey(blobId)) {
            return networkMappings.get(blobId);
        } else {
            return Collections.emptySet();
        }
    }

    @Nonnull
    public Set<NetworkId> getOrCreateNetworksForPosition(IntPos pos) {
        return getMappings(blobAllocations.get(pos));
    }

    @Nonnull
    public Set<IntPos> getBorderPositions() {
        return cachedBorderPositions;
    }

    @Nullable
    public BlobId getBlobIdForPosition(@Nonnull IntPos pos) {
        return blobAllocations.get(pos);
    }

    @Nullable
    public ColorId getColorIdForPosition(@Nonnull IntPos pos) {
        BlobId blob = getBlobIdForPosition(pos);
        if (blob == null) {
            return null;
        }
        return blobColors.get(blob);
    }

    @Nonnull
    public Set<NetworkId> getNetworks() {
        if (cachedNetworks == null) {
            cachedNetworks = new HashSet<>();
            for (Set<NetworkId> networkIds : networkMappings.values()) {
                cachedNetworks.addAll(networkIds);
            }
        }
        return cachedNetworks;
    }

    @Nonnull
    private Set<NetworkId> getMappings(BlobId blobId) {
        if (!networkMappings.containsKey(blobId)) {
            networkMappings.put(blobId, new HashSet<>());
        }
        return networkMappings.get(blobId);
    }

    @Nonnull
    public Set<IntPos> getConsumersForNetwork(NetworkId network) {
        if (cachedConsumers == null) {
            cachedConsumers = new HashMap<>();
            for (Map.Entry<IntPos, ConsumerId> entry : networkConsumers.entrySet()) {
                IntPos pos = entry.getKey();
                BlobId blobId = blobAllocations.get(pos);
                Set<NetworkId> networkIds = networkMappings.get(blobId);
                if (networkIds != null) {
                    for (NetworkId net : networkIds) {
                        if (!cachedConsumers.containsKey(net)) {
                            cachedConsumers.put(net, new HashSet<>());
                        }
                        cachedConsumers.get(net).add(pos);
                    }
                }
            }
        }
        if (cachedConsumers.containsKey(network)) {
            return cachedConsumers.get(network);
        } else {
            return Collections.emptySet();
        }
    }

    public void check(World world) {
        System.out.println("Checking chunk: " + chunkPos);
        for (int cx = 0 ; cx < 16 ; cx++) {
            for (int cz = 0 ; cz < 16 ; cz++) {
                for (int cy = 0 ; cy < 256 ; cy++) {
                    BlockPos pos = chunkPos.getBlock(cx, cy, cz);
                    Block block = world.getBlockState(pos).getBlock();
                    boolean hasid = block == NetCableSetup.connectorBlock || block == NetCableSetup.advancedConnectorBlock || block == NetCableSetup.netCableBlock || block == ModBlocks.controllerBlock || block == ModBlocks.facadeBlock;
                    if (hasid != blobAllocations.containsKey(new IntPos(pos))) {
                        if (hasid) {
                            System.out.println("Allocation at " + BlockPosTools.toString(pos) + " but no cable there!");
                        } else {
                            System.out.println("Missing allocation at " + BlockPosTools.toString(pos) + "!");
                        }
                    }
                }
            }
        }
    }

    // Go over all network providers in this chunk and distribute their id's
    // to the local blob id's
    public void fixNetworkAllocations() {
        cachedConsumers = null;
        cachedNetworks = null;
        networkMappings.clear();
        for (Map.Entry<IntPos, NetworkId> entry : networkProviders.entrySet()) {
            BlobId blobId = blobAllocations.get(entry.getKey());
            getMappings(blobId).add(entry.getValue());
        }
    }

    public void clearNetworkCache() {
        cachedNetworks = null;
    }

    public Map<IntPos, NetworkId> getNetworkProviders() {
        return networkProviders;
    }

    public void createNetworkProvider(BlockPos pos, ColorId color, NetworkId networkId) {
        IntPos posId = new IntPos(pos);
        networkProviders.put(posId, networkId);
        cachedProviders = null;
        createCableSegment(pos, color);
        getMappings(blobAllocations.get(posId)).add(networkId);
    }

    public Map<IntPos, ConsumerId> getNetworkConsumers() {
        return networkConsumers;
    }

    public IntPos getConsumerPosition(ConsumerId consumerId) {
        return consumerPositions.get(consumerId);
    }

    public void createNetworkConsumer(BlockPos pos, ColorId color, ConsumerId consumer) {
        IntPos posId = new IntPos(pos);
        networkConsumers.put(posId, consumer);
        consumerPositions.put(consumer, posId);
        createCableSegment(pos, color);
    }

    // Create a cable segment. Network ids are merged if needed.
    // This method returns true if a block on the border of this chunk changed
    public void createCableSegment(BlockPos pos, ColorId color) {
        IntPos posId = new IntPos(pos);
        if (blobAllocations.containsKey(posId)) {
            // @todo
//            System.out.println("There is already a cablesegment at " + BlockPosTools.toString(pos) + "!");
            return;
        }

        Set<BlobId> ids = new HashSet<>();
        for (int p : posId.getSidePositions()) {
            if (p != -1) {
                IntPos ip = new IntPos(p);
                BlobId blobId = blobAllocations.get(ip);
                if (blobId != null) {
                    if (blobColors.get(blobId).equals(color)) {
                        ids.add(blobId);
                    }
                }
            }
        }

        if (posId.isBorder()) {
            cachedBorderPositions.add(posId);
        }

        if (ids.isEmpty()) {
            // New id
            lastBlobId++;
            BlobId blobId = new BlobId(lastBlobId);
            blobAllocations.put(posId, blobId);
            blobColors.put(blobId, color);
        } else if (ids.size() == 1) {
            // Merge with existing
            BlobId id = ids.iterator().next();
            blobAllocations.put(posId, id);
        } else {
            // Merge several blobs
            BlobId id = ids.iterator().next();
            blobAllocations.put(posId, id);
            for (Map.Entry<IntPos, BlobId> entry : blobAllocations.entrySet()) {
                if (ids.contains(entry.getValue())) {
                    IntPos p = entry.getKey();
                    blobAllocations.put(p, id);
                }
            }
            Set<NetworkId> networkIds = new HashSet<>();
            for (Map.Entry<BlobId, Set<NetworkId>> entry : networkMappings.entrySet()) {
                if (ids.contains(entry.getKey())) {
                    networkIds.addAll(entry.getValue());
                }
            }
            networkMappings.put(id, networkIds);
            cachedConsumers = null;
            cachedNetworks = null;
        }
    }

    // Remove a cable segment and return all positions on the border of this
    // chunk where something changed. Note that this function will unlink all
    // affected network Ids (except from providers) so you have to make sure
    // to traverse the network providers again.
    // Return true if a border block changed
    public boolean removeCableSegment(BlockPos pos) {
        IntPos posId = new IntPos(pos);
        if (!blobAllocations.containsKey(posId)) {
            // @todo
//            System.out.println("There is no cablesegment at " + BlockPosTools.toString(pos) + "!");
            return getBorderPositions().contains(posId);
        }

        if (networkConsumers.containsKey(posId)) {
            consumerPositions.remove(networkConsumers.get(posId));
            networkConsumers.remove(posId);
        }
        cachedConsumers = null;
        networkProviders.remove(posId);
        cachedProviders = null;
        ColorId oldColor = blobColors.get(blobAllocations.get(posId));

        int cnt = 0;
        for (int p : posId.getSidePositions()) {
            if (p != -1) {
                BlobId blobId = blobAllocations.get(new IntPos(p));
                if (blobId != null && blobColors.get(blobId).equals(oldColor)) {
                    cnt++;
                }
            }
        }

        boolean changed = false;
        blobAllocations.remove(posId);
        if (posId.isBorder()) {
            cachedBorderPositions.remove(posId);
            changed = true;
        }
        if (cnt > 1) {
            // Multiple adjacent blocks. We might need to split in multiple blobs. For
            // every adjacent block we allocate a new id:
            for (int p : posId.getSidePositions()) {
                if (p != -1) {
                    IntPos ip = new IntPos(p);
                    BlobId oldId = blobAllocations.get(ip);
                    if (oldId != null && blobColors.get(oldId).equals(oldColor)) {
                        networkMappings.remove(oldId);
                        cachedNetworks = null;
                        cachedConsumers = null;
                        lastBlobId++;
                        BlobId newId = new BlobId(lastBlobId);
                        blobColors.put(newId, oldColor);
                        changed = propagateId(ip, oldColor, oldId, newId, changed);
                    }
                }
            }
        }
        return changed;
    }

    private boolean propagateId(IntPos pos, ColorId color, BlobId oldId, BlobId newId, boolean changed) {
        blobAllocations.put(pos, newId);
        if (pos.isBorder()) {
            changed = true;
        }
        for (int p : pos.getSidePositions()) {
            if (p != -1) {
                IntPos ip = new IntPos(p);
                BlobId blobId = blobAllocations.get(ip);
                if (oldId.equals(blobId) && blobColors.get(blobId).equals(color)) {
                    changed = propagateId(ip, color, oldId, newId, changed);
                }
            }
        }
        return changed;
    }

    private String toString(IntPos pos) {
        BlockPos p = getPosition(pos);
        return pos.getX() + "," + pos.getY() + "," + pos.getZ() + " (real:" + p.getX() + "," + p.getY() + "," + p.getZ() + ")";
    }

    public void dump() {
        System.out.println("################# Chunk (" + chunkPos.x + "," + chunkPos.z + ") #################");
        System.out.println("Network providers:");
        for (Map.Entry<IntPos, NetworkId> entry : networkProviders.entrySet()) {
            System.out.println("    " + toString(entry.getKey()) + ", network = " + entry.getValue().getId());
        }
        System.out.println("Network consumers:");
        for (Map.Entry<IntPos, ConsumerId> entry : networkConsumers.entrySet()) {
            System.out.println("    " + toString(entry.getKey()) + ", consumer = " + entry.getValue().getId());
        }
        System.out.println("Network mappings:");
        for (Map.Entry<BlobId, Set<NetworkId>> entry : networkMappings.entrySet()) {
            String s = "";
            for (NetworkId networkId : entry.getValue()) {
                s += networkId.getId() + " ";
            }
            System.out.println("    Blob(" + entry.getKey().getId() + "): networks = " + s);
        }
        System.out.println("Blob colors:");
        for (Map.Entry<BlobId, ColorId> entry : blobColors.entrySet()) {
            System.out.println("    Blob(" + entry.getKey().getId() + "): color = " + entry.getValue().getId());
        }
        System.out.println("Allocations:");
        for (Map.Entry<IntPos, BlobId> entry : blobAllocations.entrySet()) {
            System.out.println("    " + toString(entry.getKey()) + ", Blob(" + entry.getValue().getId() + ")");
        }
    }

    public void readFromNBT(NBTTagCompound compound) {
        networkMappings.clear();
        blobAllocations.clear();
        networkProviders.clear();
        blobColors.clear();
        cachedBorderPositions.clear();
        cachedNetworks = null;
        cachedConsumers = null;
        cachedProviders = null;

        lastBlobId = compound.getInteger("lastBlob");
        Set<BlobId> foundBlobs = new HashSet<>();       // Keep track of blobs we found
        if (compound.hasKey("allocations")) {
            int[] allocations = compound.getIntArray("allocations");
            int idx = 0;
            while (idx < allocations.length-1) {
                IntPos pos = new IntPos(allocations[idx]);
                BlobId blob = new BlobId(allocations[idx + 1]);
                blobAllocations.put(pos, blob);
                foundBlobs.add(blob);
                if (pos.isBorder()) {
                    cachedBorderPositions.add(pos);
                }
                idx += 2;
            }
        }

        if (compound.hasKey("mappings")) {
            int[] mappings = compound.getIntArray("mappings");
            int idx = 0;
            while (idx < mappings.length-1) {
                int key = mappings[idx];
                BlobId blob = new BlobId(key);
                Set<NetworkId> ids = new HashSet<>();
                idx++;
                while (idx < mappings.length && mappings[idx] != -1) {
                    ids.add(new NetworkId(mappings[idx]));
                    idx++;
                }
                if (foundBlobs.contains(blob)) {
                    // Only add mappings if we still have allocations for the blob
                    networkMappings.put(blob, ids);
                }
                idx++;
            }
        }

        if (compound.hasKey("providers")) {
            int[] providers = compound.getIntArray("providers");
            int idx = 0;
            while (idx < providers.length-1) {
                networkProviders.put(new IntPos(providers[idx]), new NetworkId(providers[idx+1]));
                idx += 2;
            }
        }

        if (compound.hasKey("consumers")) {
            int[] consumers = compound.getIntArray("consumers");
            int idx = 0;
            while (idx < consumers.length-1) {
                IntPos intPos = new IntPos(consumers[idx]);
                ConsumerId consumerId = new ConsumerId(consumers[idx + 1]);
                networkConsumers.put(intPos, consumerId);
                consumerPositions.put(consumerId, intPos);
                idx += 2;
            }
        }

        if (compound.hasKey("colors")) {
            int[] colors = compound.getIntArray("colors");
            int idx = 0;
            while (idx < colors.length-1) {
                BlobId blob = new BlobId(colors[idx]);
                ColorId color = new ColorId(colors[idx + 1]);
                if (foundBlobs.contains(blob)) {
                    // Only add colors if we still have allocations for the blob
                    blobColors.put(blob, color);
                }
                idx += 2;
            }
        }
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("lastBlob", lastBlobId);

        List<Integer> m = new ArrayList<>();
        for (Map.Entry<BlobId, Set<NetworkId>> entry : networkMappings.entrySet()) {
            m.add(entry.getKey().getId());
            for (NetworkId v : entry.getValue()) {
                m.add(v.getId());
            }
            m.add(-1);
        }
        NBTTagIntArray mappings = new NBTTagIntArray(m.stream().mapToInt(i -> i).toArray());
        compound.setTag("mappings", mappings);

        m.clear();
        for (Map.Entry<IntPos, BlobId> entry : blobAllocations.entrySet()) {
            m.add(entry.getKey().getPos());
            m.add(entry.getValue().getId());
        }
        NBTTagIntArray allocations = new NBTTagIntArray(m.stream().mapToInt(i -> i).toArray());
        compound.setTag("allocations", allocations);

        m.clear();
        for (Map.Entry<IntPos, NetworkId> entry : networkProviders.entrySet()) {
            m.add(entry.getKey().getPos());
            m.add(entry.getValue().getId());
        }
        NBTTagIntArray providers = new NBTTagIntArray(m.stream().mapToInt(i -> i).toArray());
        compound.setTag("providers", providers);

        m.clear();
        for (Map.Entry<IntPos, ConsumerId> entry : networkConsumers.entrySet()) {
            m.add(entry.getKey().getPos());
            m.add(entry.getValue().getId());
        }
        NBTTagIntArray consumers = new NBTTagIntArray(m.stream().mapToInt(i -> i).toArray());
        compound.setTag("consumers", consumers);

        m.clear();
        for (Map.Entry<BlobId, ColorId> entry : blobColors.entrySet()) {
            m.add(entry.getKey().getId());
            m.add(entry.getValue().getId());
        }
        NBTTagIntArray colors = new NBTTagIntArray(m.stream().mapToInt(i -> i).toArray());
        compound.setTag("colors", colors);

        return compound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChunkBlob chunkBlob = (ChunkBlob) o;

        return chunkNum == chunkBlob.chunkNum;

    }

    @Override
    public int hashCode() {
        return (int) (chunkNum ^ (chunkNum >>> 32));
    }
}
