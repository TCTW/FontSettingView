package com.rex.fontsetting.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.customview.widget.ViewDragHelper;

import com.rex.fontsetting.R;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 分段滑动带吸附效果layout
 * <p>
 * <p>
 * TOOD 后期支持：
 * 支持文字可以上中下设置
 * 支持多个滑块
 * </p>
 *
 * @author liruipeng
 * @data 2018.12.10
 */
public class FontSetSeekbar extends FrameLayout {

    private final String TAG = "FontSetSeekbar";
    /**
     * layout width
     */
    private int width;
    /**
     * layout height
     */
    private int height;

    private int downX = 0;

    private int downY = 0;

    private int upX = 0;

    private int upY = 0;

    private Paint mLinePaint;

    private Paint mCursorPaint;

    private Paint mTextPaint;

    private ResponseOnTouch responseOnTouch;
    /**
     * 默认颜色
     */
    private int[] colors = new int[]{0xffbbbbbd, 0xff1c1c1c};
    /**
     * 刻度文字, 代表刻度个
     */
    private ArrayList<String> mCursorTitle;

    /**
     * 选择的刻度（0 - section_title.size()）
     */
    private int mCursorSelect;
    /**
     * 滑块宽
     */
    private int thumbWidth = 80;
    /**
     * 滑块高
     */
    private int thumbHeight = 80;
    /**
     * 滑块距离父View顶部距离
     */
    private int thumbTop;
    /**
     * 进度条距离父View左边距
     */
    private int seekLineleftPadding;
    /**
     * 进度条距离父View右边距
     */
    private int seekLineRightPadding;
    /**
     * 进度条上刻度的高度
     */
    private int cursorLineHeight = 25;
    /**
     * 进度条起始Y坐标
     */
    private int seekLineStartY;
    /**
     * 进度条终点Y坐标
     */
    private int seekLineEndY;
    /**
     * 进度条起始X坐标
     */
    private int seekLineStartX;
    /**
     * 进度条终点X坐标
     */
    private int seekLineEndX;
    /**
     * 滑块滑动过程中距离父View最小左边距
     */
    private int thumbMoveMinLeft;
    /**
     * 滑块滑动过程中距离父View最大左边距
     */
    private int thumbMoveMaxLeft;
    /**
     * 滑块View
     */
    private View thumbView;
    /**
     * 默认刻度响应区域, 已刻度为中心的正方形点击响应区域半径
     */
    private float cursorClickScope = 30;
    /**
     * 将进度条拆分，主要用于判断滑块的自动吸附(回弹)效果, 分割后每部分长度
     * 将进度条等分成（刻度个数-1)段, 每段长度
     */
    private int seekLineCursorSplit;
    /**
     * 文字顶部距离刻度间距
     */
    private float textMarginTop;
    /**
     * 文字高度
     */
    private float textHeight;

    public FontSetSeekbar(Context context) {
        super(context);
    }

