package gr.georgiopoulos.chessboard.model

class Possition(var i: Int, var j: Int) {

    fun equals(pos: Possition): Boolean {
        return pos.i == this.i && pos.j == this.j
    }

    override fun toString(): String {
        return "Possition{i= $i, j= $j }"
    }
}