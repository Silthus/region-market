package net.silthus.regions.util;

import com.sk89q.worldedit.math.BlockVector2;

import java.util.List;

public final class MathUtil {

    public static double calculatePolygonalArea(List<BlockVector2> points) {

        double sum = 0;
        for (int i = 0; i < points.size() ; i++)
        {
            BlockVector2 point = points.get(i);
            if (i == 0)
            {
                sum += point.getBlockX() * (points.get(i + 1).getBlockZ() - points.get(points.size() - 1).getBlockZ());
            }
            else if (i < points.size() - 1)
            {
                sum += point.getBlockX() * (points.get(0).getBlockZ() - points.get(i - 1).getBlockZ());
            }
            else
            {
                sum +=  point.getBlockX() * (points.get(i + 1).getBlockZ() - points.get(i - 1).getBlockZ());
            }
        }

        return 0.5 * Math.abs(sum);
    }
}
