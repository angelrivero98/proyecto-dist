package exceptions;

public class NodeShutDownException extends RuntimeException {
    public NodeShutDownException(String message) {
        super(message);
    }
}
