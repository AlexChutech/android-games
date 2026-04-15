package com.xiangqi.app

import android.os.Bundle
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

    private val engine = XiangqiEngine()
    private val ai = AIEngine()

    private lateinit var xiangqiView: XiangqiView
    private lateinit var statusText: TextView
    private lateinit var btnNewGame: Button
    private lateinit var btnUndo: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        xiangqiView = findViewById(R.id.xiangqiView)
        statusText = findViewById(R.id.statusText)
        btnNewGame = findViewById(R.id.btnNewGame)
        btnUndo = findViewById(R.id.btnUndo)

        engine.reset()
        xiangqiView.setEngine(engine)

        updateUI()

        xiangqiView.setOnMoveListener { fr, fc, tr, tc ->
            onPlayerMove(fr, fc, tr, tc)
        }

        btnNewGame.setOnClickListener {
            engine.reset()
            xiangqiView.setEngine(engine)
            xiangqiView.clearSelection()
            xiangqiView.setLastMove(null)
            updateUI()
        }

        btnUndo.setOnClickListener {
            if (engine.undo()) {
                xiangqiView.setLastMove(null)
                xiangqiView.invalidate()
                updateUI()
            } else {
                Toast.makeText(this, "无法悔棋", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onPlayerMove(fr: Int, fc: Int, tr: Int, tc: Int) {
        if (engine.gameOver || !engine.isRedTurn) return

        val moved = engine.makeMove(fr, fc, tr, tc)
        if (!moved) {
            Toast.makeText(this, "无效走法", Toast.LENGTH_SHORT).show()
            return
        }

        xiangqiView.clearSelection()
        xiangqiView.setLastMove(Move(fr, fc, tr, tc))
        updateUI()

        if (engine.gameOver) {
            showGameOver()
            return
        }

        // Check if AI is in checkmate
        if (engine.isCheckmate()) {
            engine.gameOver = true
            engine.winner = 1
            showGameOver()
            return
        }

        // AI thinks
        statusText.text = "电脑思考中..."
        btnUndo.isEnabled = false
        xiangqiView.invalidate()

        CoroutineScope(Dispatchers.Main).launch {
            val aiMove = withContext(Dispatchers.Default) {
                ai.getBestMove(engine)
            }

            if (aiMove != null) {
                engine.makeMove(aiMove.fromRow, aiMove.fromCol, aiMove.toRow, aiMove.toCol)
                xiangqiView.setLastMove(aiMove)
                xiangqiView.invalidate()
            }

            if (engine.gameOver) {
                showGameOver()
            } else if (engine.isCheckmate()) {
                engine.gameOver = true
                engine.winner = 2
                showGameOver()
            } else {
                updateUI()
            }

            btnUndo.isEnabled = !engine.gameOver
        }
    }

    private fun updateUI() {
        when {
            engine.gameOver -> showGameOver()
            engine.isInCheck(true) && engine.isRedTurn -> {
                statusText.text = "将军! 你的回合 (红方)"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
            engine.isRedTurn -> {
                statusText.text = "你的回合 (红方)"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
            else -> {
                statusText.text = "电脑回合 (黑方)"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            }
        }

        btnUndo.isEnabled = engine.isRedTurn && !engine.gameOver
    }

    private fun showGameOver() {
        when (engine.winner) {
            1 -> {
                statusText.text = "红方胜利!"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
            2 -> {
                statusText.text = "黑方胜利!"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            }
            else -> {
                statusText.text = "和棋!"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
        }
    }
}
