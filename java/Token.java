/**
 * Token.java - Represents a work unit circulating through a ring.
 *
 * REASONING: The spec requires each token to carry token_id, ring_id, orig_input,
 * current_val (mutable), and remaining_hops. We use a record-like class with
 * mutable fields for current_val and remaining_hops since these change at each hop.
 * token_id is assigned by the Coordinator to uniquely identify each input for
 * completion reporting. ring_id identifies which of the four rings is processing
 * this token (useful for completion messages).
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
