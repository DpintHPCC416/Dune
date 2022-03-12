package Utility;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Md5 implements Hash{
    private MessageDigest messageDigest;
    private byte[] seed;
    private BigInteger modifier;

    public Md5()
    {
        seed = Util.long2Bytes(new Random(System.currentTimeMillis()).nextLong());
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        modifier = new BigInteger(String.valueOf(((long)1 << 31)));
    }

    public void setSeed(byte[] seed)
    {
        this.seed = seed;
    }
    @Override
    public int run(byte[] value) {
        messageDigest.update(value);
        messageDigest.update(seed);
        byte[] result = messageDigest.digest();
        return new BigInteger(1, result).mod(modifier).intValue();
    }
}
