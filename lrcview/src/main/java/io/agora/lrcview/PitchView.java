package io.agora.lrcview;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Property;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.List;

import io.agora.lrcview.bean.LrcData;
import io.agora.lrcview.bean.LrcEntryData;

/**
 * 音调View
 *
 * @author chenhengfei(Aslanchen)
 * @date 2021/08/04
 */
public class PitchView extends View {

    private static final int START_PERCENT = 40;

    private static volatile LrcData lrcData;
    private Handler mHandler;

    private float widthPerSecond = 0.2F;//1ms对应像素px

    private int itemHeight = 4;//每一项高度px
    private int itemSpace = 4;//间距px

    private int pitchMax = 0;//最大值
    private int pitchMin = 100;//最小值
    private int indicatorRadius;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int mNormalTextColor;
    private int mDoneTextColor;

    private LinearGradient linearGradient;

    private float dotPointX = 0F;//亮点坐标

    public PitchView(Context context) {
        super(context);
        init(null);
    }

    public PitchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public PitchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PitchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        indicatorRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7, getResources().getDisplayMetrics());
        if (attrs == null) {
            return;
        }
        this.mHandler = new Handler(Looper.myLooper());
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.PitchView);
        mNormalTextColor = ta.getColor(R.styleable.PitchView_pitchNormalTextColor, getResources().getColor(R.color.lrc_normal_text_color));
        mDoneTextColor = ta.getColor(R.styleable.PitchView_pitchDoneTextColor, getResources().getColor(R.color.lrc_current_text_color));
        ta.recycle();

        int startColor = getResources().getColor(R.color.pitch_start);
        int endColor = getResources().getColor(R.color.pitch_end);
        linearGradient = new LinearGradient(dotPointX, 0, 0, 0, startColor, endColor, Shader.TileMode.CLAMP);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            int w = right - left;
            int h = bottom - top;
            dotPointX = w * START_PERCENT / 100F;

            int startColor = getResources().getColor(R.color.pitch_start);
            int endColor = getResources().getColor(R.color.pitch_end);
            linearGradient = new LinearGradient(dotPointX, 0, 0, 0, startColor, endColor, Shader.TileMode.CLAMP);

            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawStartLine(canvas);
        drawLocalPitch(canvas);
        drawItems(canvas);
    }

    private void drawLocalPitch(Canvas canvas) {
        mPaint.setShader(null);
        mPaint.setColor(mNormalTextColor);
        float value = getPitchHeight();
        if(value >= 0){
            canvas.drawCircle(dotPointX, value, indicatorRadius, mPaint);
        }
    }

    private float getPitchHeight() {
        float res = 0;
        if(mLocalPitch!=0 && pitchMax != 0 && pitchMin != 100){
            float realPitchMax = pitchMax + 5;
            float realPitchMin = pitchMin - 5;
            res = (float) (1 - ((mLocalPitch - pitchMin)  / (realPitchMax - realPitchMin)) ) * getHeight();
        }
        else if(mLocalPitch == 0){
            res = getHeight();
        }
        return res;
    }

    private void drawStartLine(Canvas canvas) {
        mPaint.setShader(linearGradient);
        canvas.drawRect(0, 0, dotPointX, getHeight(), mPaint);
    }

    private void drawItems(Canvas canvas) {
        mPaint.setShader(null);
        mPaint.setColor(mNormalTextColor);

        if (lrcData == null || lrcData.entrys == null || lrcData.entrys.isEmpty()) {
            return;
        }

        float realPitchMax = pitchMax + 5;
        float realPitchMin = pitchMin - 5;

        List<LrcEntryData> entrys = lrcData.entrys;
        float currentPX = this.mCurrentTime * widthPerSecond;
        float x = dotPointX * 1.3f - currentPX;
        float y = 0;
        float widthTone = 0;
        float mItemHeight = getHeight() / (float) (realPitchMax - realPitchMin);//高度
        long preEndTIme = 0;
        for (int i = 0; i < entrys.size(); i++) {
            LrcEntryData entry = lrcData.entrys.get(i);
            List<LrcEntryData.Tone> tones = entry.tones;
            if (tones == null || tones.isEmpty()) {
                return;
            }

            long startTime = entry.getStartTime();
            float emptyPX = widthPerSecond * (startTime - preEndTIme);
            x = x + emptyPX;

            if (x >= getWidth()) {
                break;
            }

            preEndTIme = tones.get(tones.size() - 1).end;
            for (LrcEntryData.Tone tone : tones) {
                widthTone = widthPerSecond * tone.getDuration();
                float endX = x + widthTone;
                if (endX <= 0) {
                    x = endX;
                    continue;
                }

                if (x >= getWidth()) {
                    x = endX;
                    break;
                }

                y = (realPitchMax - tone.pitch) * mItemHeight;
                RectF r = new RectF(x, y, endX, y + itemHeight);
                canvas.drawRect(r, mPaint);

                x = endX;
            }
        }
    }

    /**
     * 设置歌词信息
     *
     * @param data 歌词信息对象
     */
    public void setLrcData(LrcData data) {
        lrcData = data;

        if (lrcData != null && lrcData.entrys != null && !lrcData.entrys.isEmpty()) {
            for (LrcEntryData entry : lrcData.entrys) {
                for (LrcEntryData.Tone tone : entry.tones) {
                    pitchMin = Math.min(pitchMin, tone.pitch);
                    pitchMax = Math.max(pitchMax, tone.pitch);
                }
            }
        }

        invalidate();
    }

    private long mCurrentTime = 0;
    private float mLocalPitch = 0;

    private void setMLocalPitch(float mLocalPitch) {
        this.mLocalPitch = mLocalPitch;
    }
    /**
     * 更新音调
     *
     * @param pitch 单位hz
     */
    public void updateLocalPitch(double pitch) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mLocalPitch == pitch){
                    mLocalPitch = 0;
                }
            }
        }, 2000L);
        ObjectAnimator.ofFloat(this, "mLocalPitch", this.mLocalPitch, (float) pitch).setDuration(50).start();
        invalidate();
    }

    /**
     * 更新进度，单位毫秒
     *
     * @param time 当前播放时间，毫秒
     */
    public void updateTime(long time) {
        if (lrcData == null) {
            return;
        }

        this.mCurrentTime = time;

        invalidate();
    }

    /**
     * 重置内部状态，清空已经加载的歌词
     */
    public void reset() {
        lrcData = null;
        mCurrentTime = 0;
        pitchMax = 0;
        pitchMin = 100;

        invalidate();
    }
}
