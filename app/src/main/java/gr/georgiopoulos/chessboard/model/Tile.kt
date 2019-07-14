package gr.georgiopoulos.chessboard.model

data class Tile(val x: Int, val y: Int, var depth: Int) {

    fun isEqual(other: Tile): Boolean {
        return this.x == other.x && this.y == other.y
    }
}