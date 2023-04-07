package mcjty.xnet.logic;

public class VersionNumber {

    private int version;

    public VersionNumber(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void inc() {
        version++;
    }
}
