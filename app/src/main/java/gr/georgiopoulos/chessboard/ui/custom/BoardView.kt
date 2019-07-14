package gr.georgiopoulos.chessboard.ui.custom

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import gr.georgiopoulos.chessboard.R
import gr.georgiopoulos.chessboard.model.Possition

import java.util.ArrayList
import kotlin.math.pow
import kotlin.math.sqrt

class BoardView : GridLayout {

    companion object {

        /**
         * Board dimension.
         * Default 8x8.
         */
        const val BOARD_DIMENSION = 8
    }

    private var dimension: Int? = null

    private var piecesMatrix: Array<Array<View?>>? = null

    /**
     * Tile background.
     */
    private var lightTile: Drawable? = null
    private var darkTile: Drawable? = null
    private var markedTileColor: Int = 0
    private var invalidPosClickColor: Int = 0


    private var lastSelectedPiece: View? = null
    private var currentPieceSelected: View? = null

    /**
     * Marked tiles for the last selected piece.
     */
    private var markedTiles: MutableList<View>? = null
    private var markedTilesEnabled: Boolean = false

    /**
     * Marking tile animation.
     */
    private var anim: Int = 0

    /**
     * Piece's movement anim duration.
     */
    private var aniMovDur = 500

    private val animMovListDur = 2000

    /**
     * Enables/Disables click on the board.
     */
    var isClickEnabled = true

    /**
     * Listener.
     */
    private var boardListener: BoardListener? = null

    private var dimm = -1

    private var alreadyCreated = false

    private val pieceQueues = ArrayList<PieceQueue>()

    constructor(context: Context) : super(context) {
        val arrayOfNulls: Array<View?> = arrayOfNulls(dimension ?: BOARD_DIMENSION)
        piecesMatrix = Array(dimension ?: BOARD_DIMENSION) { arrayOfNulls }
        markedTiles = ArrayList()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initVar(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initVar(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        initVar(context, attrs)
    }

    public override fun onMeasure(width: Int, height: Int) {
        val parentWidth = MeasureSpec.getSize(width)
        val parentHeight = MeasureSpec.getSize(height)
        this.setMeasuredDimension(parentWidth, parentHeight)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        )
        dimm = parentWidth
        if (!alreadyCreated) {
            createBoard(parentWidth)

            for (pieceQueue in pieceQueues) {
                setPiece(pieceQueue.pos!!.i, pieceQueue.pos!!.j, pieceQueue.imageId)
            }

            alreadyCreated = true
        }
    }

    private fun initVar(context: Context, attrs: AttributeSet) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.BoardView,
            0, 0
        )

