package io.github.townyadvanced.townymenus.gui.anchor;

import org.jetbrains.annotations.Nullable;

public class SlotAnchor {
    private static final SlotAnchor BOTTOM_RIGHT = of(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromRight(0));

    private final VerticalAnchor verticalAnchor;
    private final HorizontalAnchor horizontalAnchor;
    private @Nullable Integer exact;

    public SlotAnchor(VerticalAnchor verticalAnchor, HorizontalAnchor horizontalAnchor) {
        this.verticalAnchor = verticalAnchor;
        this.horizontalAnchor = horizontalAnchor;
    }

    public SlotAnchor(int exact) {
        this.exact = exact;
        this.verticalAnchor = null;
        this.horizontalAnchor = null;
    }

    public static SlotAnchor of(VerticalAnchor verticalAnchor, HorizontalAnchor horizontalAnchor) {
        return new SlotAnchor(verticalAnchor, horizontalAnchor);
    }

    public static SlotAnchor ofExact(int slot) {
        return new SlotAnchor(slot);
    }

    public static SlotAnchor bottomRight() {
        return BOTTOM_RIGHT;
    }

    public int resolveSlot(int size) {
        if (exact != null)
            return exact;

        return verticalAnchor.resolveY(size) + horizontalAnchor.offset();
    }
}
