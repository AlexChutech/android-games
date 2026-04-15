package com.gomoku.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val game = GameEngine()
    private val ai = AIEngine()
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var gomokuView: GomokuView
    private lateinit var statusText: TextView
    private lateinit var btnNewGame: Button
    private lateinit var btnUndo: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gomokuView = findViewById(R.id.gomokuView)
        statusText = findViewById(R.id.statusText)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnUndo = findViewById(R.id.btnUndo)

        updateUI()

        gomokuView.setOnMoveListener { row, col ->
            onPlayerMove(row, col)
        }

        btnNewGame.setOnClickListener {
            game.reset()
            updateUI()
        }

        btnUndo.setOnClickListener {
            if (game.undo()) {
                updateUI()
            } else {
                Toast.makeText(this, "无法悔棋", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onPlayerMove(row: Int, col: Int) {
        if (game.gameOver || game.currentPlayer != GameEngine.BLACK) return
        if (game.board[row][col] != GameEngine.EMPTY) return

        val placed = game.place(row, col)
        if (!placed) return

        updateUI()

        if (!game.gameOver) {
            // AI thinks
            statusText.text = "电脑思考中..."
            btnUndo.isEnabled = false

            CoroutineScope(Dispatchers.Main).launch {
                val aiMove = withContext(Dispatchers.Default) {
                    ai.getBestMove(game.board)
                }
                val (r, c) = aiMove
                game.place(r, c)
                updateUI()
                btnUndo.isEnabled = !game.isBoardEmpty() && !game.gameOver
            }
        }
    }

    private fun updateUI() {
        gomokuView.updateBoard(game.board, game.history.lastOrNull())

        when {
            game.gameOver && game.getPlayerWinner() -> {
                statusText.text = "你赢了!"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            }
            game.gameOver && game.getAIWinner() -> {
                statusText.text = "电脑赢了!"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
            game.gameOver -> {
                statusText.text = "平局!"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
            game.currentPlayer == GameEngine.BLACK -> {
                statusText.text = "你的回合 (黑棋)"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            }
            else -> {
                statusText.text = "电脑思考中..."
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
            }
        }

        btnUndo.isEnabled = !game.isBoardEmpty() && !game.gameOver && game.currentPlayer == GameEngine.BLACK
    }
}
