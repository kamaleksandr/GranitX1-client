
package common;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import javax.swing.event.ChangeEvent;

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
        buf = ByteBuffer.allocate(REC_BUF_SIZE);
    }

    /**
     * @return the header
     */
    public String getHeader() {
        return header;
    }

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
    protected void OnTCPRead(ChangeEvent event) {           
        try {
            buf = Receive(buf);
        } catch (Exception ex) {
            Error(ex);
            return;
        }
        if (buf.position() == 0) {
            setStatus(StatusEnum.disconnect);
            return;
        }
        header = null;
        content_position = 0;
        content_length = 0;
        for (; content_position + 3 < buf.position(); content_position++) {
            if (buf.get(content_position) == '\r'
                    && buf.get(content_position + 1) == '\n'
                    && buf.get(content_position + 2) == '\r'
                    && buf.get(content_position + 3) == '\n') {
                header = new String(buf.array(), 0, content_position, StandardCharsets.US_ASCII);
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
            buf.clear();
            return;
        }  
        Scanner scanner = new Scanner(header.substring(index + 16));
        if (scanner.hasNextInt()) {
            content_length = scanner.nextInt();
        }
        if (content_length + content_position > REC_BUF_SIZE) {
            Error( new Exception("Overload REC_BUF_SIZE") );
            buf.clear();
            return;
        }
        if (buf.position() >= content_length + content_position) {
            Packet();
            buf.clear();
        }
    }
}
