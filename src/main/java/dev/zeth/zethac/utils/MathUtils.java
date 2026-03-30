package dev.zeth.zethac.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class MathUtils {
    public static double distance3D(Location a, Location b) {
        return Math.sqrt(Math.pow(a.getX()-b.getX(),2)+Math.pow(a.getY()-b.getY(),2)+Math.pow(a.getZ()-b.getZ(),2));
    }
    public static double distanceH(Location a, Location b) {
        return Math.sqrt(Math.pow(a.getX()-b.getX(),2)+Math.pow(a.getZ()-b.getZ(),2));
    }
    public static double angleTo(Player p, Location target) {
        Vector dir = p.getLocation().getDirection();
        Vector toTarget = target.toVector().subtract(p.getLocation().toVector()).normalize();
        return Math.toDegrees(dir.angle(toTarget));
    }
    public static double clamp(double val, double min, double max) { return Math.max(min, Math.min(max, val)); }
    public static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    public static double gcd(double a, double b) { while (b > 1e-10) { double t = b; b = a%b; a = t; } return a; }
    public static double stdDev(List<Double> vals) {
        double avg = vals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return Math.sqrt(vals.stream().mapToDouble(v -> Math.pow(v-avg,2)).average().orElse(0));
    }
    public static float wrapAngle(float a) { a %= 360; if (a > 180) a -= 360; if (a < -180) a += 360; return a; }
}
