package com.example.rawconverter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.example.rawconverter.utils.Point;
import com.example.rawconverter.BezierSplineUtil;

import java.lang.Math;

import java.util.ArrayList;
import java.util.List;

public class ToneCurveView extends View {

    Paint paint_grid = new Paint();
    Paint paint_circle = new Paint();
    Paint paint_curve = new Paint();

    List<Point> knots = new ArrayList<Point>();

    int obj_radius = 30;

    int current_circle = -1;
    float current_distance = 99;

    Path path = new Path();
    Point[] firstCP;
    Point[] secondCP;


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
    }

    private void drawGrid(Canvas canvas) {
        canvas.drawLine(0, 0, this.getMeasuredWidth(), 0, paint_grid);  // Upper line
        canvas.drawLine(0, 0, 0, this.getMeasuredHeight(), paint_grid);  // Left line
        canvas.drawLine(this.getMeasuredWidth(), 0, this.getMeasuredWidth(), this.getMeasuredHeight(), paint_grid);  // Right Line
        canvas.drawLine(0, this.getMeasuredHeight(), this.getMeasuredWidth(), this.getMeasuredHeight(), paint_grid);  // Bottom Line
        canvas.drawLine(0, 0, this.getMeasuredWidth(), this.getMeasuredHeight(), paint_grid);  // Diagonal Line
    }

    private void drawPoints(Canvas canvas) {
        for (int i = 0; i < knots.size(); i++) {
            canvas.drawCircle(knots.get(i).x, knots.get(i).y, obj_radius, paint_circle);
        }
    }

    private void drawCurve(Canvas canvas) {
        path.reset();
        if (knots.size() > 2) {
            Point[] knotsArr = new Point[knots.size()];
            Point[][] controlPoints = BezierSplineUtil.getCurveControlPoints(knots.toArray(knotsArr));
            firstCP = controlPoints[0];
            secondCP = controlPoints[1];

            path.moveTo(knots.get(0).x, knots.get(0).y);

            for (int i = 0; i < firstCP.length; i++) {
                path.cubicTo(Math.max(Math.min(firstCP[i].x, maxBoundary_x), 0),
                        Math.max(Math.min(firstCP[i].y, maxBoundary_y), 0),
                        Math.max(Math.min(secondCP[i].x, maxBoundary_x), 0),
                        Math.max(Math.min(secondCP[i].y, maxBoundary_y), 0),
                        knots.get(i + 1).x, knots.get(i + 1).y);
            }
        } else {
            path.moveTo(knots.get(0).x, knots.get(0).y);
            path.lineTo(knots.get(1).x, knots.get(1).y);
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
                for (int i = 0; i < knots.size(); i++) {
                    float distance = knots.get(i).distanceToPoint(x, y);
                    if (distance < (float) (obj_radius * 3)) {
                        if (distance < current_distance) {
                            current_distance = distance;
                            current_circle = i;
                        }
                    }
                }
                if (current_circle < 0) {
                    int index = 1;
                    if (knots.size() > 2) {
                        for (int i = 1; i < knots.size(); i++) {
                            if (x < knots.get(i).x) {
                                index = i;
                                break;
                            }
                        }
                    }
                    knots.add(index, new Point(x, y));
                    current_circle = index;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (current_circle > -1) {
                    if (current_circle == 0 || current_circle == knots.size() - 1) {
                        // if first or last circle, only move in y direction
                        knots.get(current_circle).y = y;
                    } else {
                        knots.get(current_circle).y = y;

                        if (x <= knots.get(current_circle + 1).x && x >= knots.get(current_circle - 1).x)
                            knots.get(current_circle).x = x;
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
        knots.add(new Point(0.f, 0.f));
        knots.add(new Point((float) this.getMeasuredWidth(), (float) this.getMeasuredHeight()));

    }

}
