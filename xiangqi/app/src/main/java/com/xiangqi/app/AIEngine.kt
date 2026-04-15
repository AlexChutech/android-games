package com.xiangqi.app

import kotlin.math.max
import kotlin.math.min

class AIEngine {

    companion object {
        private const val MAX_DEPTH = 6
        private const val INFINITY = 10000000

        // More accurate piece values
        private val PIECE_VALUES = mapOf(
            XiangqiEngine.R_KING to 10000, XiangqiEngine.B_KING to 10000,
            XiangqiEngine.R_ADVISOR to 20, XiangqiEngine.B_ADVISOR to 20,
            XiangqiEngine.R_BISHOP to 20, XiangqiEngine.B_BISHOP to 20,
            XiangqiEngine.R_KNIGHT to 45, XiangqiEngine.B_KNIGHT to 45,
            XiangqiEngine.R_ROOK to 100, XiangqiEngine.B_ROOK to 100,
            XiangqiEngine.R_CANNON to 50, XiangqiEngine.B_CANNON to 50,
            XiangqiEngine.R_PAWN to 10, XiangqiEngine.B_PAWN to 10
        )

        // Advanced position tables
        private val KING_POS = arrayOf(
            intArrayOf(0, 0, 0, -10, 0, -10, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 5, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 10, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 10, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 5, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, -10, 0, -10, 0, 0, 0)
        )

        private val ADVISOR_POS = arrayOf(
            intArrayOf(0, 0, 0, 5, 0, 5, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 8, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 5, 0, 5, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 5, 0, 5, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 8, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 5, 0, 5, 0, 0, 0)
        )

        private val BISHOP_POS = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 5, 0, 0, 0, 5, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 8, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 8, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 5, 0, 0, 0, 5, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
        )

        // Knight loves center and enemy territory
        private val KNIGHT_POS = arrayOf(
            intArrayOf(0, -2, 4, 4, 2, 4, 4, -2, 0),
            intArrayOf(0, 4, 8, 8, 4, 8, 8, 4, 0),
            intArrayOf(4, 8, 12, 12, 8, 12, 12, 8, 4),
            intArrayOf(4, 8, 12, 16, 12, 16, 12, 8, 4),
            intArrayOf(6, 10, 16, 18, 16, 18, 16, 10, 6),
            intArrayOf(6, 10, 16, 18, 16, 18, 16, 10, 6),
            intArrayOf(4, 8, 12, 16, 12, 16, 12, 8, 4),
            intArrayOf(4, 8, 12, 12, 8, 12, 12, 8, 4),
            intArrayOf(0, 4, 8, 8, 4, 8, 8, 4, 0),
            intArrayOf(0, -2, 4, 4, 2, 4, 4, -2, 0)
        )

        // Rook loves open files and 7th rank
        private val ROOK_POS = arrayOf(
            intArrayOf(6, 8, 8, 10, 12, 10, 8, 8, 6),
            intArrayOf(8, 12, 12, 14, 16, 14, 12, 12, 8),
            intArrayOf(6, 10, 10, 12, 14, 12, 10, 10, 6),
            intArrayOf(4, 8, 8, 10, 12, 10, 8, 8, 4),
            intArrayOf(4, 8, 8, 10, 12, 10, 8, 8, 4),
            intArrayOf(4, 8, 8, 10, 12, 10, 8, 8, 4),
            intArrayOf(6, 10, 10, 12, 14, 12, 10, 10, 6),
            intArrayOf(8, 12, 12, 14, 16, 14, 12, 12, 8),
            intArrayOf(6, 8, 8, 10, 12, 10, 8, 8, 6),
            intArrayOf(6, 8, 8, 10, 12, 10, 8, 8, 6)
        )

