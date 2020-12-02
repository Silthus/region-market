package net.silthus.regions.util;

import com.sk89q.worldedit.math.BlockVector2;

import java.util.List;

public final class MathUtil {

    public static double calculatePolygonalArea(List<BlockVector2> points) {

        // https://www.wikihow.com/Calculate-the-Area-of-a-Polygon#Finding-the-Area-of-Irregular-Polygons

        BlockVector2[] vectors = points.toArray(new BlockVector2[0]);

        int sum = 0;
        for (int i = 0; i < vectors.length ; i++)
        {
            int xSum = 0;
            int zSum = 0;
            if (i < vectors.length - 1) {
                xSum = vectors[i].getX() * vectors[i + 1].getZ();
                zSum = vectors[i].getZ() * vectors[i + 1].getX();
            } else if (i == vectors.length - 1) {
                xSum = vectors[i].getX() * vectors[0].getZ();
                zSum = vectors[i].getZ() * vectors[0].getX();
            }
            sum += xSum - zSum;
        }

        return Math.abs(sum) / 2.0;
    }
}
