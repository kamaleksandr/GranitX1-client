package common;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 *
 * @author kamyshev.a
 */
public class HTTPChannel extends AbstractChannel {

    private static final int REC_BUF_SIZE = 102400;
    private int content_position, content_length;
    private String header;

    public HTTPChannel(SocketAddress address) throws Exception {
        super(address);
        buffer = ByteBuffer.allocate(REC_BUF_SIZE);
    }

    /**
     * @return the header
     */
    public String getHeader() {
        return header;
    }

    /**
     *
     * @return Input ByteBuffer
     */
    //public ByteBuffer getInputBuffer() {
    //    return buffer;
    //}

    /**
     * @return the content position
     */
    public int getContentPosition() {
        return content_position;
    }

    /**
     * @return the content length
     */
    public int getContentLength() {
        return content_length;
    }

    @Override
    protected void OnTCPRead(Object object) {
        try {
            Receive(buffer);
        } catch (Exception ex) {
            Error(ex);
            return;
        }
        if (buffer.position() == 0) {
            setStatus(StatusEnum.disconnect);
            return;
        }
        header = null;
        content_position = 0;
        content_length = 0;
        for (; content_position + 3 < buffer.position(); content_position++) {
            if (buffer.get(content_position) == '\r'
                    && buffer.get(content_position + 1) == '\n'
                    && buffer.get(content_position + 2) == '\r'
                    && buffer.get(content_position + 3) == '\n') {
                header = new String(buffer.array(), 0, content_position, StandardCharsets.US_ASCII);
                break;
            }
        }
        if (header == null) {
            return;
        }
        content_position += 4;
        int index = header.indexOf("Content-Length: ");
        if (index == -1) {
            Packet();
            buffer.clear();
            return;
        }
        Scanner scanner = new Scanner(header.substring(index + 16));
        if (scanner.hasNextInt()) {
            content_length = scanner.nextInt();
        }
        if (content_length + content_position > REC_BUF_SIZE) {
            Error(new Exception("Overload REC_BUF_SIZE"));
            buffer.clear();
            return;
        }
        if (buffer.position() >= content_length + content_position) {
            Packet();
            buffer.clear();
        }
    }
}
