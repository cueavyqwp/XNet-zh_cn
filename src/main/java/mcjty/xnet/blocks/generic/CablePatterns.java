package mcjty.xnet.blocks.generic;

import mcjty.xnet.blocks.cables.ConnectorType;

import java.util.HashMap;
import java.util.Map;

import static mcjty.xnet.blocks.cables.ConnectorType.NONE;

public class CablePatterns {

    static final Map<Pattern, QuadSetting> PATTERNS = new HashMap<>();

    public static QuadSetting findPattern(ConnectorType s1, ConnectorType s2, ConnectorType s3, ConnectorType s4) {
        return PATTERNS.get(new Pattern(s1 != NONE, s2 != NONE, s3 != NONE, s4 != NONE));
    }

    public enum SpriteIdx {
        SPRITE_NONE,
        SPRITE_END,
        SPRITE_STRAIGHT,
        SPRITE_CORNER,
        SPRITE_THREE,
        SPRITE_CROSS
    }

    public static class QuadSetting {
        private final SpriteIdx sprite;
        private final int rotation;

        public QuadSetting(SpriteIdx sprite, int rotation) {
            this.rotation = rotation;
            this.sprite = sprite;
        }

        public SpriteIdx getSprite() {
            return sprite;
        }

        public int getRotation() {
            return rotation;
        }
    }

    public static class Pattern {
        private final boolean s1;
        private final boolean s2;
        private final boolean s3;
        private final boolean s4;

        public Pattern(boolean s1, boolean s2, boolean s3, boolean s4) {
            this.s1 = s1;
            this.s2 = s2;
            this.s3 = s3;
            this.s4 = s4;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Pattern pattern = (Pattern) o;

            return s1 == pattern.s1 && s2 == pattern.s2 && s3 == pattern.s3 && s4 == pattern.s4;

        }

        @Override
        public int hashCode() {
            int result = (s1 ? 1 : 0);
            result = 31 * result + (s2 ? 1 : 0);
            result = 31 * result + (s3 ? 1 : 0);
            result = 31 * result + (s4 ? 1 : 0);
            return result;
        }
    }
}
