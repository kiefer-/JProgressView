package com.hi.dhl.jprogressview

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Looper
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator

/**
 * <pre>
 *     custom circle progress view
 *
 *     @author: dhl
 *     @date  : 2020/4/27
 * </pre>
 */

class JProgressView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : View(context, attributeSet, defStyleAttr, defStyleRes) {

    private val mCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mCenterTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val mRect = RectF()
    private val mProgressRect = RectF()

    private var mCenterX = 0f
    private var mCenterY = 0f
    private var mRadius = 0f
    private var mRound = 0f
    private var mWidth = 0f
    private var mHeight = 0f
    private var mMaxProgress = 100

    private var mCircleStrokeWidth = DEFAULT_STROKE_WIDTH
    private var mCircleColor = Color.GRAY

    private var mProgressStrokeWidth = DEFAULT_STROKE_WIDTH
    private var mProgressColor = Color.GREEN

    private var mCenterTextSize = sp2Px(20f)
    private var mCenterTextColor = Color.BLUE
    private var mProgress = 0f

    /** 变量mSweepAngle对应四个值，代表进度条开始的方向，详见 [getSweepAngle] */
    private var mSweepAngle = -90f
    private var mDuration = DEFAULT_DURATION
    private var mValueAnimator: ValueAnimator? = null
    private var mReverse: Boolean = false

    /** 变量mType 对应三个值 [TYPE_RECT]、[TYPE_CIRCLE]、[TYPE_CIRCLE] */
    private var mType: Int = TYPE_CIRCLE
    private var mTextAlign: Int = TYPE_CIRCLE
    private var mShowText = false
    private var mLabelText = ""

    init {
        initialize(attributeSet)
    }

    fun initialize(attributeSet: AttributeSet?) {
        attributeSet?.let { attr ->
            val typedArray: TypedArray =
                context.obtainStyledAttributes(attr, R.styleable.JProgressView)

            try {
                mCircleColor =
                    typedArray.getColor(
                        R.styleable.JProgressView_progress_color_background,
                        mCircleColor
                    )
                mCircleStrokeWidth = typedArray.getDimension(
                    R.styleable.JProgressView_progress_paint_bg_width,
                    mCircleStrokeWidth
                )

                mProgressColor =
                    typedArray.getColor(
                        R.styleable.JProgressView_progress_color,
                        mProgressColor
                    )
                mProgressStrokeWidth = typedArray.getDimension(
                    R.styleable.JProgressView_progress_paint_value_width,
                    mProgressStrokeWidth
                )

                mCenterTextColor = typedArray.getColor(
                    R.styleable.JProgressView_progress_text_color,
                    mCenterTextColor
                )
                mCenterTextSize = typedArray.getDimension(
                    R.styleable.JProgressView_progress_text_size,
                    mCenterTextSize
                )

                mDuration =
                    typedArray.getInteger(
                        R.styleable.JProgressView_progress_animate_duration,
                        mDuration.toInt()
                    ).toLong()

                val direction =
                    typedArray.getInteger(R.styleable.JProgressView_progress_circle_sweep_angle, 0)
                mSweepAngle = getSweepAngle(direction)
                mType = typedArray.getInteger(R.styleable.JProgressView_progress_type, mType)
                mProgress =
                    typedArray.getInteger(R.styleable.JProgressView_progress_value, 0).toFloat()

                mMaxProgress = typedArray.getInteger(
                    R.styleable.JProgressView_progress_value_max,
                    mMaxProgress
                )

                mTextAlign =
                    typedArray.getInteger(
                        R.styleable.JProgressView_progress_rect_text_align,
                        mTextAlign
                    )

                mShowText =
                    typedArray.getBoolean(
                        R.styleable.JProgressView_progress_text_visible,
                        mShowText
                    )

                mLabelText =
                    typedArray.getString(R.styleable.JProgressView_progress_text) ?: mLabelText

                mRadius =
                    typedArray.getDimension(R.styleable.JProgressView_progress_rect_radius, mRadius)
            } catch (e: Exception) {
            } finally {
                typedArray.recycle()
            }
        }

        initPaint(mCirclePaint, mCircleColor, mCircleStrokeWidth)
        initPaint(mProgressPaint, mProgressColor, mProgressStrokeWidth)
        initTextPaint(mCenterTextPaint)
    }

