package com.growbig.browser.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.growbig.browser.R;

/**
 * A small view that draws toolbar icons with Canvas.
 *
 * Why: vector drawables need AndroidX to work on Android 4.4, and Phase 1 uses no AndroidX.
 * Drawing in code keeps the app dependency-free and lets us recolour icons at runtime.
 * Icons are drawn on a 24 x 24 grid and scaled to the view.
 */
public class ToolbarIcon extends View {

    // Must match the enum values in res/values/attrs.xml.
    public static final int BACK = 0;
    public static final int FORWARD = 1;
    public static final int REFRESH = 2;
    public static final int STOP = 3;
    public static final int HOME = 4;
    public static final int MENU = 5;
    public static final int PLUS = 6;
    public static final int CLOSE = 7;
    public static final int LOCK = 8;
    public static final int LOCK_OPEN = 9;
    public static final int SEARCH = 10;
    public static final int STAR = 11;
    public static final int STAR_FILLED = 12;

    private static final int DEFAULT_COLOR = 0xFFE8EAED;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    private int iconType = BACK;
    private int iconColor = DEFAULT_COLOR;

    public ToolbarIcon(Context context) {
        this(context, null);
    }

    public ToolbarIcon(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ToolbarIcon(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ToolbarIcon,
                    defStyleAttr, 0);
            try {
                iconType = a.getInt(R.styleable.ToolbarIcon_iconType, BACK);
                iconColor = a.getColor(R.styleable.ToolbarIcon_iconColor, DEFAULT_COLOR);
            } finally {
                a.recycle();
            }
        }
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setIconType(int type) {
        if (type != iconType) {
            iconType = type;
            invalidate();
        }
    }

    public void setIconColor(int color) {
        if (color != iconColor) {
            iconColor = color;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth() - getPaddingLeft() - getPaddingRight();
        int h = getHeight() - getPaddingTop() - getPaddingBottom();
        float size = Math.min(w, h);
        if (size <= 0) {
            return;
        }

        canvas.save();
        canvas.translate(getPaddingLeft() + (w - size) / 2f, getPaddingTop() + (h - size) / 2f);
        float scale = size / 24f;
        canvas.scale(scale, scale);

        paint.setColor(iconColor);
        if (!isEnabled()) {
            paint.setAlpha(90);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);

        switch (iconType) {
            case BACK:
                canvas.drawLine(19f, 12f, 6f, 12f, paint);
                path.reset();
                path.moveTo(11.5f, 6.5f);
                path.lineTo(6f, 12f);
                path.lineTo(11.5f, 17.5f);
                canvas.drawPath(path, paint);
                break;

            case FORWARD:
                canvas.drawLine(5f, 12f, 18f, 12f, paint);
                path.reset();
                path.moveTo(12.5f, 6.5f);
                path.lineTo(18f, 12f);
                path.lineTo(12.5f, 17.5f);
                canvas.drawPath(path, paint);
                break;

            case REFRESH:
                rect.set(5f, 5f, 19f, 19f);
                canvas.drawArc(rect, 0f, 270f, false, paint);
                paint.setStyle(Paint.Style.FILL);
                path.reset();
                path.moveTo(11f, 1.5f);
                path.lineTo(17f, 5f);
                path.lineTo(11f, 8.5f);
                path.close();
                canvas.drawPath(path, paint);
                break;

            case STOP:
                canvas.drawLine(6f, 6f, 18f, 18f, paint);
                canvas.drawLine(18f, 6f, 6f, 18f, paint);
                break;

            case CLOSE:
                canvas.drawLine(7f, 7f, 17f, 17f, paint);
                canvas.drawLine(17f, 7f, 7f, 17f, paint);
                break;

            case HOME:
                path.reset();
                path.moveTo(3.5f, 12f);
                path.lineTo(12f, 4.5f);
                path.lineTo(20.5f, 12f);
                canvas.drawPath(path, paint);
                path.reset();
                path.moveTo(6f, 10.5f);
                path.lineTo(6f, 19.5f);
                path.lineTo(18f, 19.5f);
                path.lineTo(18f, 10.5f);
                canvas.drawPath(path, paint);
                break;

            case MENU:
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(12f, 5f, 1.9f, paint);
                canvas.drawCircle(12f, 12f, 1.9f, paint);
                canvas.drawCircle(12f, 19f, 1.9f, paint);
                break;

            case PLUS:
                canvas.drawLine(12f, 5f, 12f, 19f, paint);
                canvas.drawLine(5f, 12f, 19f, 12f, paint);
                break;

            case LOCK:
                canvas.drawRect(6f, 11f, 18f, 20f, paint);
                rect.set(8f, 4f, 16f, 14f);
                canvas.drawArc(rect, 180f, 180f, false, paint);
                canvas.drawLine(8f, 9f, 8f, 11f, paint);
                canvas.drawLine(16f, 9f, 16f, 11f, paint);
                break;

            case LOCK_OPEN:
                canvas.drawRect(6f, 11f, 18f, 20f, paint);
                rect.set(8f, 2f, 16f, 12f);
                canvas.drawArc(rect, 180f, 180f, false, paint);
                canvas.drawLine(16f, 7f, 16f, 11f, paint);
                break;

            case SEARCH:
                canvas.drawCircle(10f, 10f, 6f, paint);
                canvas.drawLine(14.5f, 14.5f, 20f, 20f, paint);
                break;

            case STAR:
            case STAR_FILLED:
                buildStar();
                if (iconType == STAR_FILLED) {
                    paint.setStyle(Paint.Style.FILL);
                }
                canvas.drawPath(path, paint);
                break;

            default:
                break;
        }

        canvas.restore();
    }

    private void buildStar() {
        path.reset();
        float cx = 12f;
        float cy = 12.5f;
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            float radius = (i % 2 == 0) ? 9f : 4.2f;
            float x = (float) (cx + radius * Math.cos(angle));
            float y = (float) (cy + radius * Math.sin(angle));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }
}
