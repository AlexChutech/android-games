package com.gomoku.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

class GomokuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val GRID_SIZE = GameEngine.SIZE
        private const val PADDING_RATIO = 0.08f
    }

    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#DEB887") }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }

    private val blackStonePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val whiteStonePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val lastMovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private var cellSize = 0f
    private var padding = 0f
    private var onMoveListener: ((Int, Int) -> Unit)? = null
    private var board: Array<IntArray>? = null
    private var lastMove: Pair<Int, Int>? = null

    init {
        val blackCenter = Color.parseColor("#555555")
        val blackEdge = Color.parseColor("#111111")
        blackStonePaint.shader = RadialGradient(0f, 0f, 1f,
            intArrayOf(blackCenter, blackEdge),
            floatArrayOf(0.3f, 1f),
            Shader.TileMode.CLAMP
        )

        val whiteCenter = Color.parseColor("#FFFFFF")
        val whiteEdge = Color.parseColor("#CCCCCC")
        whiteStonePaint.shader = RadialGradient(0f, 0f, 1f,
            intArrayOf(whiteCenter, whiteEdge),
            floatArrayOf(0.3f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    fun setOnMoveListener(listener: (Int, Int) -> Unit) {
        onMoveListener = listener
    }

    fun updateBoard(board: Array<IntArray>, lastMove: Pair<Int, Int>?) {
        this.board = board
        this.lastMove = lastMove
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = minOf(measureSize(widthMeasureSpec), measureSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
    }

    private fun measureSize(measureSpec: Int): Int {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec)
        return if (mode == MeasureSpec.EXACTLY) size else 900
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        padding = w * PADDING_RATIO
        cellSize = (w - 2 * padding) / (GRID_SIZE - 1)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw board background
        canvas.drawRoundRect(padding / 2, padding / 2,
            width - padding / 2, height - padding / 2, 10f, 10f, boardPaint)

        // Draw grid lines
        for (i in 0 until GRID_SIZE) {
            val pos = padding + i * cellSize
            canvas.drawLine(padding, pos, width - padding, pos, linePaint)
            canvas.drawLine(pos, padding, pos, height - padding, linePaint)
        }

        // Draw star points (天元 and 星位)
        val starPoints = arrayOf(
            3 to 3, 3 to 11, 11 to 3, 11 to 11,  // 星位
            7 to 7  // 天元
        )
        dotPaint.style = Paint.Style.FILL
        for ((r, c) in starPoints) {
            canvas.drawCircle(
                padding + c * cellSize,
                padding + r * cellSize,
                cellSize * 0.12f, dotPaint
            )
        }

        // Draw stones
        val b = board ?: return
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (b[r][c] != GameEngine.EMPTY) {
                    drawStone(canvas, r, c, b[r][c])
                }
            }
        }

        // Draw last move indicator
        lastMove?.let { (r, c) ->
            val cx = padding + c * cellSize
            val cy = padding + r * cellSize
            canvas.drawCircle(cx, cy, cellSize * 0.2f, lastMovePaint)
        }
    }

    private fun drawStone(canvas: Canvas, row: Int, col: Int, player: Int) {
        val cx = padding + col * cellSize
        val cy = padding + row * cellSize
        val radius = cellSize * 0.43f

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(radius, radius)

        val paint = if (player == GameEngine.BLACK) blackStonePaint else whiteStonePaint
        canvas.drawCircle(0f, 0f, 1f, paint)
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true

        val x = event.x
        val y = event.y

        val col = ((x - padding) / cellSize).roundToInt()
        val row = ((y - padding) / cellSize).roundToInt()

        if (row in 0 until GRID_SIZE && col in 0 until GRID_SIZE) {
            onMoveListener?.invoke(row, col)
        }
        return true
    }
}
