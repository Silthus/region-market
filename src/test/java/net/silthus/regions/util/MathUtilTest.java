package net.silthus.regions.util;

import com.sk89q.worldedit.math.BlockVector2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
class MathUtilTest {

    @Nested
    @DisplayName("calculatePolygonalArea(...)")
    class calculatePolygonalArea {

        @Test
        @DisplayName("should calculate size of simple polygon region")
        void shouldCalculateCorrectSizeOfSimpleRegion() {

            double volume = MathUtil.calculatePolygonalArea(Arrays.asList(
                    BlockVector2.at(17, 266),
                    BlockVector2.at(4, 266),
                    BlockVector2.at(4, 279),
                    BlockVector2.at(15, 279)
            ));
            assertThat(volume).isEqualTo(176);
        }

        @Test
        @DisplayName("should calculate correct size of rectangular polygon region")
        void shouldCalculateRecangularPolygonRegion() {

            double volume = MathUtil.calculatePolygonalArea(Arrays.asList(
                    BlockVector2.at(19, 263),
                    BlockVector2.at(19, 276),
                    BlockVector2.at(25, 276),
                    BlockVector2.at(25, 269),
                    BlockVector2.at(29, 269),
                    BlockVector2.at(29, 263)
                    ));
            assertThat(volume).isEqualTo(126);
        }

        @Test
        @DisplayName("should calculate correct size of a polygonal region")
        void shouldCalculateMultiPolygonRegion() {

            double volume = MathUtil.calculatePolygonalArea(Arrays.asList(
                    BlockVector2.at(33, 273),
                    BlockVector2.at(33, 283),
                    BlockVector2.at(42, 283),
                    BlockVector2.at(48, 277),
                    BlockVector2.at(48, 273)
            ));
            assertThat(volume).isEqualTo(155);
        }

        @Test
        @DisplayName("should calculate correct size of a big polygonal region")
        void shouldCalculateBigPolygonRegion() {

            double volume = MathUtil.calculatePolygonalArea(Arrays.asList(
                    BlockVector2.at(54, 270),
                    BlockVector2.at(57, 283),
                    BlockVector2.at(70, 288),
                    BlockVector2.at(72, 270)
            ));
            assertThat(volume).isEqualTo(261);
        }

        @Test
        @DisplayName("should calculate correct mathmatical size of a polygonal region")
        void shouldCalculateMathmaticalPolygonRegion() {

            double volume = MathUtil.calculatePolygonalArea(Arrays.asList(
                    BlockVector2.at(-3, 10),
                    BlockVector2.at(5, 9),
                    BlockVector2.at(8, 3),
                    BlockVector2.at(-5, -3)
            ));
            assertThat(volume).isEqualTo(101);
        }
    }
}