    fun setProgress(progress: Float): JProgressView {
        mProgress = progress
        resetProgressValue(progress)
        return this
    }

    fun setMaxProgress(maxProgress: Int): JProgressView {
        mMaxProgress = maxProgress
        return this
    }

    fun startAnimal() {
        initAnimation()
        mValueAnimator?.start()
    }

    fun setReverse(reverse: Boolean): JProgressView {
        mReverse = reverse
        return this
    }

    fun stopAnimal() {
        mValueAnimator?.end()
    }

    fun getProgressRectValue(): Float = (mProgress * mWidth) / mMaxProgress

    private fun initPaint(paint: Paint, color: Int, strokeWidth: Float) {
        paint.color = color
        paint.isAntiAlias = true
        if (mType == TYPE_RECT || mType == TYPE_ROUND_RECT) {
            paint.style = Paint.Style.FILL
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth
            paint.strokeCap = Paint.Cap.ROUND
        }
    }

    private fun initTextPaint(paint: TextPaint) {
        paint.color = mCenterTextColor
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.textSize = mCenterTextSize
    }

    fun getSweepAngle(direction: Int): Float {
        var sweepAngle = 0f
        when (direction) {
            0 -> sweepAngle = 180f
            1 -> sweepAngle = -90f
            2 -> sweepAngle = 0f
            3 -> sweepAngle = 90f
        }
        return sweepAngle
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mWidth = measuredWidth - paddingLeft.toFloat() - paddingRight.toFloat()
        mHeight = measuredHeight - paddingTop.toFloat() - paddingBottom.toFloat()

        if (mType == TYPE_CIRCLE) {
            mCenterX = mWidth / 2
            mCenterY = mWidth / 2
            val min = Math.min(mCenterX, mCenterY)
            if (mCenterX != mCenterY) {
                mCenterY = min
                mCenterY = min
            }

            if (mRadius < 1) {
                mRadius = min - Math.max(mCircleStrokeWidth, mProgressStrokeWidth)
            }

            mRect.set(
                min - mRadius,
                min - mRadius,
                min + mRadius,
                min + mRadius
            )
        } else if (mType == TYPE_RECT || mType == TYPE_ROUND_RECT) {

            mRound = mHeight / 2
            mRect.set(
                paddingLeft.toFloat(),
                paddingTop.toFloat(),
                mWidth + paddingLeft,
                mHeight + paddingTop
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mType == TYPE_CIRCLE) {
            drawCircle(canvas)
        } else if (mType == TYPE_ROUND_RECT) {
            drawRoundRect(canvas)
        } else if (mType == TYPE_RECT) {
            drawRect(canvas)
        }
    }

    private fun drawRect(canvas: Canvas) {
        canvas.drawRect(mRect, mCirclePaint)
        mProgressRect.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            paddingLeft + getProgressRectValue(),
            paddingTop + mHeight
        )
        canvas.drawRect(mProgressRect, mProgressPaint)


        val fontMetrics = mCenterTextPaint.fontMetrics
        val text: String
        if (mLabelText.isEmpty()) {
            text =
                String.format(context.getString(R.string.circle_progress_value), mProgress.toInt())
        } else {
            text = mLabelText
        }
        val baseLine = mRect.centerY() - fontMetrics.top / 2 - fontMetrics.bottom / 2
        val textWidth = mCenterTextPaint.measureText(text, 0, text.length)
        val x: Float
        val spec = 10f
        when (mTextAlign) {
            TEXT_ALIGN_LEFT -> x = spec
            TEXT_ALIGN_CENTER -> x = mRect.centerX() - textWidth / 2
            TEXT_ALIGN_RIGHT -> x = mRect.width() - textWidth - spec
            else -> x = mRect.centerX() - textWidth / 2
        }

        if (mShowText) {
            canvas.drawText(
                text,
                x,
                baseLine,
                mCenterTextPaint
            )
        }
    }

