package io.agora.lrcview;

import android.animation.ObjectAnimator;
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
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
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
    private int totalPitch = 0;

    // 完成 PitchView.OnActionListener#onOriginalPitch的需求
    // 当前 Pitch 所在的字的开始时间
    private long currentPitchStartTime = 0;
    // 当前 Pitch 所在的字的结束时间
    private long currentPitchEndTime = 0;
    // 当前 Pitch 所在的句的结束时间
    private long currentEntryEndTime = 0;
    // 当前在打分的所在句的结束时间
    private long currentScoreEntryEndTime = 0;
    // 当前在打分的所在句的结束时间
    private long lrcEndTime = 0;

    // 音调指示器的半径
    private int indicatorRadius;
    // 每句最高分
    private int scorePerSentence = 100;
    // 初始分数
    private float mInitialScore;
    // 每句歌词分数
    public List<Double> sentenceScoreList = new ArrayList<>();
    // 累计分数
    public float cumulatedScore;
    // 歌曲总分数
    public float totalScore;
    // 分数阈值 大于此值计分 小于不计分
    public final float scoreCountLine = 0.4f;

    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int mNormalTextColor;
    private int mDoneTextColor;

    private LinearGradient linearGradient;

    private float dotPointX = 0F;//亮点坐标

    // 音调及分数回调
    public OnActionListener onActionListener;

    //<editor-fold desc="Init Related">
    public PitchView(Context context) {
        this(context, null);
    }

    public PitchView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
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
        mInitialScore = ta.getFloat(R.styleable.PitchView_pitchInitialScore, 50f);
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
    //</editor-fold>

    private void drawLocalPitch(Canvas canvas) {
        mPaint.setShader(null);
        mPaint.setColor(mNormalTextColor);
        float value = getPitchHeight();
        if (value >= 0) {
            canvas.drawCircle(dotPointX, value, indicatorRadius, mPaint);
        }
    }

    private float getPitchHeight() {
        float res = 0;
        if (mLocalPitch != 0 && pitchMax != 0 && pitchMin != 100) {
            float realPitchMax = pitchMax + 5;
            float realPitchMin = pitchMin - 5;
            res = (1 - ((mLocalPitch - pitchMin) / (realPitchMax - realPitchMin))) * getHeight();
        } else if (mLocalPitch == 0) {
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
        float mItemHeight = getHeight() / (realPitchMax - realPitchMin);//高度
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
    public void setLrcData(@Nullable LrcData data) {
        lrcData = data;
        totalPitch = 0;

        mCurrentTime = 0;
        pitchMax = 0;
        pitchMin = 100;

        currentPitchStartTime = 0;
        currentPitchEndTime = 0;
        currentEntryEndTime = 0;
        currentScoreEntryEndTime = 0;
        sentenceScoreList.clear();

        if (lrcData != null && lrcData.entrys != null && !lrcData.entrys.isEmpty()) {

            lrcEndTime = lrcData.entrys.get(lrcData.entrys.size() - 1).getEndTime();
            totalScore = scorePerSentence * lrcData.entrys.size() + mInitialScore;

            for (LrcEntryData entry : lrcData.entrys) {
                for (LrcEntryData.Tone tone : entry.tones) {
                    pitchMin = Math.min(pitchMin, tone.pitch);
                    pitchMax = Math.max(pitchMax, tone.pitch);
                    totalPitch++;
                }
            }
        }

        invalidate();
    }

    private long mCurrentTime = 0;
    private float mLocalPitch = 0;

    private void setMLocalPitch(float mLocalPitch) {
        this.mLocalPitch = mLocalPitch;
        invalidate();
    }

    /**
     * 根据当前播放时间获取 Pitch
     *
     * @return 当前时间歌词的 Pitch
     */
    private float findPitchByTime() {
        if (lrcData == null) return 0;

        float resPitch = 0;
        int entryCount = lrcData.entrys.size();
        for (int i = 0; i < entryCount; i++) {
            LrcEntryData tempEntry = lrcData.entrys.get(i);
            if (mCurrentTime >= tempEntry.getStartTime()) { // 索引
                int toneCount = tempEntry.tones.size();
                for (int j = 0; j < toneCount; j++) {
                    LrcEntryData.Tone tempTone = tempEntry.tones.get(j);
                    if (mCurrentTime <= tempTone.end) {
                        resPitch = tempTone.pitch;
                        currentPitchStartTime = tempTone.begin;
                        currentPitchEndTime = tempTone.end;

                        currentEntryEndTime = tempEntry.getEndTime();
                        break;
                    }
                }
                break;
            }
        }
        if (resPitch == 0) {
            currentPitchStartTime = 0;
            currentPitchEndTime = 0;
            currentEntryEndTime = 0;
        }
        return resPitch;
    }

    /**
     * 更新音调，更新分数，执行圆点动画
     *
     * @param pitch 单位hz
     */
    public void updateLocalPitch(float pitch) {
        if (lrcData == null) return;
        float desiredPitch = findPitchByTime();
        if (desiredPitch != 0)
            updateScore(pitchToTone(pitch), pitchToTone(desiredPitch));

        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(() -> {
            if (mLocalPitch == pitch) {
                mLocalPitch = 0;
            }
        }, 2000L);
        ObjectAnimator.ofFloat(this, "mLocalPitch", this.mLocalPitch, pitch).setDuration(50).start();
    }

    /**
     * 更新当前分数
     *
     * @param currentTone 演唱值
     * @param desiredTone 理想值
     */
    private void updateScore(double currentTone, double desiredTone) {
        double score = 1 - Math.abs(desiredTone - currentTone) / desiredTone;
        score = score >= scoreCountLine ? score : 0f;
        score *= scorePerSentence;

        // 当前未在打分 <==> 定位打分句结束时间到当前句
        if (sentenceScoreList.isEmpty()) currentScoreEntryEndTime = currentEntryEndTime;

        // 打分句结束时间已过 或者 最后一句已经结束
        if (mCurrentTime > currentScoreEntryEndTime || mCurrentTime > lrcEndTime) { // 已经到下一句了
            // 分数列表不为空
            if (!sentenceScoreList.isEmpty()) {

                // 计算歌词当前句的分数 = 所有打分/分数个数
                double tempScore = 0;
                for (Double toneScore : sentenceScoreList)
                    tempScore += toneScore;

                // 统计到累计分数
                cumulatedScore += tempScore / sentenceScoreList.size();
                // 回调到上层
                dispatchScore(score);
                // 清除打分
                sentenceScoreList.clear();
            }
        }

        sentenceScoreList.add(score);
    }

    /**
     * 根据当前歌曲时间决定是否回调{@link OnActionListener#onScore(double, double, double)}
     *
     * @param score 本次算法返回的分数
     */
    private void dispatchScore(double score) {
        if (onActionListener != null) onActionListener.onScore(score, cumulatedScore, totalScore);
    }

    /**
     * 更新进度，单位毫秒
     * 根据当前时间，决定是否回调{@link OnActionListener#onOriginalPitch(float, int)}
     * 与打分逻辑无关
     *
     * @param time 当前播放时间，毫秒
     */
    public void updateTime(long time) {
        if (lrcData == null) {
            return;
        } else if (time < currentPitchStartTime || time > currentPitchEndTime) {
            onActionListener.onOriginalPitch(findPitchByTime(), totalPitch);
        }

        this.mCurrentTime = time;

        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (onActionListener != null) onActionListener = null;
    }

    public static double pitchToTone(double pitch) {
        double eps = 1e-6;
        return (Math.max(0, Math.log(pitch / 55 + eps) / Math.log(2))) * 12;
    }

    public static interface OnActionListener {

        /**
         * 咪咕歌词原始参考pitch值回调, 用于开发者自行实现打分逻辑. 歌词每个tone回调一次
         * pitch: 当前tone的pitch值
         * totalCount: 整个xml的tone个数, 用于开发者方便自己在app层计算平均分.
         */
        void onOriginalPitch(float pitch, int totalCount);

        /**
         * paas组件内置的打分回调, 每句歌词结束的时候提供回调(句指xml中的sentence节点),
         * 并提供totalScore参考值用于按照百分比方式显示分数
         *
         * @param score           这次回调的分数 0-10之间
         * @param cumulativeScore 累计的分数 初始分累计到当前的分数
         * @param totalScore      总分 = 初始分(默认值0分) + xml中sentence的个数 * 10
         */
        void onScore(double score, double cumulativeScore, double totalScore);
    }
}
