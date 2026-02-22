package com.blueberryjoy.notes.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.NonNull;

public class Avatar {
    @NonNull
    public static Bitmap createLetterAvatar(@NonNull String name, int size) {

        String letter = name.substring(0, 1).toUpperCase();

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // 🎨 Pick color based on letter
        int color = pickColor(letter);
        paint.setColor(color);

        // Draw circle
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);

        // Draw text
        paint.setColor(Color.parseColor("#673AB7"));
        paint.setTextSize((float) size / 2);
        paint.setTextAlign(Paint.Align.CENTER);

        Rect bounds = new Rect();
        paint.getTextBounds(letter, 0, 1, bounds);

        canvas.drawText(
                letter,
                size / 2f,
                size / 2f + bounds.height() / 2f,
                paint
        );

        return bitmap;
    }

    private static int pickColor(@NonNull String letter) {

        int[] colors = {
                Color.parseColor("#C4FFDD"),
                Color.parseColor("#FFF9D1"),
                Color.parseColor("#FFE8D1"),
                Color.parseColor("#FFD7E1"),
                Color.parseColor("#F9E4FF"),
                Color.parseColor("#50A6C5"),
                Color.parseColor("#DFCCC4")
        };

        return colors[Math.abs(letter.hashCode()) % colors.length];
    }
}
