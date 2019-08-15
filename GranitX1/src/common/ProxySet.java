package common;

import java.net.InetSocketAddress;
import java.util.Base64;

/**
 *
 * @author kamyshev.a
 */
public class ProxySet {

    private String address;
    private int port;
    private String login;
    private String password;  

    public ProxySet() {
    }

    /**
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return InetSocketAddress
     */
    public InetSocketAddress getSocketAddress() {
        return new InetSocketAddress(address, port);
    }

    /**
     * @param login the login to set
     */
    public void setLogin(String login) {
        this.login = login;
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
