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

    private AnnotationHelper mAnnotationHelper;

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
        mAnnotationHelper = AnnotationHelper.getInstance();

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
        mTextWidth = mTextPaint.measureText(mSourceText);//计算文本的宽度作为view的宽度
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
            height = (int) (getPaddingTop() + mTextHeight + mSmallTextHeight + getPaddingBottom());//高度为文本和标注内容的高度之和
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
        int downBaseline = downRect.top + (downRect.bottom - downRect.top - mFontMetrics.bottom + mFontMetrics.top) / 2 - mFontMetrics.top;
        //处理上面的标注
        Rect upRect = new Rect(0, 0, contentWidth, (int) mSmallTextHeight);
        int upBaseline = upRect.top + (upRect.bottom - upRect.top - mSmallFontMetrics.bottom + mSmallFontMetrics.top) / 2 - mSmallFontMetrics.top;

        //写下面的文本 -> 改为一个字符一个字符绘制
        canvas.drawText(mSourceText, downRect.centerX(), downBaseline, mTextPaint);

        Paint annotationBoundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        annotationBoundPaint.setColor(Color.RED);
        annotationBoundPaint.setStrokeWidth(1);
        annotationBoundPaint.setStyle(Paint.Style.STROKE);

        Paint sourceBoundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sourceBoundPaint.setColor(Color.GREEN);
        sourceBoundPaint.setStrokeWidth(1);
        sourceBoundPaint.setStyle(Paint.Style.STROKE);

        float[] widths = new float[mSourceText.length()];
        mTextPaint.getTextWidths(mSourceText, 0, mSourceText.length(), widths);//重点！使用getTextWidths才能得到真实的文本长度

        int offset = 0, width=0;
        String current, annotation = "";
        Rect annotationBound;
        AnnotationHelper.AnnotationItem annotationItem;

        for (int i = 0; i < mSourceText.length(); i++) {//start from 0
            if (i >= 1) offset += widths[i - 1];//加上前面的偏移量
            width = (int) widths[i];
            //Rect sourceRect = new Rect(offset, (int) mSmallTextHeight, offset + width, (int) (mTextHeight + mSmallTextHeight));
            //canvas.drawRect(sourceRect, sourceBoundPaint);//显示下面的矩形框

            annotationBound = new Rect(offset, 0, offset + width, (int) mSmallTextHeight);//取得上面的标注文本的bound
            //canvas.drawRect(annotationBound, annotationBoundPaint);//显示上面的矩形框

            current = mSourceText.substring(i, i + 1);
            if (current.equalsIgnoreCase("，") || current.equalsIgnoreCase("。") || current.equalsIgnoreCase(" ")) {//todo 不确定这里还有哪些可能的标点符号，暂时只处理例子中的逗号和句号
                continue;
            } else {//非标点符号和空格都需要处理
                annotationItem = mAnnotationHelper.getAnnotationText(current);
                if (null != annotationItem && annotationItem.visible == true) {//存在标注并且需要显示的时候才drawText
                    annotation = annotationItem.ipa;
                    canvas.drawText(annotation, annotationBound.centerX(), upBaseline, mSmallTextPaint);
                }
                /*annotation = i % 2 == 0 ? "に" : "にに";
                canvas.drawText(annotation, annotationBound.centerX(), upBaseline, mSmallTextPaint);*/
            }
        }
    }

}
