package io.github.townyadvanced.townymenus.gui.slot.anchor;

import io.github.townyadvanced.townymenus.utils.Mth;

public class VerticalAnchor {
    private final int offset;

    public VerticalAnchor(int offset) {
        this.offset = Mth.clamp(offset, 0, 5);
    }

    public static VerticalAnchor fromTop(int topOffset) {
        return new VerticalAnchor(topOffset);
    }

    public static VerticalAnchor fromBottom(int bottomOffset) {
        return new VerticalAnchor(5 - bottomOffset);
    }

    public int resolveY(int size) {
        int rows = (int) Math.ceil(size / 9d);

        return Math.min(rows - 1, this.offset) * 9;
    }
}
