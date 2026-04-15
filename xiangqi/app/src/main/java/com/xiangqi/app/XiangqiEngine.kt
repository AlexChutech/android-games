package com.xiangqi.app

data class Move(val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int)

class XiangqiEngine {

    companion object {
        const val COLS = 9
        const val ROWS = 10
        const val EMPTY = 0

        // Red pieces (bottom, player)
        const val R_KING = 1
        const val R_ADVISOR = 2
        const val R_BISHOP = 3
        const val R_KNIGHT = 4
        const val R_ROOK = 5
        const val R_CANNON = 6
        const val R_PAWN = 7

        // Black pieces (top, AI)
        const val B_KING = 8
        const val B_ADVISOR = 9
        const val B_BISHOP = 10
        const val B_KNIGHT = 11
        const val B_ROOK = 12
        const val B_CANNON = 13
        const val B_PAWN = 14

        val PIECE_NAMES = mapOf(
            R_KING to "帥", R_ADVISOR to "仕", R_BISHOP to "相", R_KNIGHT to "傌",
            R_ROOK to "俥", R_CANNON to "炮", R_PAWN to "兵",
            B_KING to "將", B_ADVISOR to "士", B_BISHOP to "象", B_KNIGHT to "馬",
            B_ROOK to "車", B_CANNON to "砲", B_PAWN to "卒"
        )

        // Piece values for evaluation
        val PIECE_VALUES = mapOf(
            R_KING to 10000, R_ADVISOR to 20, R_BISHOP to 20, R_KNIGHT to 40,
            R_ROOK to 90, R_CANNON to 45, R_PAWN to 10,
            B_KING to 10000, B_ADVISOR to 20, B_BISHOP to 20, B_KNIGHT to 40,
            B_ROOK to 90, B_CANNON to 45, B_PAWN to 10
        )
    }

    val board = Array(ROWS) { IntArray(COLS) { EMPTY } }
    var isRedTurn = true  // Red (player) moves first
    var gameOver = false
    var winner = 0  // 0=none, 1=red wins, 2=black wins

    private val moveHistory = mutableListOf<Triple<Int, Int, Int>>()

