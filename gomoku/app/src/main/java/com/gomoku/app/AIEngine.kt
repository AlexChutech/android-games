package com.gomoku.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AIEngine {

    companion object {
        private const val AI = GameEngine.WHITE
        private const val HUMAN = GameEngine.BLACK
        private const val EMPTY = GameEngine.EMPTY
        private const val SIZE = GameEngine.SIZE

        // Score thresholds for pattern evaluation
        private const val SCORE_FIVE = 1_000_000       // Win
        private const val SCORE_OPEN_FOUR = 100_000     // Unstoppable
        private const val SCORE_FOUR = 10_000           // Must block
        private const val SCORE_OPEN_THREE = 1_000      // Dangerous
        private const val SCORE_THREE = 100             // Threat
        private const val SCORE_OPEN_TWO = 100          // Building
        private const val SCORE_TWO = 10                // Early stage

        private const val MAX_DEPTH = 4
        private const val SEARCH_TIME_MS = 3000L
    }

    private var killerMoves = Array(MAX_DEPTH + 1) { mutableListOf<Pair<Int, Int>>() }
    private var nodesEvaluated = 0

    fun getBestMove(board: Array<IntArray>): Pair<Int, Int> {
        nodesEvaluated = 0
        killerMoves = Array(MAX_DEPTH + 1) { mutableListOf() }

        val candidates = getCandidates(board)
        if (candidates.isEmpty()) {
            // First move: play center
            return SIZE / 2 to SIZE / 2
        }
        if (candidates.size == 1) {
            // If only one candidate and center is empty, also play center for better positioning
            val center = SIZE / 2 to SIZE / 2
            if (board[center.first][center.second] == EMPTY) {
                return center
            }
            return candidates[0]
        }

        var bestMove = candidates[0]
        var bestScore = Int.MIN_VALUE

        val startTime = System.currentTimeMillis()

        // Iterative deepening
        for (depth in 1..MAX_DEPTH) {
            val (move, score) = iterativeSearch(board, candidates, depth, startTime)
            if (score > bestScore || (score == bestScore && depth > 1)) {
                bestScore = score
                bestMove = move
            }
            if (System.currentTimeMillis() - startTime > SEARCH_TIME_MS) break
            // If we found a winning move, no need to search deeper
            if (score >= SCORE_FIVE) break
        }

        return bestMove
    }

    private fun iterativeSearch(
        board: Array<IntArray>,
        candidates: List<Pair<Int, Int>>,
        depth: Int,
        startTime: Long
    ): Pair<Pair<Int, Int>, Int> {

        var bestMove = candidates[0]
        var bestScore = Int.MIN_VALUE
        var alpha = Int.MIN_VALUE
        var beta = Int.MAX_VALUE

        // Sort candidates by initial evaluation for better pruning
        val scoredCandidates = candidates.map { move ->
            val (r, c) = move
            board[r][c] = AI
            val score = evaluatePosition(board, r, c, AI) + evaluatePosition(board, r, c, HUMAN)
            board[r][c] = EMPTY
            move to score
        }.sortedByDescending { it.second }

        for ((move, _) in scoredCandidates) {
            if (System.currentTimeMillis() - startTime > SEARCH_TIME_MS) break

            val (r, c) = move
            board[r][c] = AI

            val score = if (GameEngine().checkWin(r, c, AI)) {
                SCORE_FIVE
            } else {
                minimax(board, depth - 1, alpha, beta, false, 1, startTime)
            }

            board[r][c] = EMPTY

            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
            alpha = max(alpha, bestScore)
        }

        return bestMove to bestScore
    }

    private fun minimax(
        board: Array<IntArray>,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        currentDepth: Int,
        startTime: Long
    ): Int {
        if (System.currentTimeMillis() - startTime > SEARCH_TIME_MS) return 0
        nodesEvaluated++

        var a = alpha
        var b = beta

        val candidates = getQuickCandidates(board)
        if (candidates.isEmpty() || depth == 0) {
            return evaluateBoard(board)
        }

        // Sort candidates
        val sorted = sortCandidates(board, candidates, a, b, currentDepth)

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for ((r, c) in sorted) {
                if (board[r][c] != EMPTY) continue
                board[r][c] = AI
                val score = if (GameEngine().checkWin(r, c, AI)) {
                    board[r][c] = EMPTY
                    SCORE_FIVE + depth
                } else {
                    val s = minimax(board, depth - 1, a, b, false, currentDepth + 1, startTime)
                    board[r][c] = EMPTY
                    s
                }
                maxEval = max(maxEval, score)
                a = max(a, score)
                if (b <= a) {
                    addKillerMove(currentDepth, r to c)
                    break
                }
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for ((r, c) in sorted) {
                if (board[r][c] != EMPTY) continue
                board[r][c] = HUMAN
                val score = if (GameEngine().checkWin(r, c, HUMAN)) {
                    board[r][c] = EMPTY
                    -SCORE_FIVE - depth
                } else {
                    val s = minimax(board, depth - 1, a, b, true, currentDepth + 1, startTime)
                    board[r][c] = EMPTY
                    s
                }
                minEval = min(minEval, score)
                b = min(b, score)
                if (b <= a) {
                    addKillerMove(currentDepth, r to c)
                    break
                }
            }
            return minEval
        }
    }

    private fun sortCandidates(
        board: Array<IntArray>,
        candidates: List<Pair<Int, Int>>,
        alpha: Int,
        beta: Int,
        depth: Int
    ): List<Pair<Int, Int>> {
        return candidates.map { move ->
            val (r, c) = move
            val attackScore = evaluatePosition(board, r, c, AI)
            val defendScore = evaluatePosition(board, r, c, HUMAN)
            val killerBonus = if (killerMoves[depth].contains(move)) 5000 else 0
            move to (attackScore + defendScore + killerBonus)
        }.sortedByDescending { it.second }
            .map { it.first }
            .take(10) // Limit branching factor
    }

    private fun addKillerMove(depth: Int, move: Pair<Int, Int>) {
        val list = killerMoves[depth]
        if (!list.contains(move)) {
            list.add(0, move)
            if (list.size > 5) list.removeAt(list.lastIndex)
        }
    }

    /**
     * Get candidate moves - empty cells near existing pieces
     */
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

    /**
     * Get quick candidates for deeper search levels (narrower search)
     */
    private fun getQuickCandidates(board: Array<IntArray>): List<Pair<Int, Int>> {
        val candidateSet = mutableSetOf<Pair<Int, Int>>()

        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] != EMPTY) {
                    for (dr in -1..1) {
                        for (dc in -1..1) {
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

    /**
     * Evaluate a single position for a player
     */
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

    /**
     * Analyze a line through the given position
     * Returns (count of consecutive pieces, number of open ends)
     */
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

        // Forward direction
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

        // Backward direction
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

    /**
     * Evaluate the entire board from AI's perspective
     */
    private fun evaluateBoard(board: Array<IntArray>): Int {
        var score = 0

        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] == AI) {
                    score += evaluatePosition(board, r, c, AI)
                } else if (board[r][c] == HUMAN) {
                    score -= (evaluatePosition(board, r, c, HUMAN) * 11 / 10) // Slight defensive bias
                }
            }
        }

        // Positional bonus: center control
        val center = SIZE / 2
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                if (board[r][c] == AI) {
                    score += (5 - abs(r - center) - abs(c - center))
                } else if (board[r][c] == HUMAN) {
                    score -= (5 - abs(r - center) - abs(c - center))
                }
            }
        }

        return score
    }
}
