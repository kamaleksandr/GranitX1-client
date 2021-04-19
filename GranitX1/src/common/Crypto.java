package common;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author kamyshev.a
 */
public class Crypto {

    private final byte[] key_bytes = {
        30, -92, -40, 107, 33, -116, 33, -34,
        42, -11, 78, -126, 81, -20, -92, -93,
        50, 47, -56, -3, 103, 16, -63, 58,
        -127, 108, -17, -46, -102, 60, 88, -62
    };
    private final Cipher cipher;
    private final SecretKeySpec key;

    public Crypto() throws NoSuchAlgorithmException, NoSuchPaddingException {
        cipher = Cipher.getInstance("AES");
        key = new SecretKeySpec(key_bytes, "AES");
    }

    public String Encode(String src) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] buf = cipher.doFinal(src.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(buf);
        } catch (UnsupportedEncodingException | InvalidKeyException | 
                BadPaddingException | IllegalBlockSizeException ex) {
            return "";
        }
    }

    public String Decode(String src) {
        try {
            byte[] buf = Base64.getDecoder().decode(src);
            cipher.init(Cipher.DECRYPT_MODE, key);
            buf = cipher.doFinal(buf);
            return new String(buf, "UTF-8");
        } catch (UnsupportedEncodingException | InvalidKeyException | 
                BadPaddingException | IllegalBlockSizeException ex) {
            return "";
        }
    }
}
