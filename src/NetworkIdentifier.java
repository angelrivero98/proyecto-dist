public class NetworkIdentifier {
    public String ipAddress;
    public int port;

    public NetworkIdentifier(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Recibe un string del formato: {ip}:{puerto} y genera una instancia de NetworkIdentifier
     */
    public NetworkIdentifier(String serializedContent) {
        String[] components = serializedContent.split(":");
        this.ipAddress = components[0];
        this.port = new Integer(components[1]);
    }

    @Override
    public String toString() {
        return String.format("%s:%d", ipAddress, port);
    }
}
