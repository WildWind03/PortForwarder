import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class MyByteBuffer {
    private static final Logger logger = Logger.getLogger(MyByteBuffer.class.getName());

    private ByteBuffer byteBuffer;
    private boolean isReadMode;

    public MyByteBuffer(ByteBuffer byteBuffer, boolean isReadMode) {
        this.byteBuffer = byteBuffer;
        this.isReadMode = isReadMode;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public boolean isReadMode() {
        return isReadMode;
    }

    public void changeMode(boolean isReadMode) {
        this.isReadMode = isReadMode;
    }
}
