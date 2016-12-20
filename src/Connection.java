import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

public class Connection {

    private static final Logger logger = Logger.getLogger(Connection.class.getName());

    private SocketChannel myChannel;
    private SocketChannel friendChannel;
    private MyByteBuffer destinationBuffer;
    private MyByteBuffer sourceBuffer;

    private SelectionKey friendSelectionKey;

    public Connection(SocketChannel myChannel, SocketChannel friendChannel, MyByteBuffer readBuffer, MyByteBuffer writeBuffer, SelectionKey friendSelectionKey) {
        this.myChannel = myChannel;
        this.destinationBuffer = readBuffer;
        this.sourceBuffer = writeBuffer;
        this.friendChannel = friendChannel;
        this.friendSelectionKey = friendSelectionKey;
    }

    public SocketChannel getFriendChannel() {
        return friendChannel;
    }

    public SocketChannel getMyChannel() {
        return myChannel;
    }

    public MyByteBuffer getDestinationBuffer() {
        return destinationBuffer;
    }

    public MyByteBuffer getSourceBuffer() {
        return sourceBuffer;
    }

    public SelectionKey getFriendSelectionKey() {
        return friendSelectionKey;
    }
}
