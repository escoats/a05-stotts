/**
 * Token.java - Represents a work unit circulating through a ring.
 */
public class Token {
    public final long tokenId;
    public final String ringId;
    public final long origInput;
    public long currentVal;
    public int remainingHops;

    public Token(long tokenId, String ringId, long origInput, int totalHops) {
        this.tokenId = tokenId;
        this.ringId = ringId;
        this.origInput = origInput;
        this.currentVal = origInput;
        this.remainingHops = totalHops;
    }
}
