package com.xiangqi.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

class XiangqiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F4D58D")
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8B4513")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val validMovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#804CAF50")
        style = Paint.Style.FILL
    }
    private val lastMovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5722")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val lastMoveBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#40FF5722")
        style = Paint.Style.FILL
    }

    private var cellWidth = 0f
    private var cellHeight = 0f
    private var paddingX = 0f
    private var paddingY = 0f
    private var pieceRadius = 0f

    private var engine: XiangqiEngine? = null
    private var selectedRow = -1
    private var selectedCol = -1
    private var validMoves = mutableListOf<Move>()
    private var lastMove: Move? = null

    private var onMoveListener: ((Int, Int, Int, Int) -> Unit)? = null

    fun setEngine(engine: XiangqiEngine) {
        this.engine = engine
        invalidate()
    }

    fun setLastMove(move: Move?) {
        lastMove = move
        invalidate()
    }

    fun setSelectedPosition(row: Int, col: Int) {
        selectedRow = row
        selectedCol = col
        validMoves.clear()
        engine?.let { e ->
            if (row >= 0 && col >= 0) {
                for (tr in 0 until XiangqiEngine.ROWS) {
                    for (tc in 0 until XiangqiEngine.COLS) {
                        if (e.isValidMove(row, col, tr, tc)) {
                            validMoves.add(Move(row, col, tr, tc))
                        }
                    }
                }
            }
        }
        invalidate()
    }

    fun clearSelection() {
        selectedRow = -1
        selectedCol = -1
        validMoves.clear()
        invalidate()
    }

    fun setOnMoveListener(listener: (Int, Int, Int, Int) -> Unit) {
        onMoveListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = measureSize(widthMeasureSpec)
        val height = (width * XiangqiEngine.ROWS / XiangqiEngine.COLS.toFloat()).roundToInt()
        setMeasuredDimension(width, height)
    }

    private fun measureSize(measureSpec: Int): Int {
        val mode = MeasureSpec.getMode(measureSpec)
        val size = MeasureSpec.getSize(measureSpec)
        return if (mode == MeasureSpec.EXACTLY) size else 900
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        paddingX = w * 0.05f
        paddingY = h * 0.03f
        cellWidth = (w - 2 * paddingX) / (XiangqiEngine.COLS - 1)
        cellHeight = (h - 2 * paddingY) / (XiangqiEngine.ROWS - 1)
        pieceRadius = minOf(cellWidth, cellHeight) * 0.42f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw board background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), boardPaint)

        // Draw grid lines
        drawBoardGrid(canvas)

        // Draw river (楚河汉界)
        drawRiver(canvas)

        // Draw valid moves
        drawValidMoves(canvas)

        // Draw pieces
        drawPieces(canvas)

        // Draw last move indicator
        drawLastMove(canvas)

        // Draw selection
        if (selectedRow >= 0 && selectedCol >= 0) {
            val x = paddingX + selectedCol * cellWidth
            val y = paddingY + selectedRow * cellHeight
            canvas.drawCircle(x, y, pieceRadius + 5f, selectedPaint)
        }
    }

    private fun drawBoardGrid(canvas: Canvas) {
        // Horizontal lines
        for (row in 0 until XiangqiEngine.ROWS) {
            val y = paddingY + row * cellHeight
            canvas.drawLine(paddingX, y, paddingX + (XiangqiEngine.COLS - 1) * cellWidth, y, linePaint)
        }

        // Vertical lines (with gap for river)
        for (col in 0 until XiangqiEngine.COLS) {
            val x = paddingX + col * cellWidth
            // Top half
            canvas.drawLine(x, paddingY, x, paddingY + 4 * cellHeight, linePaint)
            // Bottom half
            canvas.drawLine(x, paddingY + 5 * cellHeight, x, paddingY + 9 * cellHeight, linePaint)
        }

        // Border lines (full vertical)
        canvas.drawLine(paddingX, paddingY, paddingX, paddingY + 9 * cellHeight, linePaint)
        canvas.drawLine(paddingX + 8 * cellWidth, paddingY, paddingX + 8 * cellWidth, paddingY + 9 * cellHeight, linePaint)

        // Palace diagonals
        // Top palace
        canvas.drawLine(paddingX + 3 * cellWidth, paddingY, paddingX + 5 * cellWidth, paddingY + 2 * cellHeight, linePaint)
        canvas.drawLine(paddingX + 5 * cellWidth, paddingY, paddingX + 3 * cellWidth, paddingY + 2 * cellHeight, linePaint)
        // Bottom palace
        canvas.drawLine(paddingX + 3 * cellWidth, paddingY + 7 * cellHeight, paddingX + 5 * cellWidth, paddingY + 9 * cellHeight, linePaint)
        canvas.drawLine(paddingX + 5 * cellWidth, paddingY + 7 * cellHeight, paddingX + 3 * cellWidth, paddingY + 9 * cellHeight, linePaint)
    }

    private fun drawRiver(canvas: Canvas) {
        val riverTop = paddingY + 4 * cellHeight + cellHeight * 0.15f
        val riverBottom = paddingY + 5 * cellHeight - cellHeight * 0.15f

        textPaint.color = Color.parseColor("#8B4513")
        textPaint.textSize = cellHeight * 0.5f

        val centerX1 = paddingX + 2 * cellWidth
        val centerX2 = paddingX + 6 * cellWidth
        val centerY = (riverTop + riverBottom) / 2

        canvas.drawText("楚 河", centerX1, centerY + textPaint.textSize / 3, textPaint)
        canvas.drawText("漢 界", centerX2, centerY + textPaint.textSize / 3, textPaint)
    }

    private fun drawValidMoves(canvas: Canvas) {
        for (move in validMoves) {
            val x = paddingX + move.toCol * cellWidth
            val y = paddingY + move.toRow * cellHeight
            val target = engine?.board?.get(move.toRow)?.get(move.toCol) ?: 0

            if (target != XiangqiEngine.EMPTY) {
                // Capture indicator
                canvas.drawCircle(x, y, pieceRadius + 3f, validMovePaint)
            } else {
                // Move indicator
                canvas.drawCircle(x, y, pieceRadius * 0.3f, validMovePaint)
            }
        }
    }

    private fun drawLastMove(canvas: Canvas) {
        val move = lastMove ?: return

        // Draw from position (background + border)
        val fromX = paddingX + move.fromCol * cellWidth
        val fromY = paddingY + move.fromRow * cellHeight
        canvas.drawCircle(fromX, fromY, pieceRadius, lastMoveBgPaint)
        canvas.drawCircle(fromX, fromY, pieceRadius, lastMovePaint)

        // Draw to position (background + border)
        val toX = paddingX + move.toCol * cellWidth
        val toY = paddingY + move.toRow * cellHeight
        canvas.drawCircle(toX, toY, pieceRadius, lastMoveBgPaint)
        canvas.drawCircle(toX, toY, pieceRadius, lastMovePaint)
    }

    private fun drawPieces(canvas: Canvas) {
        val e = engine ?: return

        for (row in 0 until XiangqiEngine.ROWS) {
            for (col in 0 until XiangqiEngine.COLS) {
                val piece = e.board[row][col]
                if (piece != XiangqiEngine.EMPTY) {
                    drawPiece(canvas, row, col, piece, e)
                }
            }
        }
    }

    private fun drawPiece(canvas: Canvas, row: Int, col: Int, piece: Int, engine: XiangqiEngine) {
        val x = paddingX + col * cellWidth
        val y = paddingY + row * cellHeight

        // Draw piece background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5DEB3")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x, y, pieceRadius, bgPaint)

        // Draw piece border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = if (engine.isRedPiece(piece)) Color.parseColor("#C41E3A") else Color.parseColor("#1A1A1A")
        }
        canvas.drawCircle(x, y, pieceRadius, borderPaint)

        // Draw piece text
        val textColor = if (engine.isRedPiece(piece)) Color.parseColor("#C41E3A") else Color.parseColor("#1A1A1A")
        textPaint.color = textColor
        textPaint.textSize = pieceRadius * 1.2f

        val name = engine.getPieceName(piece)
        canvas.drawText(name, x, y + textPaint.textSize / 3, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true

        val touchX = event.x
        val touchY = event.y

        val col = ((touchX - paddingX) / cellWidth).roundToInt()
        val row = ((touchY - paddingY) / cellHeight).roundToInt()

        if (row !in 0 until XiangqiEngine.ROWS || col !in 0 until XiangqiEngine.COLS) return true

        val e = engine ?: return true

        if (selectedRow >= 0 && selectedCol >= 0) {
            // Try to move
            val targetPiece = e.board[row][col]
            if (e.isRedPiece(targetPiece) && e.isRedTurn) {
                // Select another red piece
                setSelectedPosition(row, col)
            } else if (e.isValidMove(selectedRow, selectedCol, row, col)) {
                onMoveListener?.invoke(selectedRow, selectedCol, row, col)
                clearSelection()
            } else {
                clearSelection()
            }
        } else {
            // Select a piece
            val piece = e.board[row][col]
            if (e.isRedPiece(piece) && e.isRedTurn) {
                setSelectedPosition(row, col)
            }
        }

        return true
    }
}
