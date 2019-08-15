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
    private final int buf_size;

    protected ExecutorService executor = Executors.newSingleThreadExecutor();

    private static class AcceptAllTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    SSLClient() throws Exception {
        SSLContext context = SSLContext.getInstance(SSL_PROTOCOL_NAME);
        context.init(new KeyManager[0], new TrustManager[]{new AcceptAllTrustManager()}, new SecureRandom());
        engine = context.createSSLEngine();
        engine.setUseClientMode(true);
        buf_size = engine.getSession().getPacketBufferSize();
    }

    public HandshakeStatus getStatus() {
        return engine.getHandshakeStatus();
    }

    /**
     * @return the buf_size
     */
    public int getBuf_size() {
        return buf_size;
    }

    public void BeginHandshake() throws SSLException {
        engine.beginHandshake();
    }

    private void NotOkResult(SSLEngineResult result) throws Exception {
        switch (result.getStatus()) {
            case BUFFER_OVERFLOW:
                throw new Exception("SSL client: Buffer overflow");
            case BUFFER_UNDERFLOW:
                throw new Exception("SSL client: Buffer underflow");
            case CLOSED:
                throw new Exception("SSL client: Closed");
        }
    }

    public ByteBuffer Unwrap(ByteBuffer data) throws Exception {
        ByteBuffer unwrap_data = ByteBuffer.allocate(data.capacity());
        SSLEngineResult result;
        result = engine.unwrap(data, unwrap_data);
        if (result.getStatus() == SSLEngineResult.Status.OK) {
            return unwrap_data;
        } else {
            NotOkResult(result);
        }
        return null;
    }

    public ByteBuffer Wrap(ByteBuffer data) throws Exception {

        ByteBuffer wrap_data;
        if (data.capacity() > buf_size) {
            wrap_data = ByteBuffer.allocate(data.capacity());
        } else {
            wrap_data = ByteBuffer.allocate(buf_size);
        }
        SSLEngineResult result;
        result = engine.wrap(data, wrap_data);
        if (result.getStatus() == SSLEngineResult.Status.OK) {
            return wrap_data;
        } else {
            NotOkResult(result);
        }
        return null;
    }

    void DelegateTask() {
        Runnable task;
        while ((task = engine.getDelegatedTask()) != null) {
            executor.execute(task);
        }
    }
}
