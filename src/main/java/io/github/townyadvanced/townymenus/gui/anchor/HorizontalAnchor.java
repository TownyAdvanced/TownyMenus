package io.github.townyadvanced.townymenus.gui.anchor;

public class HorizontalAnchor {
    private final int offset;

    public HorizontalAnchor(int offset) {
        this.offset = Math.min(Math.max(offset, 0), 8); // Ensure offset >= 0 & offset <= 8
    }

    public static HorizontalAnchor fromLeft(int leftOffset) {
        return new HorizontalAnchor(leftOffset);
    }

    public static HorizontalAnchor fromRight(int rightOffset) {
        return new HorizontalAnchor(8 - rightOffset);
    }

    public int offset() {
        return this.offset;
    }
}
