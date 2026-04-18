package com.xiangqi.app

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class AIEngine {

    companion object {
        private const val MAX_DEPTH = 8
        private const val INFINITY = 10000000
        private const val NULL_MOVE_REDUCTION = 2

        // TT flags
        private const val TT_EXACT = 0
        private const val TT_LOWER = 1
        private const val TT_UPPER = 2

        private val PIECE_VALUES = mapOf(
            XiangqiEngine.R_KING to 10000, XiangqiEngine.B_KING to 10000,
            XiangqiEngine.R_ADVISOR to 20, XiangqiEngine.B_ADVISOR to 20,
            XiangqiEngine.R_BISHOP to 20, XiangqiEngine.B_BISHOP to 20,
            XiangqiEngine.R_KNIGHT to 45, XiangqiEngine.B_KNIGHT to 45,
            XiangqiEngine.R_ROOK to 100, XiangqiEngine.B_ROOK to 100,
            XiangqiEngine.R_CANNON to 50, XiangqiEngine.B_CANNON to 50,
            XiangqiEngine.R_PAWN to 10, XiangqiEngine.B_PAWN to 10
        )

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

        // Opening book: common good opening moves for Black
        private val OPENING_BOOK = mapOf(
            // First move options
            "start" to listOf(
                Move(0, 1, 2, 2),  // 炮二平五 (Cannon to center)
                Move(0, 7, 2, 6),  // 炮八平五
                Move(0, 1, 2, 0),  // 马二进三
                Move(0, 7, 2, 8)   // 马八进九
            )
        )
    }

    // Zobrist hashing
    private val zobristTable = Array(XiangqiEngine.ROWS) {
        Array(XiangqiEngine.COLS) { LongArray(15) }
    }

    init {
        val random = Random(12345)
        for (r in 0 until XiangqiEngine.ROWS) {
            for (c in 0 until XiangqiEngine.COLS) {
                for (p in 0..14) {
                    zobristTable[r][c][p] = random.nextLong()
                }
            }
        }
    }

    data class TTEntry(
        val hash: Long,
        val depth: Int,
        val score: Int,
        val bestMove: Move?,
        val flag: Int
    )

    private val transpositionTable = mutableMapOf<Long, TTEntry>()
    private var nodesEvaluated = 0
    private var ttHits = 0
    private val killerMoves = Array(MAX_DEPTH + 1) { mutableMapOf<Int, Int>() }
    private val historyTable = mutableMapOf<Int, Int>()
    private var bestMoveAtRoot: Move? = null

    fun getBestMove(engine: XiangqiEngine): Move? {
        nodesEvaluated = 0
        ttHits = 0
        killerMoves.forEach { it.clear() }
        historyTable.clear()
        bestMoveAtRoot = null
        transpositionTable.clear()

        val moves = engine.getAllValidMoves(false)
        if (moves.isEmpty()) return null

        // Opening book
        val pieceCount = countPieces(engine)
        if (pieceCount == 32) {  // First move
            val openingMoves = OPENING_BOOK["start"]
            if (openingMoves != null && openingMoves.isNotEmpty()) {
                return openingMoves.random()
            }
        }

        // Iterative deepening
        var bestMove = moves[0]
        val hash = computeHash(engine)

        for (depth in 1..MAX_DEPTH) {
            val result = searchRoot(engine, moves, depth, hash)
            if (result != null) {
                bestMove = result
                bestMoveAtRoot = result
            }
        }

        return bestMove
    }

    private fun searchRoot(engine: XiangqiEngine, moves: List<Move>, depth: Int, hash: Long): Move? {
        var bestMove: Move? = bestMoveAtRoot
        var bestScore = -INFINITY
        var alpha = -INFINITY
        val beta = INFINITY

        val ttEntry = transpositionTable[hash]
        val pvMove = ttEntry?.bestMove

        val sortedMoves = sortMovesWithPV(engine, moves, depth, pvMove)

        for (move in sortedMoves) {
            val captured = engine.board[move.toRow][move.toCol]
            val piece = engine.board[move.fromRow][move.fromCol]
            val newHash = updateHash(hash, move, piece, captured)

            engine.board[move.toRow][move.toCol] = piece
            engine.board[move.fromRow][move.fromCol] = XiangqiEngine.EMPTY

            val score = if (captured == XiangqiEngine.R_KING) {
                INFINITY
            } else {
                -alphaBeta(engine, depth - 1, -beta, -alpha, false, newHash, false)
            }

            engine.board[move.fromRow][move.fromCol] = piece
            engine.board[move.toRow][move.toCol] = captured

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            alpha = max(alpha, score)
        }

        if (bestMove != null) {
            val moveKey = getMoveKey(bestMove)
            historyTable[moveKey] = (historyTable[moveKey] ?: 0) + depth * depth
        }

        transpositionTable[hash] = TTEntry(hash, depth, bestScore, bestMove, TT_EXACT)

        return bestMove
    }

    private fun alphaBeta(
        engine: XiangqiEngine,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        hash: Long,
        nullMoveUsed: Boolean
    ): Int {
        nodesEvaluated++

        // TT lookup
        val ttEntry = transpositionTable[hash]
        if (ttEntry != null && ttEntry.depth >= depth) {
            ttHits++
            when (ttEntry.flag) {
                TT_EXACT -> return ttEntry.score
                TT_LOWER -> if (ttEntry.score >= beta) return ttEntry.score
                TT_UPPER -> if (ttEntry.score <= alpha) return ttEntry.score
            }
        }

        if (depth <= 0) {
            return quiescenceSearch(engine, alpha, beta, isMaximizing, 4)
        }

        // Null move pruning
        if (!nullMoveUsed && depth >= 3 && !engine.isInCheck(isMaximizing)) {
            val material = countMaterial(engine)
            if (material > 1000) {  // Not in endgame
                val nullScore = -alphaBeta(engine, depth - 1 - NULL_MOVE_REDUCTION, -beta, -beta + 1, !isMaximizing, hash, true)
                if (nullScore >= beta) {
                    return beta
                }
            }
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
        var bestMove: Move? = null
        val pvMove = ttEntry?.bestMove

        val sortedMoves = sortMovesWithPV(engine, moves, depth, pvMove)

        for (move in sortedMoves) {
            val captured = engine.board[move.toRow][move.toCol]
            val piece = engine.board[move.fromRow][move.fromCol]
            val newHash = updateHash(hash, move, piece, captured)

            engine.board[move.toRow][move.toCol] = piece
            engine.board[move.fromRow][move.fromCol] = XiangqiEngine.EMPTY

            val score = -alphaBeta(engine, depth - 1, -beta, -a, !isMaximizing, newHash, false)

            engine.board[move.fromRow][move.fromCol] = piece
            engine.board[move.toRow][move.toCol] = captured

            if (score > a) {
                a = score
                bestMove = move
            }

            if (score >= beta) {
                val depthIdx = minOf(depth, MAX_DEPTH)
                val moveKey = getMoveKey(move)
                killerMoves[depthIdx][moveKey] = (killerMoves[depthIdx][moveKey] ?: 0) + depth

                val flag = TT_LOWER
                transpositionTable[hash] = TTEntry(hash, depth, score, bestMove, flag)
                return score
            }
        }

        val flag = if (a > alpha) TT_EXACT else TT_UPPER
        transpositionTable[hash] = TTEntry(hash, depth, a, bestMove, flag)

        return a
    }

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

    private fun sortMovesWithPV(engine: XiangqiEngine, moves: List<Move>, depth: Int, pvMove: Move?): List<Move> {
        return moves.map { move ->
            var score = 0

            if (move == pvMove) score += 1000000

            val captured = engine.board[move.toRow][move.toCol]
            if (captured != XiangqiEngine.EMPTY) {
                val attacker = engine.board[move.fromRow][move.fromCol]
                score += (engine.getPieceValue(captured) * 10 - engine.getPieceValue(attacker)) * 100
            }

            val moveKey = getMoveKey(move)
            score += (killerMoves[minOf(depth, MAX_DEPTH)][moveKey] ?: 0) * 50
            score += (historyTable[moveKey] ?: 0)

            val piece = engine.board[move.fromRow][move.fromCol]
            score += getPositionBonus(piece, move.toRow, move.toCol)

            move to score
        }.sortedByDescending { it.second }.map { it.first }.take(20)
    }

    private fun computeHash(engine: XiangqiEngine): Long {
        var hash = 0L
        for (r in 0 until XiangqiEngine.ROWS) {
            for (c in 0 until XiangqiEngine.COLS) {
                val piece = engine.board[r][c]
                if (piece != XiangqiEngine.EMPTY) {
                    hash = hash xor zobristTable[r][c][piece]
                }
            }
        }
        return hash
    }

    private fun updateHash(hash: Long, move: Move, piece: Int, captured: Int): Long {
        var newHash = hash
        newHash = newHash xor zobristTable[move.fromRow][move.fromCol][piece]
        newHash = newHash xor zobristTable[move.toRow][move.toCol][piece]
        if (captured != XiangqiEngine.EMPTY) {
            newHash = newHash xor zobristTable[move.toRow][move.toCol][captured]
        }
        return newHash
    }

    private fun getMoveKey(move: Move): Int {
        return move.fromRow * 1000 + move.fromCol * 100 + move.toRow * 10 + move.toCol
    }

    private fun countPieces(engine: XiangqiEngine): Int {
        var count = 0
        for (r in 0 until XiangqiEngine.ROWS) {
            for (c in 0 until XiangqiEngine.COLS) {
                if (engine.board[r][c] != XiangqiEngine.EMPTY) count++
            }
        }
        return count
    }

    private fun countMaterial(engine: XiangqiEngine): Int {
        var material = 0
        for (r in 0 until XiangqiEngine.ROWS) {
            for (c in 0 until XiangqiEngine.COLS) {
                val piece = engine.board[r][c]
                if (piece != XiangqiEngine.EMPTY) {
                    material += PIECE_VALUES[piece] ?: 0
                }
            }
        }
        return material
    }

    private fun evaluateBoard(engine: XiangqiEngine, forBlack: Boolean): Int {
        var score = 0

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

        val blackMoves = countMoves(engine, false)
        val redMoves = countMoves(engine, true)
        score += (blackMoves - redMoves) * 2

        if (engine.isInCheck(true)) score += 80
        if (engine.isInCheck(false)) score -= 80

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

            if (blackRook && piecesOnFile <= 3) score += 10
            if (redRook && piecesOnFile <= 3) score -= 10
        }
        return score
    }

    private fun getPositionBonus(piece: Int, row: Int, col: Int): Int {
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

    private fun XiangqiEngine.getPieceValue(piece: Int): Int = PIECE_VALUES[piece] ?: 0
}
