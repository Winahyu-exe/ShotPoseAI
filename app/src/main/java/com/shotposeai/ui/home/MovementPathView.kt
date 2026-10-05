package com.shotposeai.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.shotposeai.R
import com.shotposeai.analysis.AchievementRules
import com.shotposeai.analysis.MovementDefinition

/** Responsive native version of the illustrated Figma level path. */
class MovementPathView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private var movements = emptyList<MovementDefinition>()
    private var scores = emptyMap<String, Int>()
    private var onSelect: ((MovementDefinition) -> Unit)? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stepDp = 164
    private var stepPx = 0
    private var topPaddingPx = 0
    private data class Placement(val index: Int, val kind: Int)
    private val nodeSizeDp = 78
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    init { setWillNotDraw(false); clipChildren = false; clipToPadding = false }

    fun submit(items: List<MovementDefinition>, bestScores: Map<String, Int>, onClick: (MovementDefinition) -> Unit) {
        movements = items
        scores = bestScores
        onSelect = onClick
        minimumHeight = dp(items.size * stepDp + 44)
        rebuild()
        requestLayout()
        invalidate()
    }

    fun positionOf(index: Int): Int = (nodeTop(index) - dp(44)).coerceAtLeast(0)
    private fun nodeTop(index: Int) = topPaddingPx + (movements.lastIndex - index).coerceAtLeast(0) * stepPx
    private fun isLeft(index: Int) = index % 2 == 0

    private fun rebuild() {
        removeAllViews()
        val current = movements.indexOfFirst { !AchievementRules.qualifies(scores[it.name] ?: 0) }
            .let { if (it == -1) movements.lastIndex else it }
        movements.forEachIndexed { index, movement ->
            val complete = AchievementRules.qualifies(scores[movement.name] ?: 0)
            val left = isLeft(index)
            val top = nodeTop(index)
            addView(ImageButton(context).apply {
                tag = Placement(index, 0)
                scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    if (complete) intArrayOf(0xff22e83c.toInt(), 0xff0fa728.toInt()) else intArrayOf(0xffff6a2d.toInt(), 0xffc43b13.toInt())).apply {
                    shape = GradientDrawable.OVAL; setStroke(dp(2), Color.WHITE)
                }
                elevation = dp(5).toFloat()
                setImageResource(if (complete) R.drawable.ic_check else R.drawable.soccer_ball_design)
                setPadding(dp(20), dp(20), dp(20), dp(20))
                contentDescription = "${movement.name}, ${if (complete) "achievement tercapai" else "belum tercapai"}, skor terbaik ${(scores[movement.name] ?: 0) / 10.0}. Mulai latihan"
                setOnClickListener { onSelect?.invoke(movement) }
            }, LayoutParams(dp(nodeSizeDp), dp(nodeSizeDp), Gravity.TOP or if (left) Gravity.START else Gravity.END).apply {
                topMargin = top; marginStart = dp(30); marginEnd = dp(30)
            })
            addView(TextView(context).apply {
                tag = Placement(index, 1)
                text = "${movement.level}. ${movement.name}\n${if (complete) movement.badgeName else "Target 8,5/10"}"
                textSize = 11.5f; gravity = Gravity.CENTER; minHeight = dp(54)
                setTextColor(ContextCompat.getColor(context, R.color.text_primary)); setPadding(dp(7), dp(5), dp(7), dp(5))
                background = GradientDrawable().apply { setColor(0xf5ffffff.toInt()); cornerRadius = dp(11).toFloat() }
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LayoutParams(dp(150), LayoutParams.WRAP_CONTENT, Gravity.TOP or if (left) Gravity.START else Gravity.END).apply {
                topMargin = top + dp(82); marginStart = dp(10); marginEnd = dp(10)
            })
            if (index == current) addView(TextView(context).apply {
                tag = Placement(index, 2)
                minHeight = dp(30)
                text = "MULAI!"; textSize = 12f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                background = ContextCompat.getDrawable(context, R.drawable.bg_path_start_pill)
                setOnClickListener { onSelect?.invoke(movement) }
            }, LayoutParams(dp(78), LayoutParams.WRAP_CONTENT, Gravity.TOP or if (left) Gravity.START else Gravity.END).apply {
                topMargin = top - dp(36); marginStart = dp(27); marginEnd = dp(27)
            })
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Measure complete labels before positioning the path. Larger fonts may
        // increase spacing, but the original node sizes and artwork stay intact.
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        var labelHeight = dp(54)
        var pillHeight = dp(30)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val placement = child.tag as? Placement ?: continue
            if (placement.kind == 0) continue
            val params = child.layoutParams as LayoutParams
            if (placement.kind == 1) {
                params.width = minOf(dp(150), (availableWidth - dp(20)).coerceAtLeast(1))
            }
            child.measure(
                MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            if (placement.kind == 1) labelHeight = maxOf(labelHeight, child.measuredHeight)
            else pillHeight = maxOf(pillHeight, child.measuredHeight)
        }
        topPaddingPx = maxOf(dp(30), pillHeight + dp(12))
        stepPx = maxOf(dp(stepDp), dp(82) + labelHeight + pillHeight + dp(14))
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val placement = child.tag as? Placement ?: continue
            val params = child.layoutParams as LayoutParams
            params.topMargin = nodeTop(placement.index) + when (placement.kind) {
                1 -> dp(82)
                2 -> -pillHeight - dp(6)
                else -> 0
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val contentHeight = if (movements.isEmpty()) dp(44) else
            nodeTop(0) + dp(82) + labelHeight + dp(28)
        setMeasuredDimension(measuredWidth,
            resolveSizeAndState(maxOf(minimumHeight, contentHeight), heightMeasureSpec, 0))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0 || movements.isEmpty()) return
        paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), 0xffc8e982.toInt(), 0xff6e9a43.toInt(), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint); paint.shader = null
        paint.color = 0x357aa349; paint.strokeWidth = dp(1).toFloat()
        repeat(movements.size * 24) { i ->
            val x = ((i * dp(83)) % width).toFloat(); val y = ((i * dp(137)) % height).toFloat()
            canvas.drawLine(x, y, x + dp(3), y - dp(7), paint)
        }
        val path = Path()
        movements.indices.forEach { index ->
            val x = nodeCenterX(index); val y = nodeTop(index) + dp(nodeSizeDp / 2).toFloat()
            if (index == 0) path.moveTo(x, y) else {
                val previousX = nodeCenterX(index - 1); val previousY = nodeTop(index - 1) + dp(nodeSizeDp / 2).toFloat()
                val middleY = (previousY + y) / 2f
                path.cubicTo(previousX, middleY, x, middleY, x, y)
            }
        }
        paint.color = 0xff8d9785.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = dp(18).toFloat(); paint.strokeCap = Paint.Cap.ROUND
        paint.pathEffect = DashPathEffect(floatArrayOf(dp(20).toFloat(), dp(15).toFloat()), 0f)
        canvas.drawPath(path, paint); paint.pathEffect = null; paint.style = Paint.Style.FILL
        movements.indices.forEach { index ->
            val x = nodeCenterX(index); val y = nodeTop(index) + dp(nodeSizeDp / 2).toFloat()
            paint.color = 0xff66715f.toInt(); canvas.drawOval(x - dp(46), y + dp(25), x + dp(46), y + dp(39), paint)
            paint.color = 0xffaeb6a2.toInt(); canvas.drawOval(x - dp(45), y + dp(19), x + dp(45), y + dp(33), paint)
        }
    }

    private fun nodeCenterX(index: Int) = if (isLeft(index)) dp(30 + nodeSizeDp / 2).toFloat() else width - dp(30 + nodeSizeDp / 2).toFloat()
}
