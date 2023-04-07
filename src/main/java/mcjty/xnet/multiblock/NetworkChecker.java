package mcjty.xnet.multiblock;

import mcjty.xnet.api.keys.NetworkId;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Keep track of the version numbers of a number of networks and generate
 * a unique hash of that
 */
public class NetworkChecker {

    private final Set<NetworkId> networks = new HashSet<>();
    private long lastHash = -1;

    public void add(@Nullable NetworkId networkId) {
        if (networkId != null) {
            networks.add(networkId);
        }
    }

    public void add(Collection<NetworkId> networks) {
        this.networks.addAll(networks);
    }

    public void add(NetworkChecker checker) {
        networks.addAll(checker.networks);
    }

    public void remove(NetworkId networkId) {
        networks.remove(networkId);
    }

    private long hash(WorldBlob worldBlob) {
        long h = 0;

        for (NetworkId network : networks) {
            h = Long.rotateLeft(h, 3);
            h ^= worldBlob.getNetworkVersion(network);
        }

        return h;
    }

    /**
     * Return true if this checker is dirty. This will also mark it
     * as clean so you can only call this once!
     */
    public boolean isDirtyAndMarkClean(WorldBlob worldBlob) {
        long h = hash(worldBlob);
        if (h != lastHash) {
            lastHash = h;
            return true;
        }
        return false;
    }

    public void dump() {
        for (NetworkId network : networks) {
            System.out.println("network = " + network);
        }
    }

    public Collection<NetworkId> getAffectedNetworks() {
        return networks;
    }
}