    private fun drawRoundRect(canvas: Canvas) {
        canvas.drawRoundRect(mRect, mRound, mRound, mCirclePaint)
        mProgressRect.set(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            paddingLeft + getProgressRectValue(),
            paddingTop + mHeight
        )
        canvas.drawRoundRect(mProgressRect, mRound, mRound, mProgressPaint)

        val fontMetrics = mCenterTextPaint.fontMetrics
        val text: String
        if (mLabelText.isEmpty()) {
            text =
                String.format(context.getString(R.string.circle_progress_value), mProgress.toInt())
        } else {
            text = mLabelText
        }
        val baseLine = mRect.centerY() - fontMetrics.top / 2 - fontMetrics.bottom / 2
        val textWidth = mCenterTextPaint.measureText(text, 0, text.length)
        val x: Float
        val spec = 10f
        when (mTextAlign) {
            TEXT_ALIGN_LEFT -> x = spec
            TEXT_ALIGN_CENTER -> x = mRect.centerX() - textWidth / 2
            TEXT_ALIGN_RIGHT -> x = mRect.width() - textWidth - spec
            else -> x = mRect.centerX() - textWidth / 2
        }


        if (mShowText) {
            canvas.drawText(
                text,
                x,
                baseLine,
                mCenterTextPaint
            )
        }
    }

    private fun drawCircle(canvas: Canvas) {
        // draw circle
        canvas.drawCircle(mCenterX, mCenterY, mRadius, mCirclePaint)
        // draw progress
        canvas.drawArc(
            mRect, mSweepAngle, (mProgress / mMaxProgress) * 360,
            false, mProgressPaint
        )
        // draw text
        val fontMetrics = mCenterTextPaint.fontMetrics
        val text: String
        if (mLabelText.isEmpty()) {
            text =
                String.format(context.getString(R.string.circle_progress_value), mProgress.toInt())
        } else {
            text = mLabelText
        }
        val baseLine = mRect.centerY() - fontMetrics.top / 2 - fontMetrics.bottom / 2
        val textWidth = mCenterTextPaint.measureText(text, 0, text.length)

        if (mShowText) {
            canvas.drawText(
                text,
                mRect.centerX() - textWidth / 2,
                baseLine,
                mCenterTextPaint
            )
        }

    }

    private fun initAnimation() {
        if (mReverse) {
            mValueAnimator = ValueAnimator.ofFloat(mProgress, 0f)
        } else {
            mValueAnimator = ValueAnimator.ofFloat(0f, mProgress)
        }

        mValueAnimator?.run {
            if (isRunning) stopAnimal()
            duration = mDuration
            startDelay = DEFLAUT_START_DELAY
            interpolator = LinearInterpolator()
            addUpdateListener {
                val preogress = it.animatedValue as Float
                resetProgressValue(preogress)
            }
        }
    }

    private fun resetProgressValue(preogress: Float) {
        mProgress = preogress
        if (isMainThread()) {
            invalidate()
        } else {
            postInvalidate()
        }
    }

    private fun isMainThread() = Looper.getMainLooper().thread == Thread.currentThread()

    private fun sp2Px(textSize: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            textSize,
            resources.displayMetrics
        )
    }

    companion object {
        private const val DEFAULT_STROKE_WIDTH: Float = 10f
        private const val DEFAULT_DURATION = 2000L
        private const val DEFLAUT_START_DELAY = 0L
        const val TYPE_RECT = 0
        const val TYPE_CIRCLE = 1
        const val TYPE_ROUND_RECT = 2
        const val TEXT_ALIGN_LEFT = 0
        const val TEXT_ALIGN_CENTER = 1
        const val TEXT_ALIGN_RIGHT = 2
    }
}