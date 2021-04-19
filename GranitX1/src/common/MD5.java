package common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 *
 * @author kamyshev.a
 */
public class MD5 {

    static public String Hash(String src) {
        MessageDigest md;
        String dst = "";
        try {
            md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(src.getBytes());
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException ex) {
        }
        return dst;
    }

}