        // Cannon: likes to stay back for attacks
        private val CANNON_POS = arrayOf(
            intArrayOf(0, 0, 2, 4, 4, 4, 2, 0, 0),
            intArrayOf(0, 2, 4, 6, 6, 6, 4, 2, 0),
            intArrayOf(2, 4, 6, 8, 8, 8, 6, 4, 2),
            intArrayOf(0, 0, 2, 4, 4, 4, 2, 0, 0),
            intArrayOf(0, 0, 0, 2, 2, 2, 0, 0, 0),
            intArrayOf(0, 0, 0, 2, 2, 2, 0, 0, 0),
            intArrayOf(0, 0, 2, 4, 4, 4, 2, 0, 0),
            intArrayOf(2, 4, 6, 8, 8, 8, 6, 4, 2),
            intArrayOf(0, 2, 4, 6, 6, 6, 4, 2, 0),
            intArrayOf(0, 0, 2, 4, 4, 4, 2, 0, 0)
        )

        // Pawn: more valuable after crossing river
        private val PAWN_POS = arrayOf(
            intArrayOf(0, 0, 0, 12, 16, 12, 0, 0, 0),
            intArrayOf(0, 0, 0, 10, 14, 10, 0, 0, 0),
            intArrayOf(0, 0, 0, 8, 12, 8, 0, 0, 0),
            intArrayOf(0, 0, 0, 4, 8, 4, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 4, 8, 4, 0, 0, 0),
            intArrayOf(0, 0, 0, 8, 12, 8, 0, 0, 0),
            intArrayOf(0, 0, 0, 10, 14, 10, 0, 0, 0),
            intArrayOf(0, 0, 0, 12, 16, 12, 0, 0, 0)
        )
    }

    private var nodesEvaluated = 0
    private val killerMoves = Array(MAX_DEPTH + 1) { mutableMapOf<Int, Int>() }
    private val historyTable = mutableMapOf<Int, Int>()
    private var bestMoveAtRoot: Move? = null

    fun getBestMove(engine: XiangqiEngine): Move? {
        nodesEvaluated = 0
        killerMoves.forEach { it.clear() }
        historyTable.clear()
        bestMoveAtRoot = null

        val moves = engine.getAllValidMoves(false)
        if (moves.isEmpty()) return null

        // Iterative deepening
        var bestMove = moves[0]

        for (depth in 1..MAX_DEPTH) {
            val result = searchRoot(engine, moves, depth)
            if (result != null) {
                bestMove = result
                bestMoveAtRoot = result
            }
        }

        return bestMove
    }

    private fun searchRoot(engine: XiangqiEngine, moves: List<Move>, depth: Int): Move? {
        var bestMove: Move? = bestMoveAtRoot
        var bestScore = -INFINITY
        var alpha = -INFINITY
        val beta = INFINITY

        // Sort moves with multiple heuristics
        val sortedMoves = sortMoves(engine, moves, depth, true)

        for (move in sortedMoves) {
            val captured = engine.board[move.toRow][move.toCol]
            val piece = engine.board[move.fromRow][move.fromCol]

            engine.board[move.toRow][move.toCol] = piece
            engine.board[move.fromRow][move.fromCol] = XiangqiEngine.EMPTY

            val score = if (captured == XiangqiEngine.R_KING) {
                INFINITY
            } else {
                -alphaBeta(engine, depth - 1, -beta, -alpha, false)
            }

            engine.board[move.fromRow][move.fromCol] = piece
            engine.board[move.toRow][move.toCol] = captured

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            alpha = max(alpha, score)
        }

        // Update history table
        if (bestMove != null && bestScore > -INFINITY / 2) {
            val moveKey = getMoveKey(bestMove)
            historyTable[moveKey] = (historyTable[moveKey] ?: 0) + depth * depth
        }

        return bestMove
    }

    private fun alphaBeta(engine: XiangqiEngine, depth: Int, alpha: Int, beta: Int, isMaximizing: Boolean): Int {
        nodesEvaluated++

        if (depth <= 0) {
            return quiescenceSearch(engine, alpha, beta, isMaximizing, 4)
        }

        val moves = engine.getAllValidMoves(isMaximizing)

        if (moves.isEmpty()) {
            return if (engine.isInCheck(isMaximizing)) {
                -INFINITY + (MAX_DEPTH - depth)
            } else {
                0
            }
        }

        var a = alpha
        val sortedMoves = sortMoves(engine, moves, depth, isMaximizing)

        for (move in sortedMoves) {
            val captured = engine.board[move.toRow][move.toCol]
            val piece = engine.board[move.fromRow][move.fromCol]

            engine.board[move.toRow][move.toCol] = piece
            engine.board[move.fromRow][move.fromCol] = XiangqiEngine.EMPTY

            val score = -alphaBeta(engine, depth - 1, -beta, -a, !isMaximizing)

            engine.board[move.fromRow][move.fromCol] = piece
            engine.board[move.toRow][move.toCol] = captured

            if (score >= beta) {
                // Killer move
                val depthIdx = minOf(depth, MAX_DEPTH)
                val moveKey = getMoveKey(move)
                killerMoves[depthIdx][moveKey] = (killerMoves[depthIdx][moveKey] ?: 0) + depth
                return score
            }

            if (score > a) {
                a = score
            }
        }

        return a
    }

    // Quiescence search to avoid horizon effect
    private fun quiescenceSearch(engine: XiangqiEngine, alpha: Int, beta: Int, isMaximizing: Boolean, depth: Int): Int {
        val standPat = evaluateBoard(engine, isMaximizing)

        if (depth <= 0) return standPat

        if (isMaximizing) {
            if (standPat >= beta) return beta
            if (standPat > alpha) return standPat
        } else {
            if (standPat <= alpha) return alpha
            if (standPat < beta) return standPat
        }

        // Only search captures
        val moves = engine.getAllValidMoves(isMaximizing).filter { move ->
            engine.board[move.toRow][move.toCol] != XiangqiEngine.EMPTY
        }

        if (moves.isEmpty()) return standPat

        var a = alpha
        var b = beta

        val sortedMoves = moves.sortedByDescending { move ->
            engine.getPieceValue(engine.board[move.toRow][move.toCol])
        }

        for (move in sortedMoves) {
            val captured = engine.board[move.toRow][move.toCol]
            val piece = engine.board[move.fromRow][move.fromCol]

            engine.board[move.toRow][move.toCol] = piece
            engine.board[move.fromRow][move.fromCol] = XiangqiEngine.EMPTY

            val score = -quiescenceSearch(engine, -b, -a, !isMaximizing, depth - 1)

            engine.board[move.fromRow][move.fromCol] = piece
            engine.board[move.toRow][move.toCol] = captured

            if (isMaximizing) {
                if (score >= beta) return beta
                if (score > a) a = score
            } else {
                if (score <= alpha) return alpha
                if (score < b) b = score
            }
        }

        return if (isMaximizing) a else b
    }

    private fun sortMoves(engine: XiangqiEngine, moves: List<Move>, depth: Int, isMaximizing: Boolean): List<Move> {
        return moves.map { move ->
            var score = 0

            // MVV-LVA (Most Valuable Victim - Least Valuable Attacker)
            val captured = engine.board[move.toRow][move.toCol]
            if (captured != XiangqiEngine.EMPTY) {
                val attacker = engine.board[move.fromRow][move.fromCol]
                score += (engine.getPieceValue(captured) * 10 - engine.getPieceValue(attacker)) * 100
            }

            // Killer move bonus
            val moveKey = getMoveKey(move)
            score += (killerMoves[minOf(depth, MAX_DEPTH)][moveKey] ?: 0) * 50

            // History heuristic
            score += (historyTable[moveKey] ?: 0)

            // Best move from previous iteration
            if (move == bestMoveAtRoot) score += 10000

            // Position bonus for moving to good squares
            val piece = engine.board[move.fromRow][move.fromCol]
            score += getPositionBonus(piece, move.toRow, move.toCol)

            move to score
        }.sortedByDescending { it.second }.map { it.first }
    }

    private fun getMoveKey(move: Move): Int {
        return move.fromRow * 1000 + move.fromCol * 100 + move.toRow * 10 + move.toCol
    }

    private fun evaluateBoard(engine: XiangqiEngine, forBlack: Boolean): Int {
        var score = 0

        // Material and position
        for (r in 0 until XiangqiEngine.ROWS) {
            for (c in 0 until XiangqiEngine.COLS) {
                val piece = engine.board[r][c]
                if (piece == XiangqiEngine.EMPTY) continue

                val value = PIECE_VALUES[piece] ?: 0
                val posBonus = getPositionBonus(piece, r, c)

                if (engine.isBlackPiece(piece)) {
                    score += value + posBonus
                } else {
                    score -= value + posBonus
                }
            }
        }

        // Mobility bonus
        val blackMoves = countMoves(engine, false)
        val redMoves = countMoves(engine, true)
        score += (blackMoves - redMoves) * 2

        // King safety
        if (engine.isInCheck(true)) score += 80  // Red in check
        if (engine.isInCheck(false)) score -= 80 // Black in check

        // Rook on open file bonus
        score += evaluateRookFiles(engine)

        return if (forBlack) score else -score
    }

    private fun countMoves(engine: XiangqiEngine, forRed: Boolean): Int {
        var count = 0
        for (r in 0 until XiangqiEngine.ROWS) {
            for (c in 0 until XiangqiEngine.COLS) {
                val piece = engine.board[r][c]
                if (piece == XiangqiEngine.EMPTY) continue
                if (forRed && engine.isRedPiece(piece)) {
                    count += countPieceMoves(engine, r, c)
                } else if (!forRed && engine.isBlackPiece(piece)) {
                    count += countPieceMoves(engine, r, c)
                }
            }
        }
        return count
    }

    private fun countPieceMoves(engine: XiangqiEngine, row: Int, col: Int): Int {
        var count = 0
        for (tr in 0 until XiangqiEngine.ROWS) {
            for (tc in 0 until XiangqiEngine.COLS) {
                if (engine.isValidMove(row, col, tr, tc)) {
                    count++
                }
            }
        }
        return count
    }

    private fun evaluateRookFiles(engine: XiangqiEngine): Int {
        var score = 0
        for (c in 0 until XiangqiEngine.COLS) {
            var blackRook = false
            var redRook = false
            var piecesOnFile = 0

            for (r in 0 until XiangqiEngine.ROWS) {
                val piece = engine.board[r][c]
                if (piece == XiangqiEngine.B_ROOK) blackRook = true
                if (piece == XiangqiEngine.R_ROOK) redRook = true
                if (piece != XiangqiEngine.EMPTY) piecesOnFile++
            }

            // Open file bonus
            if (blackRook && piecesOnFile <= 3) score += 10
            if (redRook && piecesOnFile <= 3) score -= 10
        }
        return score
    }

    private fun getPositionBonus(piece: Int, row: Int, col: Int): Int {
        return when (piece) {
            XiangqiEngine.R_KING -> KING_POS[row][col]
            XiangqiEngine.B_KING -> KING_POS[row][col]
            XiangqiEngine.R_ADVISOR -> ADVISOR_POS[row][col]
            XiangqiEngine.B_ADVISOR -> ADVISOR_POS[row][col]
            XiangqiEngine.R_BISHOP -> BISHOP_POS[row][col]
            XiangqiEngine.B_BISHOP -> BISHOP_POS[row][col]
            XiangqiEngine.R_KNIGHT -> KNIGHT_POS[row][col]
            XiangqiEngine.B_KNIGHT -> KNIGHT_POS[row][col]
            XiangqiEngine.R_ROOK -> ROOK_POS[row][col]
            XiangqiEngine.B_ROOK -> ROOK_POS[row][col]
            XiangqiEngine.R_CANNON -> CANNON_POS[row][col]
            XiangqiEngine.B_CANNON -> CANNON_POS[row][col]
            XiangqiEngine.R_PAWN -> PAWN_POS[row][col]
            XiangqiEngine.B_PAWN -> PAWN_POS[row][col]
            else -> 0
        }
    }

    private fun XiangqiEngine.getPieceValue(piece: Int): Int = PIECE_VALUES[piece] ?: 0
}
