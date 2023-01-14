package com.example.rawconverter;
import java.lang.Math;

public class utils {

    public static class Point {
        public float x;
        public float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float distanceToPoint(float x, float y) {
            return (float) Math.sqrt((y - this.y) * (y - this.y) + (x - this.x) * (x - this.x));
        }
    }

}
