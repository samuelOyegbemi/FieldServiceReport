package com.floromsolutions.fieldservicereport;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CircularTextView extends android.support.v7.widget.AppCompatTextView {
    int solidColor = getRandomColor();
    public CircularTextView(Context context) {
        super(context);
    }

    public CircularTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CircularTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT)
        {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int height = this.getMeasuredHeight();
            int width = this.getMeasuredWidth();

            int diameter = Math.max(height, width);
            setMeasuredDimension(diameter, diameter);
        }
        else {
            try {
                int height = MeasureSpec.getSize(heightMeasureSpec);
                int width = MeasureSpec.getSize(widthMeasureSpec);

                int dimension = Math.max(height, width);
                setMeasuredDimension(dimension, dimension);
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(dimension, MeasureSpec.getMode(widthMeasureSpec));
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(dimension, MeasureSpec.getMode(heightMeasureSpec));
            } finally {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Paint circlePaint = new Paint();
        circlePaint.setColor(solidColor);
        circlePaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        int h = this.getHeight();
        int w = this.getWidth();
        int diameter = ((h > w) ? h : w);
        int radius = diameter / 2;

        this.setHeight(diameter);
        this.setWidth(diameter);

        canvas.drawCircle(diameter / 2, diameter / 2, radius, circlePaint);
        super.draw(canvas);
    }

    private int getRandomColor() {
        List<Integer> permittedColors = new ArrayList<>();

        permittedColors.add(ContextCompat.getColor(getContext(), R.color.pink));
        permittedColors.add(ContextCompat.getColor(getContext(), R.color.florom_color));
        permittedColors.add(ContextCompat.getColor(getContext(), R.color.colorAccent));
        permittedColors.add(ContextCompat.getColor(getContext(), R.color.colorPrimary));
        permittedColors.add(ContextCompat.getColor(getContext(), R.color.dark_slate_gray));
        permittedColors.add(ContextCompat.getColor(getContext(), R.color.light_salmon));
        permittedColors.add(ContextCompat.getColor(getContext(), R.color.sea_green));
        permittedColors.add(ContextCompat.getColor(getContext(), R.color.spring_gren));
        permittedColors.add(ContextCompat.getColor(getContext(), R.color.sky_blue));
        permittedColors.add(ContextCompat.getColor(getContext(), R.color.teal));
        permittedColors.add(ContextCompat.getColor(getContext(), R.color.slate_gray));
        permittedColors.add(ContextCompat.getColor(getContext(), R.color.dark_gray));

        Random r = new Random();
        int cPos = r.nextInt(11 );
        return permittedColors.get(cPos);
    }
}
