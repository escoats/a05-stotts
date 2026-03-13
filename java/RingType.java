/**
 * RingType.java - Enum for the four ring types with their transformation rules.
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
