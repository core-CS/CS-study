public final class String implements java.io.Serializable, Comparable<String>, CharSequence {
    
    @Stable
    private final byte[] value;
    
    private final byte coder;
    
    private int hash;
    
    // ...
    
    static final byte LATIN1 = 0;
    static final byte UTF16 = 1;
    static final boolean COMPACT_STRINGS;

    
    static {
        COMPACT_STRINGS = true;
    }
    
    public int indexOf(int ch, int fromIndex) {
        return isLatin1() 
            ? StringLatin1.indexOf(value, ch, fromIndex) 
            : StringUTF16.indexOf(value, ch, fromIndex);
    }
}  

    private boolean isLatin1() {
        return COMPACT_STRINGS && coder == LATIN1;
    }
}
