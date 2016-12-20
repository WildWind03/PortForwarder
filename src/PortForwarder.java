import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

public class PortForwarder {
    public static final int BUFFER_SIZE = 1024;

    private static final Logger logger = Logger.getLogger(PortForwarder.class.getName());
    private InetSocketAddress rInetSocketAddress;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;


    public PortForwarder(int lport, String rhost, int rport) throws IOException {
        rInetSocketAddress = new InetSocketAddress(rhost, rport);
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(lport));
        serverSocketChannel.configureBlocking(false);

        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void start() throws IOException {
        while (true) {
            selector.select();

            Set<SelectionKey> selectionKeySet = selector.selectedKeys();

            Iterator<SelectionKey> iterator = selectionKeySet.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();

                if (!selectionKey.isValid()) {
                    iterator.remove();
                    continue;
                }

                if (selectionKey.isAcceptable()) {
                    ByteBuffer byteBufferToServer = ByteBuffer.allocate(BUFFER_SIZE);
                    ByteBuffer byteBufferToClient = ByteBuffer.allocate(BUFFER_SIZE);
                    MyByteBuffer myByteBufferToServer = new MyByteBuffer(byteBufferToServer, false);
                    MyByteBuffer myByteBufferToClient = new MyByteBuffer(byteBufferToClient, false);

                    SocketChannel clientChannel = serverSocketChannel.accept();
                    clientChannel.configureBlocking(false);

                    SocketChannel serverChannel = SocketChannel.open();
                    serverChannel.configureBlocking(false);

                    SelectionKey clientSelectionKey;

                    boolean connectResult = serverChannel.connect(rInetSocketAddress);
                    SelectionKey serverSelectionKey;
                    if (!connectResult) {
                        serverSelectionKey = serverChannel.register(selector, SelectionKey.OP_CONNECT);
                        clientSelectionKey = clientChannel.register(selector, 0);
                    } else {
                        serverSelectionKey = serverChannel.register(selector, SelectionKey.OP_READ);
                        clientSelectionKey = clientChannel.register(selector, SelectionKey.OP_READ);
                    }

                    Connection clientConnection = new Connection(clientChannel, serverChannel, myByteBufferToClient, myByteBufferToServer, serverSelectionKey);
                    Connection serverConnection = new Connection(serverChannel, clientChannel, myByteBufferToServer, myByteBufferToClient, clientSelectionKey);

                    clientSelectionKey.attach(clientConnection);
                    serverSelectionKey.attach(serverConnection);
                }

                if (selectionKey.isConnectable()) {
                    Connection connection = (Connection) selectionKey.attachment();
                    boolean connect_result = connection.getMyChannel().finishConnect();

                    if (!connect_result) {
                        logger.info("Can not connect");
                    }

                    int newOps = 0;
                    if (connection.getSourceBuffer().getByteBuffer().hasRemaining()) {
                        newOps = SelectionKey.OP_WRITE;
                    }

                    newOps = newOps | SelectionKey.OP_READ;
                    selectionKey.interestOps(newOps);
                }

                if (selectionKey.isReadable()) {
                    Connection connection = (Connection) selectionKey.attachment();
                    ByteBuffer destinationBuffer = connection.getDestinationBuffer().getByteBuffer();

                    if (connection.getDestinationBuffer().isReadMode()) {
                        destinationBuffer.compact();
                        connection.getDestinationBuffer().changeMode(false);
                    }


                    int result_of_reading = connection.getMyChannel().read(destinationBuffer);

                    byte[] bytes = destinationBuffer.array();


                    if (-1 == result_of_reading) {
                        connection.getFriendChannel().close();
                        connection.getMyChannel().close();
                        selectionKey.cancel();
                        connection.getFriendSelectionKey().cancel();
                        continue;
                    } else {
                        if (!destinationBuffer.hasRemaining()) {
                            selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_READ);
                        }

                        connection.getFriendSelectionKey().interestOps(connection.getFriendSelectionKey().interestOps() | SelectionKey.OP_WRITE);
                    }
                }

                if (selectionKey.isWritable()) {
                    Connection connection = (Connection) selectionKey.attachment();
                    ByteBuffer readBuffer = connection.getSourceBuffer().getByteBuffer();

                    if (!connection.getSourceBuffer().isReadMode()) {
                        readBuffer.flip();
                        connection.getSourceBuffer().changeMode(true);
                    }

                    try {
                        connection.getMyChannel().write(readBuffer);
                    } catch (IOException e) {
                        connection.getFriendChannel().close();
                        connection.getMyChannel().close();
                        selectionKey.cancel();
                        connection.getFriendSelectionKey().cancel();
                        continue;
                    }

                    if (!readBuffer.hasRemaining()) {
                        selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                    }

                    connection.getFriendSelectionKey().interestOps(connection.getFriendSelectionKey().interestOps() | SelectionKey.OP_READ);
                }

                iterator.remove();
            }
        }
    }
}
