package com.xiangqi.app

import kotlin.math.max
import kotlin.math.min

class AIEngine {

    companion object {
        private const val MAX_DEPTH = 4
        private const val INFINITY = 1000000

        // Position bonus tables for better AI play
        // Higher values = better positions

        // King position bonus (staying in center of palace is good)
        private val KING_POS = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 1, 2, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 1, 3, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 1, 3, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 1, 2, 1, 0, 0, 0)
        )

        // Advisor position bonus
        private val ADVISOR_POS = arrayOf(
            intArrayOf(0, 0, 0, 2, 0, 2, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 2, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 2, 0, 2, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 2, 0, 2, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 2, 0, 0, 0, 0)
        )

        // Bishop position bonus
        private val BISHOP_POS = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 2, 0, 0, 0, 2, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 3, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 3, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 2, 0, 0, 0, 2, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
        )

        // Knight position bonus
        private val KNIGHT_POS = arrayOf(
            intArrayOf(0, 0, 2, 0, 0, 0, 2, 0, 0),
            intArrayOf(0, 2, 4, 4, 0, 4, 4, 2, 0),
            intArrayOf(2, 4, 6, 6, 0, 6, 6, 4, 2),
            intArrayOf(0, 0, 4, 6, 6, 6, 4, 0, 0),
            intArrayOf(0, 0, 4, 6, 8, 6, 4, 0, 0),
            intArrayOf(0, 0, 4, 6, 8, 6, 4, 0, 0),
            intArrayOf(0, 0, 4, 6, 6, 6, 4, 0, 0),
            intArrayOf(2, 4, 6, 6, 0, 6, 6, 4, 2),
            intArrayOf(0, 2, 4, 4, 0, 4, 4, 2, 0),
            intArrayOf(0, 0, 2, 0, 0, 0, 2, 0, 0)
        )

        // Rook position bonus
        private val ROOK_POS = arrayOf(
            intArrayOf(6, 8, 6, 6, 4, 6, 6, 8, 6),
            intArrayOf(6, 10, 8, 6, 4, 6, 8, 10, 6),
            intArrayOf(4, 8, 6, 6, 4, 6, 6, 8, 4),
            intArrayOf(4, 6, 6, 6, 4, 6, 6, 6, 4),
            intArrayOf(4, 6, 6, 6, 4, 6, 6, 6, 4),
            intArrayOf(4, 6, 6, 6, 4, 6, 6, 6, 4),
            intArrayOf(4, 6, 6, 6, 4, 6, 6, 6, 4),
            intArrayOf(4, 8, 6, 6, 4, 6, 6, 8, 4),
            intArrayOf(6, 10, 8, 6, 4, 6, 8, 10, 6),
            intArrayOf(6, 8, 6, 6, 4, 6, 6, 8, 6)
        )

        // Cannon position bonus
        private val CANNON_POS = arrayOf(
            intArrayOf(0, 0, 2, 4, 4, 4, 2, 0, 0),
            intArrayOf(0, 2, 4, 4, 0, 4, 4, 2, 0),
            intArrayOf(2, 4, 4, 4, 0, 4, 4, 4, 2),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(2, 4, 4, 4, 0, 4, 4, 4, 2),
            intArrayOf(0, 2, 4, 4, 0, 4, 4, 2, 0),
            intArrayOf(0, 0, 2, 4, 4, 4, 2, 0, 0)
        )

        // Pawn position bonus
        private val PAWN_POS = arrayOf(
            intArrayOf(0, 0, 0, 8, 10, 8, 0, 0, 0),
            intArrayOf(0, 0, 0, 6, 8, 6, 0, 0, 0),
            intArrayOf(0, 0, 0, 4, 6, 4, 0, 0, 0),
            intArrayOf(0, 0, 0, 2, 4, 2, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 2, 4, 2, 0, 0, 0),
            intArrayOf(0, 0, 0, 4, 6, 4, 0, 0, 0),
            intArrayOf(0, 0, 0, 6, 8, 6, 0, 0, 0),
            intArrayOf(0, 0, 0, 8, 10, 8, 0, 0, 0)
        )
    }

    private var nodesEvaluated = 0

    fun getBestMove(engine: XiangqiEngine): Move? {
        nodesEvaluated = 0
        val moves = engine.getAllValidMoves(false) // Black (AI) moves

        if (moves.isEmpty()) return null

        var bestMove: Move? = null
        var bestScore = -INFINITY

        // Sort moves by initial evaluation (captures first)
        val sortedMoves = moves.sortedByDescending { move ->
            val captured = engine.getPieceAt(move.toRow, move.toCol)
            if (captured != XiangqiEngine.EMPTY) engine.getPieceValue(captured) else 0
        }

        for (move in sortedMoves) {
            val fr = move.fromRow
            val fc = move.fromCol
            val tr = move.toRow
            val tc = move.toCol
            // Make move
            val captured = engine.board[tr][tc]
            val piece = engine.board[fr][fc]
            engine.board[tr][tc] = piece
            engine.board[fr][fc] = XiangqiEngine.EMPTY

            val score = if (captured == XiangqiEngine.R_KING) {
                INFINITY  // Win!
            } else {
                -minimax(engine, MAX_DEPTH - 1, -INFINITY, INFINITY, true)
            }

            // Undo move
            engine.board[fr][fc] = piece
            engine.board[tr][tc] = captured

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }

        return bestMove
    }

    private fun minimax(engine: XiangqiEngine, depth: Int, alpha: Int, beta: Int, isMaximizing: Boolean): Int {
        nodesEvaluated++

        if (depth == 0) {
            return evaluateBoard(engine)
        }

        val moves = engine.getAllValidMoves(isMaximizing)

        if (moves.isEmpty()) {
            // No moves available = checkmate or stalemate
            return if (engine.isInCheck(isMaximizing)) {
                -INFINITY + (MAX_DEPTH - depth)  // Checkmate (worse if found earlier)
            } else {
                0  // Stalemate
            }
        }

        var a = alpha
        var b = beta
        var bestScore = if (isMaximizing) -INFINITY else INFINITY

        // Sort moves for better pruning
        val sortedMoves = moves.sortedByDescending { move ->
            val captured = engine.board[move.toRow][move.toCol]
            if (captured != XiangqiEngine.EMPTY) engine.getPieceValue(captured) else 0
        }

        for (move in sortedMoves) {
            val fr = move.fromRow
            val fc = move.fromCol
            val tr = move.toRow
            val tc = move.toCol
            val captured = engine.board[tr][tc]
            val piece = engine.board[fr][fc]
            engine.board[tr][tc] = piece
            engine.board[fr][fc] = XiangqiEngine.EMPTY

            val score = -minimax(engine, depth - 1, -b, -a, !isMaximizing)

            engine.board[fr][fc] = piece
            engine.board[tr][tc] = captured

            if (isMaximizing) {
                bestScore = max(bestScore, score)
                a = max(a, score)
            } else {
                bestScore = min(bestScore, score)
                b = min(b, score)
            }

            if (a >= b) break  // Alpha-beta pruning
        }

        return bestScore
    }

    private fun evaluateBoard(engine: XiangqiEngine): Int {
        var score = 0

        for (r in 0 until XiangqiEngine.ROWS) {
            for (c in 0 until XiangqiEngine.COLS) {
                val piece = engine.board[r][c]
                if (piece == XiangqiEngine.EMPTY) continue

                val value = engine.getPieceValue(piece)
                val posBonus = getPositionBonus(piece, r, c)

                if (engine.isBlackPiece(piece)) {
                    score += value + posBonus
                } else {
                    score -= value + posBonus
                }
            }
        }

        // Bonus for checking opponent
        if (engine.isInCheck(true)) score += 50
        if (engine.isInCheck(false)) score -= 50

        return score
    }

    private fun getPositionBonus(piece: Int, row: Int, col: Int): Int {
        val mirrorRow = if (row < 5) row else 9 - row

        return when (piece) {
            XiangqiEngine.R_KING, XiangqiEngine.B_KING -> KING_POS[row][col]
            XiangqiEngine.R_ADVISOR, XiangqiEngine.B_ADVISOR -> ADVISOR_POS[row][col]
            XiangqiEngine.R_BISHOP, XiangqiEngine.B_BISHOP -> BISHOP_POS[row][col]
            XiangqiEngine.R_KNIGHT, XiangqiEngine.B_KNIGHT -> KNIGHT_POS[row][col]
            XiangqiEngine.R_ROOK, XiangqiEngine.B_ROOK -> ROOK_POS[row][col]
            XiangqiEngine.R_CANNON, XiangqiEngine.B_CANNON -> CANNON_POS[row][col]
            XiangqiEngine.R_PAWN, XiangqiEngine.B_PAWN -> PAWN_POS[row][col]
            else -> 0
        }
    }
}
