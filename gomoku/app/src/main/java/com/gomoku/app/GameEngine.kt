package com.gomoku.app

class GameEngine {

    companion object {
        const val SIZE = 15
        const val EMPTY = 0
        const val BLACK = 1
        const val WHITE = 2
    }

    val board = Array(SIZE) { IntArray(SIZE) { EMPTY } }
    val history = mutableListOf<Pair<Int, Int>>()
    var currentPlayer = BLACK
    var gameOver = false
    var winner = EMPTY

    fun reset() {
        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                board[i][j] = EMPTY
            }
        }
        history.clear()
        currentPlayer = BLACK
        gameOver = false
        winner = EMPTY
    }

    fun place(row: Int, col: Int): Boolean {
        if (gameOver || board[row][col] != EMPTY) return false
        board[row][col] = currentPlayer
        history.add(row to col)

        if (checkWin(row, col, currentPlayer)) {
            winner = currentPlayer
            gameOver = true
        } else if (history.size == SIZE * SIZE) {
            gameOver = true
        }

        currentPlayer = if (currentPlayer == BLACK) WHITE else BLACK
        return true
    }

    fun undo(): Boolean {
        if (history.size < 2 || gameOver) return false
        // Undo both player and AI moves
        repeat(2) {
            if (history.isNotEmpty()) {
                val (r, c) = history.removeAt(history.lastIndex)
                board[r][c] = EMPTY
            }
        }
        currentPlayer = BLACK
        gameOver = false
        winner = EMPTY
        return true
    }

    fun checkWin(row: Int, col: Int, player: Int): Boolean {
        val directions = arrayOf(
            0 to 1,   // horizontal
            1 to 0,   // vertical
            1 to 1,   // diagonal
            1 to -1   // anti-diagonal
        )

        for ((dr, dc) in directions) {
            var count = 1
            // Check forward
            var r = row + dr
            var c = col + dc
            while (r in 0 until SIZE && c in 0 until SIZE && board[r][c] == player) {
                count++
                r += dr
                c += dc
            }
            // Check backward
            r = row - dr
            c = col - dc
            while (r in 0 until SIZE && c in 0 until SIZE && board[r][c] == player) {
                count++
                r -= dr
                c -= dc
            }
            if (count >= 5) return true
        }
        return false
    }

    fun isBoardEmpty(): Boolean = history.isEmpty()

    fun getAIWinner(): Boolean = winner == WHITE
    fun getPlayerWinner(): Boolean = winner == BLACK
}
