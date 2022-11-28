package io.github.townyadvanced.townymenus.gui.anchor;

public class SlotAnchor {
    private final VerticalAnchor verticalAnchor;
    private final HorizontalAnchor horizontalAnchor;

    public SlotAnchor(VerticalAnchor verticalAnchor, HorizontalAnchor horizontalAnchor) {
        this.verticalAnchor = verticalAnchor;
        this.horizontalAnchor = horizontalAnchor;
    }

    public static SlotAnchor anchor(VerticalAnchor verticalAnchor, HorizontalAnchor horizontalAnchor) {
        return new SlotAnchor(verticalAnchor, horizontalAnchor);
    }

    public static SlotAnchor ofSlot(int slot) {
        return anchor(VerticalAnchor.fromTop(slot / 9), HorizontalAnchor.fromLeft(slot % 9));
    }

    public static SlotAnchor bottomRight() {
        return anchor(VerticalAnchor.fromBottom(0), HorizontalAnchor.fromRight(0));
    }

    public int resolveSlot(int size) {
        return verticalAnchor.resolveY(size) + horizontalAnchor.offset();
    }
}
