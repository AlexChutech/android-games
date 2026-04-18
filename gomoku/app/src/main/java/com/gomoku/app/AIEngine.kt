package com.gomoku.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class AIEngine {

    companion object {
        private const val AI = GameEngine.WHITE
        private const val HUMAN = GameEngine.BLACK
        private const val EMPTY = GameEngine.EMPTY
        private const val SIZE = GameEngine.SIZE

        // Score thresholds
        private const val SCORE_FIVE = 10_000_000
        private const val SCORE_OPEN_FOUR = 1_000_000
        private const val SCORE_FOUR = 100_000
        private const val SCORE_OPEN_THREE = 10_000
        private const val SCORE_THREE = 1_000
        private const val SCORE_OPEN_TWO = 100
        private const val SCORE_TWO = 10

        private const val MAX_DEPTH = 8
        private const val SEARCH_TIME_MS = 5000L

        // Transposition table flags
        private const val TT_EXACT = 0
        private const val TT_LOWER = 1
        private const val TT_UPPER = 2
    }

    // Zobrist hashing for transposition table
    private val zobristTable = Array(SIZE) { Array(SIZE) { LongArray(3) } }

    init {
        val random = Random(42)
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                for (p in 0..2) {
                    zobristTable[r][c][p] = random.nextLong()
                }
            }
        }
    }

    data class TTEntry(
        val hash: Long,
        val depth: Int,
        val score: Int,
        val bestMove: Pair<Int, Int>?,
        val flag: Int
    )

    private val transpositionTable = mutableMapOf<Long, TTEntry>()
    private var killerMoves = Array(MAX_DEPTH + 1) { mutableListOf<Pair<Int, Int>>() }
    private var nodesEvaluated = 0
    private var ttHits = 0

    fun getBestMove(board: Array<IntArray>): Pair<Int, Int> {
        nodesEvaluated = 0
        ttHits = 0
        killerMoves = Array(MAX_DEPTH + 1) { mutableListOf() }
        transpositionTable.clear()

        val candidates = getCandidates(board)

        // Opening book
        if (candidates.isEmpty()) {
            return SIZE / 2 to SIZE / 2
        }

        val moveCount = countPieces(board)
        if (moveCount == 1) {
            // Second move: play near center
            val center = SIZE / 2
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val r = center + dr
                    val c = center + dc
                    if (r in 0 until SIZE && c in 0 until SIZE && board[r][c] == EMPTY) {
                        return r to c
                    }
                }
            }
        }

        // Check for immediate winning move
        for ((r, c) in candidates) {
            board[r][c] = AI
            if (GameEngine().checkWin(r, c, AI)) {
                board[r][c] = EMPTY
                return r to c
            }
            board[r][c] = EMPTY
        }

        // Check for must-block move
        for ((r, c) in candidates) {
            board[r][c] = HUMAN
            if (GameEngine().checkWin(r, c, HUMAN)) {
                board[r][c] = EMPTY
                return r to c
            }
            board[r][c] = EMPTY
        }

        // Check for double-threat moves (moves that create two threats)
        val doubleThreat = findDoubleThreat(board, candidates)
        if (doubleThreat != null) return doubleThreat

        var bestMove = candidates[0]
        var bestScore = Int.MIN_VALUE
        val startTime = System.currentTimeMillis()

        // Iterative deepening with transposition table
        for (depth in 1..MAX_DEPTH) {
            val hash = computeHash(board)
            val (move, score) = searchRoot(board, candidates, depth, startTime, hash)

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }

            if (System.currentTimeMillis() - startTime > SEARCH_TIME_MS) break
            if (score >= SCORE_FIVE) break
        }

        return bestMove
    }

    private fun searchRoot(
        board: Array<IntArray>,
        candidates: List<Pair<Int, Int>>,
        depth: Int,
        startTime: Long,
        hash: Long
    ): Pair<Pair<Int, Int>, Int> {
        var bestMove = candidates[0]
        var bestScore = Int.MIN_VALUE
        var alpha = Int.MIN_VALUE
        val beta = Int.MAX_VALUE

        // Get PV move from transposition table
        val ttEntry = transpositionTable[hash]
        val pvMove = ttEntry?.bestMove

        // Sort candidates with PV move first
        val sortedCandidates = sortCandidatesWithPV(board, candidates, pvMove)

        for (move in sortedCandidates) {
            if (System.currentTimeMillis() - startTime > SEARCH_TIME_MS) break

            val (r, c) = move
            val newHash = hash xor zobristTable[r][c][AI]
            board[r][c] = AI

            val score = if (GameEngine().checkWin(r, c, AI)) {
                SCORE_FIVE
            } else {
                -alphaBeta(board, depth - 1, -beta, -alpha, false, 1, startTime, newHash)
            }

            board[r][c] = EMPTY

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            alpha = max(alpha, score)
        }

        // Store in transposition table
        transpositionTable[hash] = TTEntry(hash, depth, bestScore, bestMove, TT_EXACT)

        return bestMove to bestScore
    }

    private fun alphaBeta(
        board: Array<IntArray>,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        currentDepth: Int,
        startTime: Long,
        hash: Long
    ): Int {
        if (System.currentTimeMillis() - startTime > SEARCH_TIME_MS) return 0
        nodesEvaluated++

        // Transposition table lookup
        val ttEntry = transpositionTable[hash]
        if (ttEntry != null && ttEntry.depth >= depth) {
            ttHits++
            when (ttEntry.flag) {
                TT_EXACT -> return ttEntry.score
                TT_LOWER -> if (ttEntry.score >= beta) return ttEntry.score
                TT_UPPER -> if (ttEntry.score <= alpha) return ttEntry.score
            }
        }

        val candidates = getQuickCandidates(board)
        if (candidates.isEmpty() || depth == 0) {
            return evaluateBoard(board)
        }

        var a = alpha
        var b = beta
        var bestMove: Pair<Int, Int>? = null
        val pvMove = ttEntry?.bestMove

        val sorted = sortCandidatesWithPV(board, candidates, pvMove)

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for ((r, c) in sorted) {
                if (board[r][c] != EMPTY) continue

                val newHash = hash xor zobristTable[r][c][AI]
                board[r][c] = AI

                val score = if (GameEngine().checkWin(r, c, AI)) {
                    board[r][c] = EMPTY
                    SCORE_FIVE + depth
                } else {
                    val s = alphaBeta(board, depth - 1, a, b, false, currentDepth + 1, startTime, newHash)
                    board[r][c] = EMPTY
                    s
                }

                if (score > maxEval) {
                    maxEval = score
                    bestMove = r to c
                }
                a = max(a, score)

                if (b <= a) {
                    addKillerMove(currentDepth, r to c)
                    break
                }
            }

            // Store in transposition table
            val flag = when {
                maxEval <= alpha -> TT_UPPER
                maxEval >= beta -> TT_LOWER
                else -> TT_EXACT
            }
            transpositionTable[hash] = TTEntry(hash, depth, maxEval, bestMove, flag)

            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for ((r, c) in sorted) {
                if (board[r][c] != EMPTY) continue

                val newHash = hash xor zobristTable[r][c][HUMAN]
                board[r][c] = HUMAN

                val score = if (GameEngine().checkWin(r, c, HUMAN)) {
                    board[r][c] = EMPTY
                    -SCORE_FIVE - depth
                } else {
                    val s = alphaBeta(board, depth - 1, a, b, true, currentDepth + 1, startTime, newHash)
                    board[r][c] = EMPTY
                    s
                }

                if (score < minEval) {
                    minEval = score
                    bestMove = r to c
                }
                b = min(b, score)

                if (b <= a) {
                    addKillerMove(currentDepth, r to c)
                    break
                }
            }

            val flag = when {
                minEval <= alpha -> TT_UPPER
                minEval >= beta -> TT_LOWER
                else -> TT_EXACT
            }
            transpositionTable[hash] = TTEntry(hash, depth, minEval, bestMove, flag)

            return minEval
        }
    }

    private fun findDoubleThreat(board: Array<IntArray>, candidates: List<Pair<Int, Int>>): Pair<Int, Int>? {
        for ((r, c) in candidates) {
            board[r][c] = AI
            val score = evaluatePosition(board, r, c, AI)
            board[r][c] = EMPTY

            // If this move creates multiple open threes or a four, it's likely a double threat
            if (score >= SCORE_OPEN_THREE * 2 || score >= SCORE_FOUR) {
                return r to c
            }
        }
        return null
    }

    private fun sortCandidatesWithPV(
        board: Array<IntArray>,
        candidates: List<Pair<Int, Int>>,
        pvMove: Pair<Int, Int>?
    ): List<Pair<Int, Int>> {
        return candidates.map { move ->
            val (r, c) = move
            var score = 0

            // PV move gets highest priority
            if (move == pvMove) score += 1000000

            // Evaluate position
            board[r][c] = AI
            score += evaluatePosition(board, r, c, AI)
            board[r][c] = EMPTY

            board[r][c] = HUMAN
            score += evaluatePosition(board, r, c, HUMAN)
            board[r][c] = EMPTY

            // Killer move bonus
            if (killerMoves.any { it.contains(move) }) score += 5000

            move to score
        }.sortedByDescending { it.second }.map { it.first }.take(15)
    }

    private fun addKillerMove(depth: Int, move: Pair<Int, Int>) {
        if (depth >= killerMoves.size) return
        val list = killerMoves[depth]
        if (!list.contains(move)) {
            list.add(0, move)
            if (list.size > 3) list.removeAt(list.lastIndex)
        }
    }

    private fun computeHash(board: Array<IntArray>): Long {
        var hash = 0L
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] != EMPTY) {
                    hash = hash xor zobristTable[r][c][board[r][c]]
                }
            }
        }
        return hash
    }

    private fun countPieces(board: Array<IntArray>): Int {
        var count = 0
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] != EMPTY) count++
            }
        }
        return count
    }

    private fun getCandidates(board: Array<IntArray>): List<Pair<Int, Int>> {
        val candidateSet = mutableSetOf<Pair<Int, Int>>()
        var hasPieces = false

        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] != EMPTY) {
                    hasPieces = true
                    for (dr in -2..2) {
                        for (dc in -2..2) {
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0 until SIZE && nc in 0 until SIZE && board[nr][nc] == EMPTY) {
                                candidateSet.add(nr to nc)
                            }
                        }
                    }
                }
            }
        }

        return if (!hasPieces) emptyList() else candidateSet.toList()
    }

    private fun getQuickCandidates(board: Array<IntArray>): List<Pair<Int, Int>> {
        val candidateSet = mutableSetOf<Pair<Int, Int>>()

        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] != EMPTY) {
                    for (dr in -2..2) {
                        for (dc in -2..2) {
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0 until SIZE && nc in 0 until SIZE && board[nr][nc] == EMPTY) {
                                candidateSet.add(nr to nc)
                            }
                        }
                    }
                }
            }
        }

        return candidateSet.toList()
    }

    fun evaluatePosition(board: Array<IntArray>, row: Int, col: Int, player: Int): Int {
        var score = 0
        val directions = arrayOf(0 to 1, 1 to 0, 1 to 1, 1 to -1)

        for ((dr, dc) in directions) {
            val (count, openEnds) = analyzeLine(board, row, col, dr, dc, player)
            score += when {
                count >= 5 -> SCORE_FIVE
                count == 4 && openEnds == 2 -> SCORE_OPEN_FOUR
                count == 4 && openEnds == 1 -> SCORE_FOUR
                count == 3 && openEnds == 2 -> SCORE_OPEN_THREE
                count == 3 && openEnds == 1 -> SCORE_THREE
                count == 2 && openEnds == 2 -> SCORE_OPEN_TWO
                count == 2 && openEnds == 1 -> SCORE_TWO
                else -> 0
            }
        }
        return score
    }

    private fun analyzeLine(
        board: Array<IntArray>,
        row: Int,
        col: Int,
        dr: Int,
        dc: Int,
        player: Int
    ): Pair<Int, Int> {
        var count = 1
        var openEnds = 0

        var r = row + dr
        var c = col + dc
        while (r in 0 until SIZE && c in 0 until SIZE && board[r][c] == player) {
            count++
            r += dr
            c += dc
        }
        if (r in 0 until SIZE && c in 0 until SIZE && board[r][c] == EMPTY) {
            openEnds++
        }

        r = row - dr
        c = col - dc
        while (r in 0 until SIZE && c in 0 until SIZE && board[r][c] == player) {
            count++
            r -= dr
            c -= dc
        }
        if (r in 0 until SIZE && c in 0 until SIZE && board[r][c] == EMPTY) {
            openEnds++
        }

        return count to openEnds
    }

    private fun evaluateBoard(board: Array<IntArray>): Int {
        var score = 0

        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] == AI) {
                    score += evaluatePosition(board, r, c, AI)
                } else if (board[r][c] == HUMAN) {
                    score -= (evaluatePosition(board, r, c, HUMAN) * 12 / 10)
                }
            }
        }

        val center = SIZE / 2
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] == AI) {
                    score += (6 - abs(r - center) - abs(c - center))
                } else if (board[r][c] == HUMAN) {
                    score -= (6 - abs(r - center) - abs(c - center))
                }
            }
        }

        return score
    }
}
