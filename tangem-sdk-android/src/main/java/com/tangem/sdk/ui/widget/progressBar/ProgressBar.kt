package com.tangem.sdk.ui.widget.progressBar

import android.animation.TimeInterpolator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.tangem.sdk.R
import kotlin.math.min

/**
 * Created by Anton Zhilenkov on 27/07/2020.
 */
class SdkProgressBar(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    var onProgressChangeListener: ((Float) -> Unit)? = null

    var onIndeterminateChangeListener: ((Boolean) -> Unit)? = null
    var sweepStep = 4

    var angleToSwitchSweepIncrement = 5
    var angleToSwitchSweepDecrement = 320
    var isCountDownActive = false

    private var sweepAngle = 0f

    private var sweepState: SweepState = SweepState.INCREMENT
    var progress: Float = 0f
        set(value) {
            field = if (progress <= progressMax) value else progressMax
            onProgressChangeListener?.invoke(progress)
            invalidate()
        }

    var progressMax: Float = DEFAULT_MAX_VALUE

    var progressBarThickness: Float = 8f
        set(value) {
            field = value.dpToPx()
            progressBarPaint.strokeWidth = field
            requestLayout()
            invalidate()
        }

    var secondaryProgressBarThickness: Float = 8f
        set(value) {
            field = value.dpToPx()
            secondaryProgressBarPaint.strokeWidth = field
            requestLayout()
            invalidate()
        }
    var progressBarColor: Int = Color.BLACK
        set(value) {
            field = value
            progressBarPaint.color = progressBarColor
            invalidate()
        }
    var secondaryProgressBarColor: Int = Color.GRAY
        set(value) {
            field = value
            secondaryProgressBarPaint.color = secondaryProgressBarColor
            invalidate()
        }
    var roundBorder = false
        set(value) {
            field = value
            secondaryProgressBarPaint.strokeCap = if (field) Paint.Cap.ROUND else Paint.Cap.BUTT
            invalidate()
        }
    var isIndeterminate = false
        set(value) {
            field = value
            onIndeterminateChangeListener?.invoke(field)
            progressAnimator?.cancel()
            invalidate()
        }
    private var startAngle: Float = DEFAULT_START_ANGLE
        set(value) {
            field = value
            invalidate()
        }

    private var progressAnimator: ValueAnimator? = null

    private var paintRect = RectF()

    private var progressBarPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private var secondaryProgressBarPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    init {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val attributes = context.theme.obtainStyledAttributes(attrs, R.styleable.SdkProgressBar, 0, 0)

        try {
            isIndeterminate = attributes.getBoolean(R.styleable.SdkProgressBar_pb_is_indeterminate, isIndeterminate)
            progress = attributes.getFloat(R.styleable.SdkProgressBar_pb_progress, progress)
            progressMax = attributes.getFloat(R.styleable.SdkProgressBar_pb_progress_max, progressMax)
            sweepStep = attributes.getInt(R.styleable.SdkProgressBar_pb_sweep_step, sweepStep)
            angleToSwitchSweepIncrement = attributes.getInt(
                R.styleable.SdkProgressBar_pb_sweep_start_increment,
                angleToSwitchSweepIncrement,
            )
            angleToSwitchSweepDecrement = attributes.getInt(
                R.styleable.SdkProgressBar_pb_sweep_start_decrement,
                angleToSwitchSweepDecrement,
            )
            progressBarColor = attributes.getInt(R.styleable.SdkProgressBar_pb_color, progressBarColor)
            progressBarThickness = attributes.getDimension(
                R.styleable.SdkProgressBar_pb_thickness,
                progressBarThickness,
            ).pxToDp()
            secondaryProgressBarColor = attributes.getInt(
                R.styleable.SdkProgressBar_pb_secondary_color,
                secondaryProgressBarColor,
            )
            secondaryProgressBarThickness = attributes.getDimension(
                R.styleable.SdkProgressBar_pb_secondary_thickness,
                secondaryProgressBarThickness,
            ).pxToDp()
            roundBorder = attributes.getBoolean(R.styleable.SdkProgressBar_pb_round_border, roundBorder)
        } finally {
            attributes.recycle()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressAnimator?.cancel()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        invalidate()
    }

    @Suppress("MagicNumber")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isIndeterminate) {
            val step = if (sweepState == SweepState.INCREMENT) sweepStep else sweepStep * 2
            startAngle = (startAngle + step) % 360f

            if (sweepState == SweepState.INCREMENT) sweepAngle += sweepStep else sweepAngle -= sweepStep

            if (sweepAngle >= angleToSwitchSweepDecrement) {
                sweepState = SweepState.DECREMENT
            } else if (sweepAngle <= angleToSwitchSweepIncrement) {
                sweepState = SweepState.INCREMENT
            }
            canvas.drawArc(paintRect, startAngle, sweepAngle, false, progressBarPaint)
        } else {
            val realProgress = progress * DEFAULT_MAX_VALUE / progressMax
            val angle = 360 * realProgress / 100
            canvas.drawOval(paintRect, secondaryProgressBarPaint)
            canvas.drawArc(paintRect, DEFAULT_START_ANGLE, angle, false, progressBarPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val width = getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val min = min(width, height)
        setMeasuredDimension(min, min)
        val highStroke = if (progressBarThickness > secondaryProgressBarThickness) {
            progressBarThickness
        } else {
            secondaryProgressBarThickness
        }
        paintRect.set(0 + highStroke / 2, 0 + highStroke / 2, min - highStroke / 2, min - highStroke / 2)
    }

    fun setProgressWithAnimation(
        progress: Float,
        duration: Long? = DEFAULT_ANIMATION_DURATION,
        interpolator: TimeInterpolator? = null,
        startDelay: Long? = null,
    ) {
        progressAnimator?.cancel()
        val animator = ValueAnimator.ofFloat(this.progress, progress)
        progressAnimator = animator

        duration?.also { animator.duration = it }
        interpolator?.also { animator.interpolator = it }
        startDelay?.also { animator.startDelay = it }

        animator.addUpdateListener { animation ->
            val fValue = animation.animatedValue as? Float ?: return@addUpdateListener

            if (!isIndeterminate) this.progress = fValue
        }
        animator.start()
    }

    fun setProgressBarColorWithAnimation(color: Int) {
        animateProgressBarColorChanges(
            color,
            progressBarColor,
            listener = ValueAnimator.AnimatorUpdateListener {
                progressBarColor = it.animatedValue as Int
            },
        )
    }

    private fun animateProgressBarColorChanges(
        toColorId: Int,
        fromColorId: Int = -1,
        duration: Long = DEFAULT_ANIMATION_DURATION,
        listener: ValueAnimator.AnimatorUpdateListener,
    ) {
        if (fromColorId == -1) return

        ValueAnimator().apply {
            setIntValues(fromColorId, toColorId)
            setEvaluator(InnerArbEvaluator())
            this.duration = duration
            addUpdateListener(listener)
        }.start()
    }

    private fun Float.dpToPx(): Float = this * Resources.getSystem().displayMetrics.density

    private fun Float.pxToDp(): Float = this / Resources.getSystem().displayMetrics.density

    companion object {
        private const val DEFAULT_MAX_VALUE = 100f
        private const val DEFAULT_START_ANGLE = 270f
        private const val DEFAULT_ANIMATION_DURATION = 300L
    }
}

enum class SweepState {
    INCREMENT, DECREMENT
}

class InnerArbEvaluator : TypeEvaluator<Int> {

    override fun evaluate(fraction: Float, startValue: Int, endValue: Int): Int {
        val startA = startValue.shr(bitCount = 24).and(other = 0xff)
        val startR = startValue.shr(bitCount = 16).and(other = 0xff)
        val startG = startValue.shr(bitCount = 8).and(other = 0xff)
        val startB = startValue.and(other = 0xff)

        val endA = endValue.shr(bitCount = 24).and(other = 0xff)
        val endR = endValue.shr(bitCount = 16).and(other = 0xff)
        val endG = endValue.shr(bitCount = 8).and(other = 0xff)
        val endB = endValue.and(other = 0xff)

        return (
            (startA + (fraction * (endA - startA)).toInt()).shl(bitCount = 24)
                or (startR + (fraction * (endR - startR)).toInt()).shl(bitCount = 16)
                or (startG + (fraction * (endG - startG)).toInt()).shl(bitCount = 8)
                or startB + (fraction * (endB - startB)).toInt()
            )
    }
}
