package nl.MenTych;

        import java.util.Base64;

public class Guard {

    public String encrypt(String content) {
        return Base64.getEncoder().encodeToString(content.getBytes());
    }

    public String decrypt(String content) {
        byte[] decodedBytes = Base64.getDecoder().decode(content);
        return new String(decodedBytes);
    }
}
