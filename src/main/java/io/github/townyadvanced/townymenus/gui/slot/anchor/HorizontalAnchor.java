package io.github.townyadvanced.townymenus.gui.slot.anchor;

import io.github.townyadvanced.townymenus.utils.Mth;

public class HorizontalAnchor {
    private final int offset;

    public HorizontalAnchor(int offset) {
        this.offset = Mth.clamp(offset, 0, 8);
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
