package Utility;

public interface Hash {
    int run(byte[] value);
    void setSeed(byte[] seed);
}
