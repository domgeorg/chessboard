package gr.georgiopoulos.chessboard.ui.activity

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import gr.georgiopoulos.chessboard.R
import gr.georgiopoulos.chessboard.model.Possition
import gr.georgiopoulos.chessboard.model.Tile
import gr.georgiopoulos.chessboard.ui.custom.BoardView
import kotlinx.android.synthetic.main.activity_chessboard.*
import kotlinx.android.synthetic.main.layout_toolbar.*
import java.util.*

class ChessboardActivity : AppCompatActivity() {

    companion object {
        const val DEFAULT_DIMENSION = 8
        const val POST_DELAY_ANIMATION = 200L
    }

    private var chessBoardDimension = DEFAULT_DIMENSION
    private var chessboard = Array(DEFAULT_DIMENSION) { arrayOfNulls<Tile>(DEFAULT_DIMENSION) }
    private var queue: Queue<Tile> = LinkedList()
    private var materialDialog: MaterialDialog? = null
    private var knightPos: Possition? = null
    private var targetPos: Possition? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chessboard)
        //Populate the chessboard with position values as unreachable
        populateChessBoard()
        initLayout()
    }

    private fun initLayout() {
        closeImageView?.setOnClickListener { onBackPressed() }
        refreshImageView?.setOnClickListener { reset() }
        val chessBoardDistance = 8
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

                            if (tile.depth <= 3) {

                                // We found the Position. Now trace back from this position to get the actual shortest path
                                val path = getShortestPath(startTile, endTile)

                                val knightPath: ArrayList<Possition> = arrayListOf()

                                knightPath.add(Possition(tile.x, tile.y))
                                path.map {
                                    knightPath.add(Possition(it.x, it.y))
                                }

                                Handler().postDelayed({
                                    chessBoardView?.movePiece(knightPath.reversed())
                                }, POST_DELAY_ANIMATION)

                                Handler().postDelayed({
                                    resetTarget()
                                }, POST_DELAY_ANIMATION)


                            } else {
                                showErrorDialog("Oups", "The knight can not reach end position in 3 steps", closeListener = MaterialDialog.SingleButtonCallback { materialDialog: MaterialDialog, dialogAction: DialogAction ->
                                    materialDialog.dismiss()
                                    resetTarget()
                                })
                            }
                        } else {
                            // perform BFS on this Pos if it is not already visited
                            bfs(tile, ++tile.depth)
                        }
                    }
                    if (tile.depth == Integer.MAX_VALUE) {
                        showErrorDialog("Oups", "End position is not reachable for the knight")
                    }
                }
            }
        })
    }

    private fun resetTarget() {
        targetPos?.let {
            chessBoardView.removePiece(it.i, it.j)
            chessboard = Array(chessBoardDimension) { arrayOfNulls<Tile>(chessBoardDimension) }
            populateChessBoard()
            queue = LinkedList()
            targetPos = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        materialDialog?.dismiss()
    }


    private fun reset() {
        knightPos?.let {
            chessBoardView?.removePiece(it.i, it.j)
            knightPos = null
        }
        targetPos?.let {
            chessBoardView.removePiece(it.i, it.j)
            chessboard = Array(chessBoardDimension) { arrayOfNulls<Tile>(chessBoardDimension) }
            populateChessBoard()
            queue = LinkedList()
            targetPos = null
        }
    }

    fun showErrorDialog(
            error: String = "",
            errorDescription: String = "",
            closeListener: MaterialDialog.SingleButtonCallback? = null
    ) {
        val materialBuilder = MaterialDialog.Builder(this)
                .title(error)
                .content(errorDescription)
        closeListener?.let {
            materialBuilder.positiveText(getString(android.R.string.ok))
            materialBuilder.onPositive(closeListener)
        }

        materialDialog?.dismiss()
        materialDialog = materialBuilder.build()
        materialDialog?.show()
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
        for (i in 0 until chessboard.size) {
            for (j in 0 until chessboard[0].size) {
                chessboard[i][j] = Tile(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)
            }
        }
    }

    private fun inRange(x: Int, y: Int): Boolean {
        return x in 0..7 && 0 <= y && y < 8
    }

    /**
     * Check if this is a valid jump or position for Knight based on its current location
     */
    fun isValid(current: Tile, next: Tile): Boolean {
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