    public FontSetSeekbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FontSetSeekbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FontSetSeekBar, defStyleAttr, 0);

        int lineColor = a.getColor(R.styleable.FontSetSeekBar_line_color, colors[0]);
        int cursorColor = a.getColor(R.styleable.FontSetSeekBar_cursor_color, colors[0]);
        float lineSize = a.getDimension(R.styleable.FontSetSeekBar_line_size, 2f);
        cursorLineHeight = a.getDimensionPixelOffset(R.styleable.FontSetSeekBar_cursor_height, 25);
        float cursorLineWidth = a.getDimension(R.styleable.FontSetSeekBar_cursor_width, 2f);
        seekLineleftPadding = a.getDimensionPixelOffset(R.styleable.FontSetSeekBar_line_marginLeft, 0);
        seekLineRightPadding = a.getDimensionPixelOffset(R.styleable.FontSetSeekBar_line_marginRight, 0);
        float defLineTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics());
        int lineTextColor = a.getColor(R.styleable.FontSetSeekBar_line_textColor, colors[1]);
        float lineTextSize = a.getDimension(R.styleable.FontSetSeekBar_line_textSize, defLineTextSize);
        textMarginTop = a.getDimension(R.styleable.FontSetSeekBar_line_textMarginTop, 0f);
        cursorClickScope = a.getDimension(R.styleable.FontSetSeekBar_cursor_clickRadius, 30f);

        initViewDragHelper();
        mCursorSelect = 0;
        //进度线Paint
        mLinePaint = new Paint(Paint.DITHER_FLAG);
        mLinePaint.setAntiAlias(true);//锯齿不显示
        mLinePaint.setStrokeWidth(lineSize);
        mLinePaint.setColor(lineColor);
        //刻度Paint
        mCursorPaint = new Paint(Paint.DITHER_FLAG);
        mCursorPaint.setAntiAlias(true);//锯齿不显示
        mCursorPaint.setStrokeWidth(cursorLineWidth);
        mCursorPaint.setColor(cursorColor);
        //文字Paint
        mTextPaint = new Paint(Paint.DITHER_FLAG);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(lineTextSize);
        mTextPaint.setColor(lineTextColor);
        initData();
    }

    /**
     * 实例化后调用，设置bar的段数和文字
     */
    private void initData() {
        if (null == mCursorTitle) {
            mCursorTitle = new ArrayList<>();
        }
        String[] str = new String[]{"标准", "大号", "特大"};
        mCursorTitle.clear();
        mCursorTitle.addAll(Arrays.asList(str));
        initTextHeight();
    }

    public void setData(ArrayList<String> section) {
        if (null == mCursorTitle) {
            mCursorTitle = new ArrayList<>();
        }
        if (section != null) {
            mCursorTitle.clear();
            mCursorTitle.addAll(section);
        } else {//默认刻度
            String[] str = new String[]{"标准", "大号", "特大"};
            mCursorTitle.clear();
            mCursorTitle.addAll(Arrays.asList(str));
        }
        initTextHeight();
    }

    /**
     * 计算文字的高度
     */
    public void initTextHeight() {
        if (mCursorTitle.size() > 0) {
            Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
            textHeight = fontMetrics.bottom - fontMetrics.top;
//            int textHeight2 = ((int) Math.ceil(fontMetrics.descent - fontMetrics.top) + 2) / 2;
        } else {
            textHeight = 0;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        width = widthSize;
        height = heightSize;
        //TODO 计算宽度，以后再说
        //计算layout高度
        switch (heightMode) {
            case MeasureSpec.AT_MOST://wrap_content
                int chileViewHeight = 0;
                if (getChildCount() > 0) {
                    chileViewHeight = getChildAt(0).getMeasuredHeight();
                }
                if (chileViewHeight > cursorLineHeight) {//滑块高度大于刻线高度
                    if ((chileViewHeight >> 1) >= textHeight + textMarginTop) {
                        height = getPaddingTop() + getPaddingBottom() + chileViewHeight;
                    } else {
                        height = getPaddingTop() + getPaddingBottom() + (chileViewHeight >> 1) + (cursorLineHeight >> 1) + (int) textHeight + (int) (textMarginTop);
                    }
                } else {//滑块高度小于刻线高度,layout的高度以刻线高度来计算
                    height = getPaddingTop() + getPaddingBottom() + cursorLineHeight + (int) textHeight + (int) (textMarginTop);
                }
                break;
            case MeasureSpec.EXACTLY://match_parent
                break;
            case MeasureSpec.UNSPECIFIED:
                break;
        }
        setMeasuredDimension(width, height);
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        //计算进度线起始X坐标
        seekLineStartX = seekLineleftPadding;
        seekLineEndX = width - seekLineRightPadding;

        if (mCursorTitle.size() > 0) {
            //将进度条等分成（刻度个数-1)段, 每段长度
            seekLineCursorSplit = (seekLineEndX - seekLineStartX) / (mCursorTitle.size() - 1);
        }

        if (getChildCount() > 0) {
            if (null == thumbView) {
                thumbView = getChildAt(0);
            }
            thumbWidth = thumbView.getWidth();
            thumbHeight = thumbView.getHeight();
            int thumbViewLeft = seekLineStartX + mCursorSelect * seekLineCursorSplit - (thumbWidth >> 1);
            int thumbViewRight = thumbViewLeft + thumbWidth;
            thumbTop = 0;
            if (thumbHeight < cursorLineHeight) {//滑块高度小于刻线高度
                //计算进度新起始Y坐标
                seekLineStartY = getPaddingTop() + (cursorLineHeight >> 1);
                seekLineEndY = seekLineStartY;
            } else {
                //计算进度新起始Y坐标
                seekLineStartY = thumbView.getTop() + (thumbHeight >> 1);
                seekLineEndY = seekLineStartY;
            }
            thumbTop = seekLineStartY - (thumbHeight >> 1);
            thumbView.layout(thumbViewLeft, thumbTop, thumbViewRight, thumbTop + thumbHeight);
            //计算滑块滑动过程中距离父View最小左边距
            thumbMoveMinLeft = seekLineStartX - (thumbWidth >> 1);
            //计算滑块滑动过程中距离父View最大左边距
            thumbMoveMaxLeft = seekLineEndX - (thumbWidth >> 1);
        } else {
            //计算进度新起始Y坐标
            seekLineStartY = getPaddingTop() + (cursorLineHeight >> 1);
            seekLineEndY = seekLineStartY;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //进度条
        canvas.drawLine(seekLineStartX, seekLineStartY, seekLineEndX, seekLineEndY, mLinePaint);

        float baseLineY = Math.abs(mTextPaint.getFontMetrics().top) + (seekLineEndY + (cursorLineHeight >> 1)) + textMarginTop;
        for (int i = 0; i < mCursorTitle.size(); i++) {
            canvas.drawLine(
                    seekLineStartX + seekLineCursorSplit * i,
                    seekLineStartY - (cursorLineHeight >> 1),
                    seekLineStartX + seekLineCursorSplit * i,
                    seekLineEndY + (cursorLineHeight >> 1), mCursorPaint);
            // 文字中心X
            float textCenterX = mTextPaint.measureText(mCursorTitle.get(i)) / 2;
            if (i == 0) {
                canvas.drawText(mCursorTitle.get(i), seekLineStartX + seekLineCursorSplit * i, baseLineY, mTextPaint);
            } else if (i == mCursorTitle.size() - 1) {
                canvas.drawText(mCursorTitle.get(i), seekLineStartX + seekLineCursorSplit * i - textCenterX * 2, baseLineY, mTextPaint);
            } else {
                canvas.drawText(mCursorTitle.get(i), seekLineStartX + seekLineCursorSplit * i - textCenterX, baseLineY, mTextPaint);
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return dragHelper.shouldInterceptTouchEvent(ev);
    }

    /**
     * 按下位置
     */
    int downEventPosition;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = (int) event.getX();
                downY = (int) event.getY();
                downEventPosition = checkTouchEvent(downX, downY);
                break;
            case MotionEvent.ACTION_UP:
                if (isDraging) {
                    break;
                }
                upX = (int) event.getX();
                upY = (int) event.getY();
                if (downEventPosition >= 0 && downEventPosition == checkTouchEvent(upX, upY)) {
                    //有效的刻度点击
                    setProgress(downEventPosition);
                    if (null != responseOnTouch) {
                        responseOnTouch.onTouchResponse(downEventPosition);
                    }
                }
                break;
        }
        dragHelper.processTouchEvent(event);
        return true;
    }

    /**
     * 检查按下和按起事件
     *
     * @return 按下和按起事件是否有
     */
    private int checkTouchEvent(int x, int y) {
        float clickTopY = seekLineStartY - cursorClickScope;
        float clickBottomY = seekLineStartY + cursorClickScope;
        if (y < clickTopY || y > clickBottomY) {//点击的点没有在指定Y坐标范围内
            return -1;
        }
        int position = -1;
        for (int i = 0; i < mCursorTitle.size(); i++) {
            if (x >= (seekLineStartX + seekLineCursorSplit * i) - cursorClickScope && x <= (seekLineStartX + seekLineCursorSplit * i) + cursorClickScope) {
                position = i;
                break;
            }
        }
        return position;
    }


    //设置监听
    public void setResponseOnTouch(ResponseOnTouch response) {
        responseOnTouch = response;
    }

    //设置进度
    public void setProgress(int progress) {
        if (progress >= mCursorTitle.size()) {
            throw new IndexOutOfBoundsException();
        }
        mCursorSelect = progress;
        requestLayout();
    }


    private ViewDragHelper dragHelper;

    private boolean isDraging;

    private void initViewDragHelper() {
        dragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelper.Callback() {
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                return true;
            }

            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                return thumbTop;
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                if (left < thumbMoveMinLeft) {
                    return thumbMoveMinLeft;
                }
                if (left > thumbMoveMaxLeft) {
                    return thumbMoveMaxLeft;
                }
                return left;
            }

            @Override
            public void onViewDragStateChanged(int state) {
                if (state == 0) {//停止拖动
                    if (null != responseOnTouch) {
                        responseOnTouch.onTouchResponse(mCursorSelect);
                    }
                    isDraging = false;
                } else {
                    isDraging = true;
                }
            }

            @Override
            public void onViewCaptured(@NonNull View capturedChild, int activePointerId) {

            }

            @Override
            public void onViewReleased(@NonNull View releasedChild, float xvel, float yvel) {
                dragHelper.settleCapturedViewAt(thumbScrollToLeft(releasedChild.getLeft() + (thumbWidth >> 1)), thumbTop);
                invalidate();
            }
        });
    }

    /**
     * 计算手指放开的时候滑块中心应该滚动到的位置, 吸附效果
     *
     * @param centerLeft 滑块中心点距离父布局左边距
     */
    private int thumbScrollToLeft(int centerLeft) {
        if (mCursorTitle.size() == 0) {
            mCursorSelect = 0;
            return thumbMoveMinLeft;
        }
        mCursorSelect = 0;
        //滑块中心点第一个刻度距离
        int tempLenght = Math.abs(centerLeft - seekLineStartX);
        for (int i = 1; i < mCursorTitle.size(); i++) {
            int lenght = Math.abs(centerLeft - (seekLineStartX + seekLineCursorSplit * i));
            if (tempLenght > lenght) {
                tempLenght = lenght;
                mCursorSelect = i;
            }
        }
        Log.i(TAG, "thumbScrollToLeft() mCursorSelect=" + mCursorSelect);
        return thumbMoveMinLeft + mCursorSelect * seekLineCursorSplit;
    }

    @Override
    public void computeScroll() {
        if (dragHelper != null && dragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    public interface ResponseOnTouch {

        public void onTouchResponse(int resp);
    }

}
