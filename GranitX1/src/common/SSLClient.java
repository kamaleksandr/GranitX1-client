package common;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author kamyshev.a
 */
public class SSLClient {

    private static final String SSL_PROTOCOL_NAME = "TLSv1.2";
    private final SSLEngine engine;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    protected SSLEngineResult result;

    private static class AcceptAllTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            X509Certificate[] certificate = new X509Certificate[0];
            return certificate;
        }
    }

    SSLClient() throws Exception {
        SSLContext context = SSLContext.getInstance(SSL_PROTOCOL_NAME);
        context.init(new KeyManager[0], new TrustManager[]{
            new AcceptAllTrustManager()}, new SecureRandom());
        engine = context.createSSLEngine();
        engine.setUseClientMode(true);
        result = new SSLEngineResult(SSLEngineResult.Status.CLOSED,
                HandshakeStatus.NOT_HANDSHAKING, 0, 0);
    }

    public HandshakeStatus getStatus() {
        return engine.getHandshakeStatus();
    }

    /**
     * @return the buf_size
     */
    public int getBufSize() {
        return engine.getSession().getPacketBufferSize();
    }

    public void BeginHandshake() throws SSLException {
        engine.beginHandshake();
    }

    public void Unwrap(ByteBuffer src, ByteBuffer dst) throws Exception {
        result = engine.unwrap(src, dst);
        switch (result.getStatus()) {
            case OK:
                break;
            case BUFFER_OVERFLOW:
                throw new Exception("SSL client: Buffer overflow");
            case BUFFER_UNDERFLOW:
                throw new Exception("SSL client: Buffer underflow");
            case CLOSED:
                throw new Exception("SSL client: Closed");
        }
    }

    public void Wrap(ByteBuffer src, ByteBuffer dst) throws Exception {
        result = engine.wrap(src, dst);
        switch (result.getStatus()) {
            case OK:
                break;
            case BUFFER_OVERFLOW:
                throw new Exception("SSL client: Buffer overflow");
            case BUFFER_UNDERFLOW:
                throw new Exception("SSL client: Buffer underflow");
            case CLOSED:
                throw new Exception("SSL client: Closed");
        }
    }

    void DelegateTask() {
        Runnable task;
        while ((task = engine.getDelegatedTask()) != null) {
            executor.execute(task);
        }
    }
}
