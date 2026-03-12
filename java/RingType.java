/**
 * RingType.java - Enum for the four ring types with their transformation rules.
 *
 * REASONING: The spec defines four rings (NEG, ZERO, POS_EVEN, POS_ODD) each
 * with a distinct arithmetic transformation. Centralizing these in an enum
 * avoids switch/if-else sprawl and makes it easy to add new rings. Java long
 * overflow is explicitly allowed per the spec ("Java long overflow is acceptable"),
 * so we use long arithmetic without explicit wraparound handling.
 */
public enum RingType {
    NEG {
        @Override
        public long transform(long v) {
            return v * 3 + 1;
        }
    },
    ZERO {
        @Override
        public long transform(long v) {
            return v + 7;
        }
    },
    POS_EVEN {
        @Override
        public long transform(long v) {
            return v * 101;
        }
    },
    POS_ODD {
        @Override
        public long transform(long v) {
            return v * 101 + 1;
        }
    };

    public abstract long transform(long v);
}
