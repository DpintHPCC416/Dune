package Utility;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Sha256 implements Hash{
    private MessageDigest messageDigest;
    private byte[] seed;
    private BigInteger modifier;
    public Sha256()
    {
        seed = Util.long2Bytes(new Random(System.currentTimeMillis()).nextLong());
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        modifier = new BigInteger(String.valueOf(((long)1 << 31)));
    }


    @Override
    public int run(byte[] value) {
        messageDigest.update(value);
        messageDigest.update(seed);
        byte[] result = messageDigest.digest();
        return new BigInteger(1, result).mod(modifier).abs().intValue();
    }

    @Override
    public void setSeed(byte[] seed) {
        this.seed = seed;
    }
}
