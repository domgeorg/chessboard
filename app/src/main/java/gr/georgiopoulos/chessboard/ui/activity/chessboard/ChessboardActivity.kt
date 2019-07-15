package gr.georgiopoulos.chessboard.ui.activity.chessboard

import android.os.Bundle
import android.os.Handler
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import gr.georgiopoulos.chessboard.R
import gr.georgiopoulos.chessboard.model.Possition
import gr.georgiopoulos.chessboard.model.Tile
import gr.georgiopoulos.chessboard.ui.custom.boardView.BoardView
import kotlinx.android.synthetic.main.activity_chessboard.*
import kotlinx.android.synthetic.main.layout_toolbar.*
import java.util.*

class ChessboardActivity : AppCompatActivity() {

    companion object {
        const val DEFAULT_DIMENSION = 8
        const val POST_DELAY_ANIMATION = 200L
        const val CHESS_DIMENSION = "chess dimension"
    }

    private var chessboard = Array(DEFAULT_DIMENSION) { arrayOfNulls<Tile>(DEFAULT_DIMENSION) }
    private var chessBoardDistance = DEFAULT_DIMENSION
    private var queue: Queue<Tile> = LinkedList()
    private var isNotReachable: Boolean = true
    private var knightPos: Possition? = null
    private var targetPos: Possition? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chessBoardDistance = intent?.getIntExtra(CHESS_DIMENSION, DEFAULT_DIMENSION)
                ?: DEFAULT_DIMENSION
        chessboard = Array(chessBoardDistance) { arrayOfNulls<Tile>(chessBoardDistance) }
        setContentView(R.layout.activity_chessboard)
        //Populate the chessboard with position values as unreachable
        populateChessBoard()
        initLayout()
    }

    private fun initLayout() {
        closeImageView?.setOnClickListener { onBackPressed() }
        refreshImageView?.setOnClickListener { reset() }
        chessboardDimensionImageView?.setOnClickListener { showSelectDimensionDialog() }
        chessBoardView?.setDimension(chessBoardDistance)
        chessBoardView?.setBoardListener(object : BoardView.BoardListener {
            override fun onClickPiece(pos: Possition?, isSameLast: Boolean) {
                if (isSameLast) {
                    return
                }
            }

            override fun onClickTile(posPiece: Possition?, posTile: Possition) {
                if (knightPos == null) {
                    knightPos = Possition(posTile.i, posTile.j)
                    chessBoardView?.setPiece(posTile.i, posTile.j, R.drawable.drawable_horse)
                }
                if (targetPos == null && knightPos?.equals(posTile) == false) {
                    targetPos = Possition(posTile.i, posTile.j)
                    chessBoardView?.setPiece(posTile.i, posTile.j, R.drawable.vector_target_position)

                    val startTile = Tile(knightPos!!.i, knightPos!!.j, 0)
                    val endTile = Tile(targetPos!!.i, targetPos!!.j, Integer.MAX_VALUE)

                    //Assign starting depth for the source as 0 (as this position is reachable in 0 moves)
                    chessboard[0][1] = startTile

                    //Add start position to queue
                    queue.add(startTile)
                    var tile = endTile

                    // While queue is not empty
                    while (queue.size != 0) {
                        tile = queue.poll()

                        //If this position is same as the end position, you found the destination
                        if (endTile.isEqual(tile)) {
                            isNotReachable = false
                            if (tile.depth <= 3) {

                                // We found the Position. Now trace back from this position to get the actual shortest path
                                val path = getShortestPath(startTile, endTile)

                                val knightPath: ArrayList<Possition> = arrayListOf()

                                knightPath.add(Possition(tile.x, tile.y))
                                path.map {
                                    knightPath.add(Possition(it.x, it.y))
                                }

                                Handler().postDelayed({ chessBoardView?.movePiece(knightPath.reversed()) }, POST_DELAY_ANIMATION)
                            } else {
                                showErrorDialog(R.string.error_title, R.string.error_message_steps) { resetTarget() }
                            }
                        } else {
                            // perform BFS on this Pos if it is not already visited
                            bfs(tile, ++tile.depth)
                        }
                    }
                    if (isNotReachable) {
                        showErrorDialog(R.string.error_title, R.string.error_message_not_reachable) {}
                    }
                }
            }
        })
    }

    private fun resetTarget() {
        targetPos?.let {
            chessBoardView.removePiece(it.i, it.j)
            chessboard = Array(chessBoardDistance) { arrayOfNulls<Tile>(chessBoardDistance) }
            populateChessBoard()
            queue = LinkedList()
            isNotReachable = true
            targetPos = null
        }
    }

    private fun reset() {
        knightPos?.let {
            chessBoardView?.removePiece(it.i, it.j)
            knightPos = null
        }
        resetTarget()
    }

    private fun showSelectDimensionDialog() {
        val chessboardDimens: ArrayList<Int> = arrayListOf()
        for (i in 6..16) {
            chessboardDimens.add(i)
        }
        val dialogItems: ArrayList<String> = arrayListOf()
        chessboardDimens.map {
            dialogItems.add(it.toString())
        }

        MaterialDialog(this).show {
            title(R.string.select_dimension_title)
            listItemsSingleChoice(items = dialogItems) { dialog, index, text ->
                chessBoardDistance = chessboardDimens[index]
                intent.putExtra(CHESS_DIMENSION, chessBoardDistance)
                ActivityCompat.startActivity(this@ChessboardActivity, intent, ActivityOptionsCompat
                        .makeCustomAnimation(this@ChessboardActivity, android.R.anim.fade_in, android.R.anim.fade_out)
                        .toBundle())
                finish()
            }
        }
    }

    private fun showErrorDialog(
            @StringRes error: Int,
            @StringRes errorDescription: Int,
            dialogAction: () -> Unit
    ) {
        MaterialDialog(this).show {
            cancelable(true)
            cancelOnTouchOutside(true)
            title(error)
            message(errorDescription)
            positiveButton(text = getString(android.R.string.ok)) {
                dialogAction
            }
            lifecycleOwner(this@ChessboardActivity)
        }
    }

    /**
     * Breadth First Search
     */
    private fun bfs(current: Tile, depth: Int) {
        // Start from -2 to +2 range and start marking each location on the board
        for (i in -2..2) {
            for (j in -2..2) {
                val next = Tile(current.x + i, current.y + j, depth)
                if (inRange(next.x, next.y)) {
                    //Skip if next location is same as the location you came from in previous run
                    if (current == next) continue
                    if (isValid(current, next)) {
                        val position = chessboard[next.x][next.y]
                        /*
						 * Get the current position object at this location on chessboard.
						 * If this location was reachable with a costlier depth, this iteration has given a shorter way to reach
						 */
                        if (position?.depth ?: 0 > depth) {
                            chessboard[current.x + i][current.y + j] = Tile(current.x, current.y, depth)
                            queue.add(next)
                        }
                    }
                }
            }
        }
    }

    /**
     * Populate initial chessboard values
     */
    private fun populateChessBoard() {
        val size = chessBoardDistance
        for (i in 0 until size) {
            for (j in 0 until size) {
                chessboard[i][j] = Tile(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)
            }
        }
    }

    private fun inRange(x: Int, y: Int): Boolean {
        return x in 0 until chessBoardDistance - 1 && 0 <= y && y < chessBoardDistance
    }

    /**
     * Check if this is a valid jump or position for Knight based on its current location
     */
    private fun isValid(current: Tile, next: Tile): Boolean {
        // Use Pythagoras theorem to ensure that a move makes a right-angled triangle with sides of 1 and 2. 1-squared + 2-squared is 5.
        val deltaR = next.x - current.x
        val deltaC = next.y - current.y
        return 5 == deltaR * deltaR + deltaC * deltaC
    }

    /**
     * Get the shortest Path and return iterable object
     */
    private fun getShortestPath(start: Tile, end: Tile): Iterable<Tile> {
        val path = Stack<Tile>()
        var current = chessboard[end.x][end.y]
        while (current?.isEqual(start) == false) {
            path.add(current)
            current = chessboard[current.x][current.y]
        }
        path.add(Tile(start.x, start.y, 0))
        return path
    }

}
