package common;

import java.net.InetSocketAddress;
import java.util.Base64;

/**
 *
 * @author kamyshev.a
 */
public class ProxySet {

    public String address;
    public Integer port;
    public String login;
    public String password;  

    public ProxySet() {
    }

    /**
     * @return InetSocketAddress
     */
    public InetSocketAddress getSocketAddress() {
        return new InetSocketAddress(address, port);
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public String getBasicAuthorization() {
        String s = login + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(s.getBytes());
    }

}
