package com.example.rawconverter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.rawconverter.utils.Point;

import java.util.ArrayList;
import java.util.List;

public class ToneCurveView extends View {

    Paint paint_grid = new Paint();
    Paint paint_circle = new Paint();
    Paint paint_curve = new Paint();

    List<Point> knots_r = new ArrayList<>();
    List<Point> knots_g = new ArrayList<>();
    List<Point> knots_b = new ArrayList<>();
    List<List<Point>> knotsList = new ArrayList<>();

    int obj_radius = 30;

    int current_circle = -1;
    float current_distance = 99;

    Path path = new Path();

    Point[] firstCP;
    Point[] secondCP;

    Point[] firstCP_g;
    Point[] secondCP_g;

    Point[] firstCP_b;
    Point[] secondCP_b;

    Point[][] firstCPArr = {firstCP, firstCP_g, firstCP_b};
    Point[][] secondCPArr = {secondCP, secondCP_g, secondCP_b};

    int currentRGB = 2;


    float maxBoundary_x;
    float maxBoundary_y;

    public ToneCurveView(Context context) {
        super(context);
        init();
    }

    public ToneCurveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ToneCurveView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }


    private void init() {
        paint_grid.setColor(Color.parseColor("#FFFFFF"));
        paint_grid.setStrokeWidth(5);

        paint_circle.setColor(Color.parseColor("#CD5C5C"));

        paint_curve.setColor(Color.parseColor("#6811f5"));
        paint_curve.setStrokeWidth(20);
        paint_curve.setStyle(Paint.Style.FILL);
        paint_curve.setPathEffect(null);
        paint_curve.setStyle(Paint.Style.STROKE);
        knotsList.add(knots_r);
        knotsList.add(knots_g);
        knotsList.add(knots_b);
    }

    private void drawGrid(Canvas canvas) {
        canvas.drawLine(0, 0, this.getMeasuredWidth(), 0, paint_grid);  // Upper line
        canvas.drawLine(0, 0, 0, this.getMeasuredHeight(), paint_grid);  // Left line
        canvas.drawLine(this.getMeasuredWidth(), 0, this.getMeasuredWidth(), this.getMeasuredHeight(), paint_grid);  // Right Line
        canvas.drawLine(0, this.getMeasuredHeight(), this.getMeasuredWidth(), this.getMeasuredHeight(), paint_grid);  // Bottom Line
        canvas.drawLine(0, 0, this.getMeasuredWidth(), this.getMeasuredHeight(), paint_grid);  // Diagonal Line
    }

    private void drawPoints(Canvas canvas) {
        for (int i = 0; i < knotsList.get(currentRGB).size(); i++) {
            canvas.drawCircle(knotsList.get(currentRGB).get(i).x, knotsList.get(currentRGB).get(i).y, obj_radius, paint_circle);
        }
    }

    private void drawCurve(Canvas canvas) {
        path.reset();
        if (knotsList.get(currentRGB).size() > 2) {
            Point[] knotsArr = new Point[this.knotsList.get(currentRGB).size()];
            Point[][] controlPoints = BezierSplineUtil.getCurveControlPoints(this.knotsList.get(currentRGB).toArray(knotsArr));
            firstCPArr[currentRGB] = controlPoints[0];
            secondCPArr[currentRGB] = controlPoints[1];

            path.moveTo(this.knotsList.get(currentRGB).get(0).x, this.knotsList.get(currentRGB).get(0).y);

            for (int i = 0; i < firstCPArr[currentRGB].length; i++) {
                path.cubicTo(Math.max(Math.min(firstCPArr[currentRGB][i].x, maxBoundary_x), 0),
                        Math.max(Math.min(firstCPArr[currentRGB][i].y, maxBoundary_y), 0),
                        Math.max(Math.min(secondCPArr[currentRGB][i].x, maxBoundary_x), 0),
                        Math.max(Math.min(secondCPArr[currentRGB][i].y, maxBoundary_y), 0),
                        this.knotsList.get(currentRGB).get(i + 1).x, this.knotsList.get(currentRGB).get(i + 1).y);
            }
        } else {
            path.moveTo(knotsList.get(currentRGB).get(0).x, knotsList.get(currentRGB).get(0).y);
            path.lineTo(knotsList.get(currentRGB).get(1).x, knotsList.get(currentRGB).get(1).y);
        }
        canvas.drawPath(path, paint_curve);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.scale(1f, -1f, maxBoundary_x / 2f, maxBoundary_y / 2f);
        drawGrid(canvas);
        drawCurve(canvas);
        drawPoints(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = Math.max(Math.min(event.getX(), maxBoundary_x), 0);
        float y = Math.max(Math.min(maxBoundary_y - event.getY(), maxBoundary_y), 0);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                for (int i = 0; i < knotsList.get(currentRGB).size(); i++) {
                    float distance = knotsList.get(currentRGB).get(i).distanceToPoint(x, y);
                    if (distance < (float) (obj_radius * 3)) {
                        if (distance < current_distance) {
                            current_distance = distance;
                            current_circle = i;
                        }
                    }
                }
                if (current_circle < 0) {
                    int index = 1;
                    if (knotsList.get(currentRGB).size() > 2) {
                        for (int i = 1; i < knotsList.get(currentRGB).size(); i++) {
                            if (x < knotsList.get(currentRGB).get(i).x) {
                                index = i;
                                break;
                            }
                        }
                    }
                    knotsList.get(currentRGB).add(index, new Point(x, y));
                    current_circle = index;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (current_circle > -1) {
                    if (current_circle == 0 || current_circle == knotsList.get(currentRGB).size() - 1) {
                        // if first or last circle, only move in y direction
                        knotsList.get(currentRGB).get(current_circle).y = y;
                    } else {
                        knotsList.get(currentRGB).get(current_circle).y = y;

                        if (x <= knotsList.get(currentRGB).get(current_circle + 1).x && x >= knotsList.get(currentRGB).get(current_circle - 1).x)
                            knotsList.get(currentRGB).get(current_circle).x = x;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                current_circle = -1;
                current_distance = 99;

                break;
            default:
                return false;
        }

        invalidate();

        return true;
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        maxBoundary_x = this.getMeasuredWidth();
        maxBoundary_y = this.getMeasuredHeight();

        // Add first point and second point
        knotsList.get(currentRGB).add(new Point(0.f, 0.f));
        knotsList.get(currentRGB).add(new Point((float) this.getMeasuredWidth(), (float) this.getMeasuredHeight()));

    }

}
