package gr.georgiopoulos.chessboard.ui.activity.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import gr.georgiopoulos.chessboard.ui.activity.chessboard.ChessboardActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, ChessboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}