        try {
            dimension = a.getInt(
                R.styleable.BoardView_dimension,
                BOARD_DIMENSION
            )
            markedTilesEnabled = a.getBoolean(R.styleable.BoardView_tileMarkingEnabled, false)
            isClickEnabled = a.getBoolean(R.styleable.BoardView_clickEnabled, true)
            darkTile = a.getDrawable(R.styleable.BoardView_darkTileImage)
            lightTile = a.getDrawable(R.styleable.BoardView_lightTileImage)
            markedTileColor = a.getColor(R.styleable.BoardView_markedTileColor, Color.CYAN)
            anim = a.getInt(R.styleable.BoardView_tileMarkingAnimation, 0)
            invalidPosClickColor = a.getColor(R.styleable.BoardView_invalidPosClickColor, Color.RED)
        } finally {
            a.recycle()
        }
        val arrayOfNulls: Array<View?> = arrayOfNulls(dimension ?: BOARD_DIMENSION)
        piecesMatrix = Array(dimension ?: BOARD_DIMENSION) { arrayOfNulls }
        markedTiles = ArrayList()
    }

    fun setDimension(dimension: Int) {
        this.dimension = dimension
        val arrayOfNulls: Array<View?> = arrayOfNulls(dimension)
        piecesMatrix = Array(dimension) { arrayOfNulls }
    }

    fun getDimension(): Int {
        return dimension ?: BOARD_DIMENSION
    }

    fun enableMarkedTiles(bool: Boolean) {
        this.markedTilesEnabled = bool
    }

    fun setDurationMovAnim(duration: Int) {
        this.aniMovDur = duration
    }

    private fun createBoard(dimm: Int) {
        this.rowCount = dimension ?: BOARD_DIMENSION
        this.columnCount = dimension ?: BOARD_DIMENSION

        setBoardBackground(dimm)
    }

    /**
     * Remove a piece from the board.
     * @param i coordenada i.
     * @param j coordenada j.
     */
    fun removePiece(i: Int, j: Int) {
        if (!isPosValid(i, j)) {
            throw IllegalArgumentException("Invalid position: [$i;$j].")
        }

        val view = piecesMatrix!![i][j]
        view?.visibility = View.GONE
        piecesMatrix!![i][j] = null
    }

    /**
     * Create board background.
     */
    private fun setBoardBackground(dimm: Int) {
        val boardDimension = dimension ?: BOARD_DIMENSION
        for (i in 0 until boardDimension) {
            for (j in 0 until boardDimension) {
                val tile = createTile(chooseTileColor(i, j), dimm)
                addViewToGrid(tile, i, j)
            }
        }
    }

    /**
     * Create a piece and set it on te given position.
     * @param i position coord i.
     * @param j position coord j.
     */
    fun setPiece(i: Int, j: Int, imageid: Int) {
        if (dimm == -1) {
            pieceQueues.add(PieceQueue(Possition(i, j), imageid))
            return
        }

        val peca = createPiece(imageid)

        if (!isPosValid(i, j)) {
            throw IllegalArgumentException("Invalid position: [$i;$j].")
        }

        piecesMatrix!![i][j] = peca

        addViewToGrid(peca, i, j)
    }

    /**
     * Aux for background intercalation.
     * @param i tile coord i.
     * @param j tile coord j.
     * @return image id.
     */
    private fun chooseTileColor(i: Int, j: Int): Drawable? {
        return if (i % 2 == 0) {
            if (j % 2 == 0) {
                lightTile
            } else {
                darkTile
            }
        } else {
            if (j % 2 == 0) {
                darkTile
            } else {
                lightTile
            }
        }
    }

    /**
     * Get tile position.
     * @param tile tile being searched.
     * @return Possition instance.
     */
    private fun getTilePos(tile: View): Possition {
        val index = this.indexOfChild(tile)
        val boardDimension = dimension ?: BOARD_DIMENSION
        val linha = index / boardDimension
        val coluna = index % boardDimension

        return Possition(linha, coluna)
    }

    /**
     * Get piece position[
     * @param piece piece being searched.
     * @return Possition instance.
     */
    private fun getPiecePos(piece: View?): Possition? {
        val boardDimension = dimension ?: BOARD_DIMENSION
        for (i in 0 until boardDimension) {
            for (j in 0 until boardDimension) {
                val aux = piecesMatrix!![i][j]
                if (aux != null) {
                    if (aux == piece) {
                        return Possition(i, j)
                    }
                }
            }
        }

        return null
    }

    /**
     *
     * @param view View being added.
     * @param i i coord.
     * @param j j coord.
     */
    private fun addViewToGrid(view: View, i: Int, j: Int) {
        val indexI = spec(i)
        val indexJ = spec(j)

        val gridParam = LayoutParams(indexI, indexJ)

        this.addView(view, gridParam)
    }

    private fun iterativeFadeInAnim(duration: Int, offset: Int): Animation {
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.duration = duration.toLong()
        fadeIn.startOffset = offset.toLong()

        return fadeIn
    }

    private fun simpleFadeIn(duration: Int): Animation {
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = DecelerateInterpolator()
        fadeIn.duration = duration.toLong()

        return fadeIn
    }

    private fun fadeViewOut(view: View) {
        val frameLayout = view as FrameLayout
        val image = frameLayout.getChildAt(1)

        val animation = simpleFadeIn(100)


        image.setBackgroundColor(invalidPosClickColor)
        image.visibility = View.VISIBLE

        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {

            }

            override fun onAnimationEnd(animation: Animation) {
                image.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })

        image.startAnimation(animation)
    }

    fun invalidPosAnim(i: Int, j: Int) {
        val view = getTile(Possition(i, j))
        val frameLayout = view as FrameLayout
        val image = frameLayout.getChildAt(1)

        val animation = simpleFadeIn(100)


        image.setBackgroundColor(invalidPosClickColor)
        image.visibility = View.VISIBLE

        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {

            }

            override fun onAnimationEnd(animation: Animation) {
                image.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })

        image.startAnimation(animation)
    }

    private fun selectAnimation(duration: Int, offset: Int): Animation {
        when (anim) {
            0 -> return simpleFadeIn(duration)
        }

        return iterativeFadeInAnim(duration, offset)
    }

    private fun markTile(i: Int, j: Int, offset: Int) {
        val boardDimension = dimension ?: BOARD_DIMENSION
        val pos = i * boardDimension + j
        val view = this.getChildAt(pos) as FrameLayout
        val image = view.getChildAt(1)
        image.setBackgroundColor(markedTileColor)
        val animation = selectAnimation(350, offset)
        image.visibility = View.VISIBLE

        image.startAnimation(animation)

        markedTiles!!.add(view)
    }

    fun unmarkTile(i: Int, j: Int) {
        val boardDimension = dimension ?: BOARD_DIMENSION
        val pos = i * boardDimension + j
        val view = this.getChildAt(pos) as FrameLayout
        val image = view.getChildAt(1)
        image.visibility = View.GONE
    }

    fun unmarkAllTiles() {
        for (view in markedTiles!!) {
            val posViewTabuleiro = getTilePos(view)
            unmarkTile(posViewTabuleiro.i, posViewTabuleiro.j)
        }

        markedTiles!!.clear()
    }


    private fun createTile(imageId: Drawable?, dimm: Int): FrameLayout {
        val boardDimension = dimension ?: BOARD_DIMENSION
        val size = dimm / boardDimension

        val frameLayout = FrameLayout(context)

        val params = FrameLayout.LayoutParams(size, size)

        val piece = ImageView(context)
        piece.layoutParams = params
        piece.background = imageId
        piece.scaleType = ImageView.ScaleType.CENTER_CROP

        val marked = FrameLayout(context)
        marked.layoutParams = params
        marked.setBackgroundColor(markedTileColor)
        marked.alpha = 0.5.toFloat()
        marked.visibility = View.GONE

        frameLayout.setOnClickListener(onClickTile())

        frameLayout.addView(piece)
        frameLayout.addView(marked)

        return frameLayout
    }


    private fun createPiece(imageId: Int): LinearLayout {
        val boardDimension = dimension ?: BOARD_DIMENSION
        val size = dimm / boardDimension

        val frameLayout = LinearLayout(context)

        val params = LinearLayout.LayoutParams(size, size, Gravity.CENTER.toFloat())

        val piece = ImageView(context)
        val padding = pxFromDp(context, 5f).toInt()
        piece.setPadding(padding, padding, padding, padding)
        piece.layoutParams = params
        piece.setImageResource(imageId)
        piece.scaleType = ImageView.ScaleType.CENTER_CROP

        frameLayout.setOnClickListener(onClickPiece())

        frameLayout.addView(piece)

        return frameLayout
    }

    private fun pxFromDp(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    private fun getPiece(pos: Possition): View? {
        return piecesMatrix!![pos.i][pos.j]
    }

    private fun getTile(pos: Possition): View {
        return getChildAt(pos.i * 8 + pos.j)
    }


    private fun onClickPiece(): OnClickListener {
        return OnClickListener { v ->
            if (!isClickEnabled) {
                return@OnClickListener
            }

            unmarkAllTiles()

            val isSameLast = v == lastSelectedPiece

            currentPieceSelected = if (isSameLast && currentPieceSelected != null) null else v

            lastSelectedPiece = v
            if (boardListener != null) {
                val pos = getPiecePos(v)
                boardListener!!.onClickPiece(pos, isSameLast && currentPieceSelected == null)
            }
        }
    }


    private fun onClickTile(): OnClickListener {
        return OnClickListener { v ->
            if (!isClickEnabled) {
                return@OnClickListener
            }

            if (markedTilesEnabled && markedTiles!!.indexOf(v) == -1) {
                fadeViewOut(v)
                return@OnClickListener
            }

            if (boardListener != null) {
                val pos = getTilePos(v)
                val posPeca = getPiecePos(lastSelectedPiece)
                boardListener!!.onClickTile(posPeca, pos)
            }

            unmarkAllTiles()
        }
    }

    fun movePiece(startPos: Possition, endPos: Possition) {
        val view = piecesMatrix!![startPos.i][startPos.j]

        piecesMatrix!![endPos.i][endPos.j] = piecesMatrix!![startPos.i][startPos.j]
        piecesMatrix!![startPos.i][startPos.j] = null
        val boardDimension = dimension ?: BOARD_DIMENSION
        val casa = getChildAt(endPos.i * boardDimension + endPos.j)

        view?.let { pieceMovAnim(it, casa) }
    }

    fun movePiece(positions: List<Possition>) {
        val piece = getPiece(positions[0])

        val eachAnimDur = animMovListDur / positions.size

        for (i in 1 until positions.size) {
            val pos = positions[i]
            val tile = getTile(pos)

            val finalX = tile.x
            val finalY = tile.y

            val animX = ObjectAnimator.ofFloat(piece, "x", finalX)
            val animY = ObjectAnimator.ofFloat(piece, "y", finalY)
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(animX, animY)
            animatorSet.duration = eachAnimDur.toLong()
            animatorSet.startDelay = (eachAnimDur * i).toLong()
            animatorSet.start()
        }
    }

    private fun pieceMovAnim(piece: View, tile: View) {
        val finalX = tile.x
        val finalY = tile.y

        val animX = ObjectAnimator.ofFloat(piece, "x", finalX)
        val animY = ObjectAnimator.ofFloat(piece, "y", finalY)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animX, animY)
        animatorSet.duration = aniMovDur.toLong()
        animatorSet.start()
    }

    fun markTiles(positions: MutableList<Possition>) {
        val ordered = orderPos(positions)

        var offset = 0

        for (pos in ordered) {
            markTile(pos.i, pos.j, offset)
            offset += 50
        }
    }

    private fun orderPos(positions: MutableList<Possition>): List<Possition> {
        for (i in positions.indices) {
            for (j in i until positions.size) {
                if (biggerThan(getPiecePos(lastSelectedPiece), positions[i], positions[j])) {
                    val aux = positions[i]
                    positions[i] = positions[j]
                    positions[j] = aux
                }
            }
        }

        return positions
    }

    private fun biggerThan(posBase: Possition?, pos1: Possition, pos2: Possition): Boolean {
        val distance1 = distance(posBase!!, pos1)
        val distance2 = distance(posBase, pos2)

        return distance1 >= distance2
    }

    private fun distance(pos1: Possition, pos2: Possition): Int {
        val delta1 = (pos1.i - pos2.i).toDouble().pow(2.0)
        val delta2 = (pos1.j - pos2.j).toDouble().pow(2.0)

        return sqrt(delta1 + delta2).toInt()
    }

    private fun isPosValid(i: Int, j: Int): Boolean {
        val boardDimension = dimension ?: BOARD_DIMENSION
        val max = boardDimension - 1
        return !(i < 0 || i > max || j < 0 || j > max)
    }

    fun setBoardListener(boardListener: BoardListener) {
        this.boardListener = boardListener
    }

    interface BoardListener {
        fun onClickPiece(pos: Possition?, isSameLast: Boolean)
        fun onClickTile(posPiece: Possition?, posTile: Possition)
    }

    private inner class PieceQueue(var pos: Possition?, var imageId: Int)
}
