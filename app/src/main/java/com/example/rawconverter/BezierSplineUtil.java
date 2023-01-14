package com.example.rawconverter;
import com.example.rawconverter.utils.Point;

// From: https://stackoverflow.com/questions/22763632/construct-spline-with-android-graphics-path
public class BezierSplineUtil {

    /**
     * Get open-ended bezier spline control points.
     *
     * @param knots bezier spline points.
     * @return [2 x knots.length - 1] matrix. First row of the matrix = first
     *         control points. Second row of the matrix = second control points.
     * @throws IllegalArgumentException if less than two knots are passed.
     */
    public static Point[][] getCurveControlPoints(Point[] knots) {
        if (knots == null || knots.length < 2) {
            throw new IllegalArgumentException("At least two knot points are required");
        }

        final int n = knots.length - 1;
        final Point[] firstControlPoints = new Point[n];
        final Point[] secondControlPoints = new Point[n];

        // Special case: bezier curve should be a straight line
        if (n == 1) {
            // 3P1 = 2P0 + P3
            float x = (2 * knots[0].x + knots[1].x) / 3;
            float y = (2 * knots[0].y + knots[1].y) / 3;
            firstControlPoints[0] = new Point(x, y);

            // P2 = 2P1 - P0
            x = 2 * firstControlPoints[0].x - knots[0].x;
            y = 2 * firstControlPoints[0].y - knots[0].y;
            secondControlPoints[0] = new Point(x, y);

            return new Point[][] { firstControlPoints, secondControlPoints };
        }

        // Calculate first bezier control points
        // Right hand side vector
        float[] rhs = new float[n];

        // Set right hand side X values
        for (int i = 1; i < n - 1; i++) {
            rhs[i] = 4 * knots[i].x + 2 * knots[i + 1].x;
        }
        rhs[0] = knots[0].x + 2 * knots[1].x;
        rhs[n - 1] = (8 * knots[n - 1].x + knots[n].x) / 2f;

        // Get first control points X-values
        float[] x = getFirstControlPoints(rhs);

        // Set right hand side Y values
        for (int i = 1; i < n - 1; i++) {
            rhs[i] = 4 * knots[i].y + 2 * knots[i + 1].y;
        }
        rhs[0] = knots[0].y + 2 * knots[1].y;
        rhs[n - 1] = (8 * knots[n - 1].y + knots[n].y) / 2f;

        // Get first control points Y-values
        float[] y = getFirstControlPoints(rhs);

        for (int i = 0; i < n; i++) {
            // First control point
            firstControlPoints[i] = new Point(x[i], y[i]);

            // Second control point
            if (i < n - 1) {
                float xx = 2 * knots[i + 1].x - x[i + 1];
                float yy = 2 * knots[i + 1].y - y[i + 1];
                secondControlPoints[i] = new Point(xx, yy);
            } else {
                float xx = (knots[n].x + x[n - 1]) / 2;
                float yy = (knots[n].y + y[n - 1]) / 2;
                secondControlPoints[i] = new Point(xx, yy);
            }
        }

        return new Point[][] { firstControlPoints, secondControlPoints };
    }

    /**
     * Solves a tridiagonal system for one of coordinates (x or y) of first
     * bezier control points.
     *
     * @param rhs right hand side vector.
     * @return Solution vector.
     */
    private static float[] getFirstControlPoints(float[] rhs) {
        int n = rhs.length;
        float[] x = new float[n]; // Solution vector
        float[] tmp = new float[n]; // Temp workspace

        float b = 2.0f;
        x[0] = rhs[0] / b;

        // Decomposition and forward substitution
        for (int i = 1; i < n; i++) {
            tmp[i] = 1 / b;
            b = (i < n - 1 ? 4.0f : 3.5f) - tmp[i];
            x[i] = (rhs[i] - x[i - 1]) / b;
        }

        // Backsubstitution
        for (int i = 1; i < n; i++) {
            x[n - i - 1] -= tmp[n - i] * x[n - i];
        }

        return x;
    }

}