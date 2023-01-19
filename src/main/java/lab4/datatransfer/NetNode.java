package lab4.datatransfer;

import lombok.Getter;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class NetNode implements Serializable {
    private static final int MIN_PORT = 1025;
    private static final int MAX_PORT = 65535;

    @Getter private final InetAddress address;
    @Getter private final int port;

    public NetNode(InetAddress address, int port) {
        this.address = address;
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("Port must be in range [" + MIN_PORT + ", " + MAX_PORT + "]");
        }
        this.port = port;
    }

    public NetNode(String address, int port) throws UnknownHostException {
        this(InetAddress.getByName(address), port);
    }


    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof NetNode other)) {
            return false;
        }
        return (port == other.port) && address.equals(other.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port);
    }

    @Override
    public String toString() {
        return String.format("NetNode{%s:%d}", address, port);
    }
}