    fun reset() {
        // Clear board
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                board[r][c] = EMPTY
            }
        }

        // Setup black pieces (top, rows 0-4)
        board[0][0] = B_ROOK
        board[0][1] = B_KNIGHT
        board[0][2] = B_BISHOP
        board[0][3] = B_ADVISOR
        board[0][4] = B_KING
        board[0][5] = B_ADVISOR
        board[0][6] = B_BISHOP
        board[0][7] = B_KNIGHT
        board[0][8] = B_ROOK
        board[2][1] = B_CANNON
        board[2][7] = B_CANNON
        board[3][0] = B_PAWN
        board[3][2] = B_PAWN
        board[3][4] = B_PAWN
        board[3][6] = B_PAWN
        board[3][8] = B_PAWN

        // Setup red pieces (bottom, rows 5-9)
        board[9][0] = R_ROOK
        board[9][1] = R_KNIGHT
        board[9][2] = R_BISHOP
        board[9][3] = R_ADVISOR
        board[9][4] = R_KING
        board[9][5] = R_ADVISOR
        board[9][6] = R_BISHOP
        board[9][7] = R_KNIGHT
        board[9][8] = R_ROOK
        board[7][1] = R_CANNON
        board[7][7] = R_CANNON
        board[6][0] = R_PAWN
        board[6][2] = R_PAWN
        board[6][4] = R_PAWN
        board[6][6] = R_PAWN
        board[6][8] = R_PAWN

        isRedTurn = true
        gameOver = false
        winner = 0
        moveHistory.clear()
    }

    fun isRedPiece(piece: Int): Boolean = piece in 1..7
    fun isBlackPiece(piece: Int): Boolean = piece in 8..14

    fun getPieceAt(row: Int, col: Int): Int {
        if (row !in 0 until ROWS || col !in 0 until COLS) return -1
        return board[row][col]
    }

    fun isValidMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        if (fromRow == toRow && fromCol == toCol) return false
        if (toRow !in 0 until ROWS || toCol !in 0 until COLS) return false

        val piece = board[fromRow][fromCol]
        if (piece == EMPTY) return false

        // Check if it's the right turn
        if (isRedTurn && !isRedPiece(piece)) return false
        if (!isRedTurn && !isBlackPiece(piece)) return false

        // Can't capture own piece
        val target = board[toRow][toCol]
        if (target != EMPTY) {
            if (isRedPiece(piece) && isRedPiece(target)) return false
            if (isBlackPiece(piece) && isBlackPiece(target)) return false
        }

        return when (piece) {
            R_KING, B_KING -> isValidKingMove(fromRow, fromCol, toRow, toCol, piece)
            R_ADVISOR, B_ADVISOR -> isValidAdvisorMove(fromRow, fromCol, toRow, toCol, piece)
            R_BISHOP, B_BISHOP -> isValidBishopMove(fromRow, fromCol, toRow, toCol, piece)
            R_KNIGHT, B_KNIGHT -> isValidKnightMove(fromRow, fromCol, toRow, toCol)
            R_ROOK, B_ROOK -> isValidRookMove(fromRow, fromCol, toRow, toCol)
            R_CANNON, B_CANNON -> isValidCannonMove(fromRow, fromCol, toRow, toCol)
            R_PAWN, B_PAWN -> isValidPawnMove(fromRow, fromCol, toRow, toCol, piece)
            else -> false
        }
    }

    // King (帥/將): moves one step orthogonally within palace
    private fun isValidKingMove(fr: Int, fc: Int, tr: Int, tc: Int, piece: Int): Boolean {
        if (tc < 3 || tc > 5) return false

        if (isRedPiece(piece)) {
            if (tr < 7 || tr > 9) return false
        } else {
            if (tr < 0 || tr > 2) return false
        }

        val dr = kotlin.math.abs(tr - fr)
        val dc = kotlin.math.abs(tc - fc)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }

    // Advisor (仕/士): moves one step diagonally within palace
    private fun isValidAdvisorMove(fr: Int, fc: Int, tr: Int, tc: Int, piece: Int): Boolean {
        if (tc < 3 || tc > 5) return false

        if (isRedPiece(piece)) {
            if (tr < 7 || tr > 9) return false
        } else {
            if (tr < 0 || tr > 2) return false
        }

        val dr = kotlin.math.abs(tr - fr)
        val dc = kotlin.math.abs(tc - fc)
        return dr == 1 && dc == 1
    }

    // Bishop (相/象): moves exactly 2 steps diagonally, cannot cross river
    private fun isValidBishopMove(fr: Int, fc: Int, tr: Int, tc: Int, piece: Int): Boolean {
        val dr = kotlin.math.abs(tr - fr)
        val dc = kotlin.math.abs(tc - fc)
        if (dr != 2 || dc != 2) return false

        // Cannot cross river
        if (isRedPiece(piece) && tr < 5) return false
        if (isBlackPiece(piece) && tr > 4) return false

        // Check blocking piece (象眼)
        val blockRow = (fr + tr) / 2
        val blockCol = (fc + tc) / 2
        return board[blockRow][blockCol] == EMPTY
    }

    // Knight (傌/馬): L-shape move, can be blocked
    private fun isValidKnightMove(fr: Int, fc: Int, tr: Int, tc: Int): Boolean {
        val dr = tr - fr
        val dc = tc - fc

        val validMoves = listOf(
            Pair(2, 1) to Pair(1, 0),
            Pair(2, -1) to Pair(1, 0),
            Pair(-2, 1) to Pair(-1, 0),
            Pair(-2, -1) to Pair(-1, 0),
            Pair(1, 2) to Pair(0, 1),
            Pair(1, -2) to Pair(0, -1),
            Pair(-1, 2) to Pair(0, 1),
            Pair(-1, -2) to Pair(0, -1)
        )

        for ((move, block) in validMoves) {
            if (dr == move.first && dc == move.second) {
                val blockRow = fr + block.first
                val blockCol = fc + block.second
                return board[blockRow][blockCol] == EMPTY
            }
        }
        return false
    }

    // Rook (俥/車): moves any distance orthogonally
    private fun isValidRookMove(fr: Int, fc: Int, tr: Int, tc: Int): Boolean {
        if (fr != tr && fc != tc) return false

        if (fr == tr) {
            val minC = minOf(fc, tc) + 1
            val maxC = maxOf(fc, tc)
            for (c in minC until maxC) {
                if (board[fr][c] != EMPTY) return false
            }
        } else {
            val minR = minOf(fr, tr) + 1
            val maxR = maxOf(fr, tr)
            for (r in minR until maxR) {
                if (board[r][fc] != EMPTY) return false
            }
        }
        return true
    }

    // Cannon (炮/砲): moves like rook, captures by jumping over exactly one piece
    private fun isValidCannonMove(fr: Int, fc: Int, tr: Int, tc: Int): Boolean {
        if (fr != tr && fc != tc) return false

        val target = board[tr][tc]
        var jumpCount = 0

        if (fr == tr) {
            val minC = minOf(fc, tc) + 1
            val maxC = maxOf(fc, tc)
            for (c in minC until maxC) {
                if (board[fr][c] != EMPTY) jumpCount++
            }
        } else {
            val minR = minOf(fr, tr) + 1
            val maxR = maxOf(fr, tr)
            for (r in minR until maxR) {
                if (board[r][fc] != EMPTY) jumpCount++
            }
        }

        return if (target == EMPTY) {
            jumpCount == 0  // Move without capture: no jumps
        } else {
            jumpCount == 1  // Capture: exactly one jump
        }
    }

    // Pawn (兵/卒): forward only, can move sideways after crossing river
    private fun isValidPawnMove(fr: Int, fc: Int, tr: Int, tc: Int, piece: Int): Boolean {
        val dr = tr - fr
        val dc = kotlin.math.abs(tc - fc)

        if (isRedPiece(piece)) {
            // Red pawn moves up (negative row)
            if (fr > 4) {  // Not crossed river
                return dr == -1 && dc == 0
            } else {  // Crossed river
                return (dr == -1 && dc == 0) || (dr == 0 && dc == 1)
            }
        } else {
            // Black pawn moves down (positive row)
            if (fr < 5) {  // Not crossed river
                return dr == 1 && dc == 0
            } else {  // Crossed river
                return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
            }
        }
    }

    fun makeMove(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int): Boolean {
        if (!isValidMove(fromRow, fromCol, toRow, toCol)) return false

        val captured = board[toRow][toCol]
        moveHistory.add(Triple(fromRow * 10 + fromCol, toRow * 10 + toCol, captured))

        board[toRow][toCol] = board[fromRow][fromCol]
        board[fromRow][fromCol] = EMPTY

        // Check for king capture (game over)
        if (captured == R_KING) {
            gameOver = true
            winner = 2  // Black wins
        } else if (captured == B_KING) {
            gameOver = true
            winner = 1  // Red wins
        }

        isRedTurn = !isRedTurn
        return true
    }

    fun undo(): Boolean {
        if (moveHistory.size < 2 || gameOver) return false

        // Undo both moves (player and AI)
        repeat(2) {
            if (moveHistory.isNotEmpty()) {
                val (from, to, captured) = moveHistory.removeAt(moveHistory.lastIndex)
                val fromRow = from / 10
                val fromCol = from % 10
                val toRow = to / 10
                val toCol = to % 10
                board[fromRow][fromCol] = board[toRow][toCol]
                board[toRow][toCol] = captured
            }
        }

        isRedTurn = true
        gameOver = false
        winner = 0
        return true
    }

    fun getAllValidMoves(forRed: Boolean): List<Move> {
        val moves = mutableListOf<Move>()

        for (fr in 0 until ROWS) {
            for (fc in 0 until COLS) {
                val piece = board[fr][fc]
                if (piece == EMPTY) continue
                if (forRed && !isRedPiece(piece)) continue
                if (!forRed && !isBlackPiece(piece)) continue

                for (tr in 0 until ROWS) {
                    for (tc in 0 until COLS) {
                        if (isValidMove(fr, fc, tr, tc)) {
                            moves.add(Move(fr, fc, tr, tc))
                        }
                    }
                }
            }
        }
        return moves
    }

    // Check if a king is in check
    fun isInCheck(isRed: Boolean): Boolean {
        // Find king position
        var kingRow = -1
        var kingCol = -1
        val kingPiece = if (isRed) R_KING else B_KING

        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                if (board[r][c] == kingPiece) {
                    kingRow = r
                    kingCol = c
                    break
                }
            }
        }

        if (kingRow == -1) return false

        // Check if any enemy piece can capture the king
        val enemyMoves = getAllValidMoves(!isRed)
        for ((_, _, tr, tc) in enemyMoves) {
            if (tr == kingRow && tc == kingCol) return true
        }

        return false
    }

    // Check if the game is checkmate for the current player
    fun isCheckmate(): Boolean {
        val moves = getAllValidMoves(isRedTurn)
        if (moves.isEmpty()) return true

        // Try each move to see if any escapes check
        for ((fr, fc, tr, tc) in moves) {
            val originalFrom = board[fr][fc]
            val originalTo = board[tr][tc]

            board[tr][tc] = originalFrom
            board[fr][fc] = EMPTY

            val stillInCheck = isInCheck(isRedTurn)

            board[fr][fc] = originalFrom
            board[tr][tc] = originalTo

            if (!stillInCheck) return false
        }

        return true
    }

    // Get piece name for display
    fun getPieceName(piece: Int): String = PIECE_NAMES[piece] ?: ""

    // Get piece value for AI evaluation
    fun getPieceValue(piece: Int): Int = PIECE_VALUES[piece] ?: 0
}
