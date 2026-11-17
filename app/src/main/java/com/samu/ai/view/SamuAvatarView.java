package com.samu.ai.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Samu - the robot girl avatar. Drawn live on a Canvas so she can blink,
 * glow while listening and move her mouth while speaking, with a soft 3D
 * look (gradients + shadows). No external asset needed.
 */
public class SamuAvatarView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private boolean speaking = false;
    private boolean listening = false;
    private float time = 0;
    private float blink = 1f;
    private float mouth = 0f;

    private final ValueAnimator ticker = ValueAnimator.ofFloat(0f, 1f);

    public SamuAvatarView(Context context) {
        super(context);
        init();
    }

    public SamuAvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SamuAvatarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        ticker.setDuration(50);
        ticker.setRepeatCount(ValueAnimator.INFINITE);
        ticker.setInterpolator(new LinearInterpolator());
        ticker.addUpdateListener(a -> {
            time += 0.05f;
            if (speaking) {
                mouth = Math.abs((float) Math.sin(time * 9));
            } else {
                mouth = Math.max(0f, mouth - 0.08f);
            }
            long ms = System.currentTimeMillis() % 4000;
            blink = ms < 140 ? Math.abs(1f - (ms / 70f)) : 1f;
            invalidate();
        });
        ticker.start();
    }

    public void setSpeaking(boolean speaking) {
        this.speaking = speaking;
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) * 0.34f;

        // Ambient glow ring
        float glowR = r * (listening ? 1.45f + 0.06f * (float) Math.sin(time * 6)
                : 1.3f);
        glow.setShader(new RadialGradient(cx, cy, glowR,
                listening ? Color.argb(120, 255, 80, 160) : Color.argb(60, 120, 160, 255),
                Color.TRANSPARENT, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, glowR, glow);

        // Hair (back layer)
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#3B2A55"));
        rect.set(cx - r * 1.18f, cy - r * 1.28f, cx + r * 1.18f, cy + r * 1.35f);
        canvas.drawRoundRect(rect, r, r, paint);

        // Head with 3D gradient
        paint.setShader(new RadialGradient(cx - r * 0.3f, cy - r * 0.4f, r * 1.9f,
                Color.parseColor("#FDEFF5"), Color.parseColor("#D9B8E8"),
                Shader.TileMode.CLAMP));
        rect.set(cx - r, cy - r, cx + r, cy + r * 1.05f);
        canvas.drawRoundRect(rect, r * 0.85f, r * 0.85f, paint);
        paint.setShader(null);

        // Fringe
        paint.setColor(Color.parseColor("#4A3268"));
        rect.set(cx - r, cy - r, cx + r, cy - r * 0.35f);
        canvas.drawRoundRect(rect, r * 0.85f, r * 0.4f, paint);

        // Antenna + glowing tip
        paint.setColor(Color.parseColor("#8E7BB5"));
        paint.setStrokeWidth(r * 0.09f);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(cx, cy - r, cx, cy - r * 1.35f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#FF5FA2"));
        canvas.drawCircle(cx, cy - r * 1.42f, r * 0.12f * (1f + 0.2f * (float) Math.sin(time * 5)), paint);

        // Eyes (blink squishes height)
        float eyeY = cy + r * 0.05f;
        float eyeDx = r * 0.42f;
        float eyeR = r * 0.16f * blink;
        paint.setColor(Color.parseColor("#2A1E3F"));
        canvas.drawCircle(cx - eyeDx, eyeY, Math.max(eyeR, r * 0.015f), paint);
        canvas.drawCircle(cx + eyeDx, eyeY, Math.max(eyeR, r * 0.015f), paint);

        // Eye shine
        paint.setColor(Color.WHITE);
        canvas.drawCircle(cx - eyeDx + eyeR * 0.3f, eyeY - eyeR * 0.3f, eyeR * 0.28f, paint);
        canvas.drawCircle(cx + eyeDx + eyeR * 0.3f, eyeY - eyeR * 0.3f, eyeR * 0.28f, paint);

        // Blush
        paint.setColor(Color.argb(90, 255, 120, 170));
        canvas.drawCircle(cx - eyeDx, cy + r * 0.42f, r * 0.14f, paint);
        canvas.drawCircle(cx + eyeDx, cy + r * 0.42f, r * 0.14f, paint);

        // Mouth (animates while speaking)
        paint.setColor(Color.parseColor("#B3447A"));
        float mw = r * 0.34f;
        float mh = Math.max(r * 0.02f, r * 0.22f * mouth);
        rect.set(cx - mw, eyeY + r * 0.28f - mh, cx + mw, eyeY + r * 0.28f + mh);
        canvas.drawRoundRect(rect, mw, mh, paint);
    }

    @Override
    protected void onDetachedFromWindow() {
        ticker.cancel();
        super.onDetachedFromWindow();
    }
}
