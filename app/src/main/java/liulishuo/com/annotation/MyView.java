package liulishuo.com.annotation;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * 自定义的View，带显示标注内容
 * 1.支持自定义文本颜色和标注文本颜色
 * 2.支持自定义文本颜色和标注文本大小，但是为了保持标注的对应，**目前假设一个字最多就是两个标注字**
 * 3.支持在xml中设置文本属性
 */
public class MyView extends View {

    private static final String TAG = MyView.class.getSimpleName();

    private String mSourceText;
    private int mSourceColor = Color.BLACK;
    private float mSourceDimension = 0;
    private int mAnnotationColor = Color.GRAY;
    private float mAnnotationDimension = 0;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;
    private TextPaint mSmallTextPaint;
    private float mSmallTextHeight;
    private Paint.FontMetricsInt mFontMetrics;
    private Paint.FontMetricsInt mSmallFontMetrics;

    //private AnnotationHelper mAnnotationHelper;

    public MyView(Context context) {
        super(context);
        init(null, 0);
    }

    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public MyView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        //mAnnotationHelper = AnnotationHelper.getInstance();

        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MyView, defStyle, 0);
        mSourceText = a.getString(R.styleable.MyView_sourceText);
        mSourceColor = a.getColor(R.styleable.MyView_sourceColor, mSourceColor);
        mSourceDimension = a.getDimension(R.styleable.MyView_sourceDimension, (int) mSourceDimension);//todo getDimensionPixelSize
        mAnnotationColor = a.getColor(R.styleable.MyView_annotationColor, mAnnotationColor);
        mAnnotationDimension = a.getDimension(R.styleable.MyView_annotationDimension, (int) mAnnotationDimension);
        a.recycle();

        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTypeface(Typeface.MONOSPACE);

        mSmallTextPaint = new TextPaint();
        mSmallTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mSmallTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTypeface(Typeface.MONOSPACE);

        invalidateTextPaintAndMeasurements();
    }

    private void invalidateTextPaintAndMeasurements() {
        mTextPaint.setTextSize(mSourceDimension);
        mTextPaint.setColor(mSourceColor);
        mTextWidth = mTextPaint.measureText(mSourceText);//
        mFontMetrics = mTextPaint.getFontMetricsInt();
        mTextHeight = mFontMetrics.bottom - mFontMetrics.top;//文本边界

        mSmallTextPaint.setTextSize(mAnnotationDimension);
        mSmallTextPaint.setColor(mAnnotationColor);
        mSmallFontMetrics = mSmallTextPaint.getFontMetricsInt();
        mSmallTextHeight = mSmallFontMetrics.bottom - mSmallFontMetrics.top;//标注内容边界
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int width;
        int height;
        if (widthMode == MeasureSpec.EXACTLY) {//最好不是强制设置宽和高
            width = widthSize;
        } else {
            width = (int) (getPaddingLeft() + mTextWidth + getPaddingRight());
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            height = (int) (getPaddingTop() + mTextHeight + mSmallTextHeight + getPaddingBottom());
        }

        setMeasuredDimension(width, height);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        int contentWidth = getWidth() - paddingLeft - paddingRight;
        int contentHeight = getHeight() - paddingTop - paddingBottom;

        //计算下面的文本的显示区域
        Rect downRect = new Rect(0, (int) mSmallTextHeight, contentWidth, contentHeight);//left, top, right, bottom
        //计算baseline，使得文字垂直居中
        int baseline = downRect.top + (downRect.bottom - downRect.top - mFontMetrics.bottom + mFontMetrics.top) / 2 - mFontMetrics.top;
        //写下面的文本
        canvas.drawText(mSourceText, downRect.centerX(), baseline, mTextPaint);

        //处理上面的标注
        Rect upRect = new Rect(0, 0, contentWidth, (int) mSmallTextHeight);
        baseline = upRect.top + (upRect.bottom - upRect.top - mSmallFontMetrics.bottom + mSmallFontMetrics.top) / 2 - mSmallFontMetrics.top;

        Paint boundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boundPaint.setColor(Color.RED);
        boundPaint.setStrokeWidth(1);
        boundPaint.setStyle(Paint.Style.STROKE);

        Paint sourceBoundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sourceBoundPaint.setColor(Color.GREEN);
        sourceBoundPaint.setStrokeWidth(1);
        sourceBoundPaint.setStyle(Paint.Style.STROKE);

        String current, start, annotation = "";
        AnnotationHelper.AnnotationItem annotationItem;
        int offset = 0;
        Rect annotationBound;
        Rect sourceBound = new Rect(), offsetBound = new Rect();
        for (int i = 0; i < mSourceText.length(); i++) {//start from 0
            offset = (int) (mTextPaint.measureText(mSourceText, 0, i));//+ mTextPaint.getLetterSpacing()*i
            /*mTextPaint.getTextBounds(mSourceText, 0, i, offsetBound);
            Log.e(TAG, "offset " + i + " :" + sourceBound.toString());
            offset = offsetBound.width();*/

            mTextPaint.getTextBounds(mSourceText, i, i + 1, sourceBound);//先获取下面的文本的原文字的宽度
            Log.e(TAG, "source " + i + " :" + sourceBound.toString());
            Rect sourceRect = new Rect(offset, (int) mSmallTextHeight, offset + sourceBound.width(), (int) (mTextHeight + mSmallTextHeight));
            canvas.drawRect(sourceRect, sourceBoundPaint);

            //offset = (int)((i - 1) * mAnnotationDimension);
            annotationBound = new Rect(offset, 0, offset + sourceBound.width(), (int) mSmallTextHeight);//取得上面的标注文本的bound
            Log.e(TAG, "annotation " + i + " :" + annotationBound.toString());
            canvas.drawRect(annotationBound, boundPaint);

            current = mSourceText.substring(i, i + 1);
            if (current.equalsIgnoreCase("，") || current.equalsIgnoreCase("。") || current.equalsIgnoreCase(" ")) {//todo 不确定这里还有哪些可能的标点符号，暂时只处理例子中的逗号和句号
                continue;
            } else {//非标点符号和空格都需要处理
                /*annotationItem = mAnnotationHelper.getAnnotationText(current);
                if (null != annotationItem && annotationItem.visible == true) {//存在标注并且需要显示的时候才drawText
                    annotation = annotationItem.ipa;
                    canvas.drawText(annotation, annotationBound.centerX(), baseline, mSmallTextPaint);
                }*/
                annotation = i % 2 == 0 ? "に" : "にに";
                canvas.drawText(annotation, annotationBound.centerX(), baseline, mSmallTextPaint);
            }
        }
    }

}
