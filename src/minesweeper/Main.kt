package minesweeper

import java.lang.Exception
import kotlin.random.Random
import java.util.*
import kotlin.system.exitProcess

enum class SIGNS(val sign: Char) {
    MINE('X'),
    EXPLORED('/'),
    UNEXPLORED('.'),
    MARK('*'),
}

enum class Suggestions(val value: String) {
    FREE("free"),
    MINE("mine")
}

data class Cell(
        val x: Int,
        val y: Int,
        var isMine: Boolean = false,
        var isOpened: Boolean = false,
        var isMarked: Boolean = false
)

class Field(private val minesAmount: Int) {
    companion object {
        private const val FIELD_WIDTH = 9
        private const val FIELD_HEIGHT = 9
        const val MAX_MINE_NUMBER = FIELD_HEIGHT * FIELD_WIDTH

        private const val FIELD_START = -2
        private const val BORDER_CELL = -1
        private val BORDERS_RANGE = FIELD_START until 0
    }

    private val cells: MutableList<MutableList<Cell>> = MutableList(FIELD_HEIGHT) { y -> MutableList(FIELD_WIDTH) { x -> Cell(x, y) } }

    private val mines: MutableList<Cell> = mutableListOf()
    private val extraMarks: MutableList<Cell> = mutableListOf()

    private var didFirstFreeMove: Boolean = false
    var gameOver: Boolean = false

    private fun fill() {
        val s = mutableSetOf<Int>()
        while (s.size < minesAmount) {
            s.add(Random.nextInt(0, MAX_MINE_NUMBER))
        }

        var currentPosition = 0
        for (currentLine in 0 until FIELD_HEIGHT) {
            for (currentPoint in 0 until FIELD_WIDTH) {
                this.set(currentPoint, currentLine, s.contains(currentPosition))
                currentPosition += 1
            }
        }
    }

    fun set(x: Int, y: Int, isMine: Boolean) {
        val cell = cells[y][x]
        cell.isMine = isMine
        if (isMine) {
            mines.add(cell)
        }
    }

    private fun getCellsAround(cell: Cell): MutableList<Cell> {
        val cellsAround = mutableListOf<Cell>()
        val nearMineRange = -1..1
        for (i in nearMineRange) {
            val targetRow = cell.y + i
            if (targetRow !in 0 until FIELD_HEIGHT) {
                continue
            }
            for (j in nearMineRange) {
                val targetColumn = cell.x + j
                if (targetColumn !in 0 until FIELD_WIDTH) {
                    continue
                }
                cellsAround.add(cells[targetRow][targetColumn])
            }
        }
        return cellsAround
    }

    private fun calculatesMinesAround(cell: Cell): Int {
        var minesAmount = 0
        for (neighborCell in getCellsAround(cell)) {
            if (neighborCell.isMine) {
                minesAmount += 1
            }
        }
        return minesAmount
    }

    fun show() {
        for (row in FIELD_START..FIELD_HEIGHT) {
            for (column in FIELD_START..FIELD_WIDTH) {
                print(
                        if (row in BORDERS_RANGE || row == FIELD_HEIGHT) {
                            if (row == FIELD_START) {
                                when (column) {
                                    FIELD_START -> ' '
                                    BORDER_CELL, FIELD_WIDTH -> '|'
                                    else -> column + 1
                                }
                            } else {
                                when (column) {
                                    BORDER_CELL, FIELD_HEIGHT -> '|'
                                    else -> '-'
                                }
                            }
                        } else {
                            if (column in BORDERS_RANGE || column == FIELD_WIDTH) {
                                if (column == FIELD_START) row + 1 else '|'
                            } else {
                                val cell = cells[row][column]
                                when {
                                    cell.isOpened -> {
                                        val minesAround = calculatesMinesAround(cell)
                                        if (minesAround == 0) SIGNS.EXPLORED.sign else minesAround
                                    }
                                    cell.isMarked -> SIGNS.MARK.sign
                                    cell.isMine -> if (gameOver) SIGNS.MINE.sign else SIGNS.UNEXPLORED.sign
                                    else -> SIGNS.UNEXPLORED.sign
                                }
                            }
                        }
                )
            }
            println()
        }
    }

    private fun toggleMark(cell: Cell) {
        cell.isMarked = !cell.isMarked
        if (didFirstFreeMove) {
            if (!cell.isMine) {
                if (cell.isMarked) {
                    extraMarks.add(cell)
                } else {
                    extraMarks.removeAt(extraMarks.indexOf(cell))
                }
            }
        }
    }

    private fun markMine(x: Int, y: Int) {
        toggleMark(cells[y][x])
    }

    private fun exploreCell(cell: Cell) {
        if (cell.isOpened) {
            return
        }
        if (cell.isMine) {
            gameOver = true
            return
        }
        cell.isOpened = true
        if (cell.isMarked) {
            toggleMark(cell)
        }
        if (calculatesMinesAround(cell) != 0) {
            return
        }
        for (neighborCell in getCellsAround(cell)) {
            exploreCell(neighborCell)
        }
    }

    private fun markFree(x: Int, y: Int) {
        if (!didFirstFreeMove) {
            fill()
            for (row in cells) {
                for (cell in row) {
                    if (cell.isMarked && !cell.isMine) {
                        extraMarks.add(cell)
                    }
                }
            }
            didFirstFreeMove = true
        }
        exploreCell(cells[y][x])
    }

    fun isCompleted(): Boolean {
        return extraMarks.isEmpty() && mines.fold(true, { acc, cell -> acc && cell.isMarked })
    }

    fun makeMove(x: Int, y: Int, suggestion: String) {
        when (suggestion) {
            Suggestions.MINE.value -> markMine(x, y)
            Suggestions.FREE.value -> markFree(x, y)
            else -> throw Exception("Unknown move")
        }
    }
}

fun main() {
    println("How many mines do you want on the fields?")

    val scanner = Scanner(System.`in`)
    val minesAmount = scanner.nextInt()

    if (minesAmount > Field.MAX_MINE_NUMBER) {
        println("Too many mines. Maximum amount is ${Field.MAX_MINE_NUMBER}")
        exitProcess(1)
    }

    val field = Field(minesAmount)
    field.show()

    while (true) {
        print("Set/unset mines marks or claim a cell as free: ")
        val x = scanner.nextInt() - 1
        val y = scanner.nextInt() - 1
        val suggestion = scanner.next()
        field.makeMove(x, y, suggestion)
        field.show()
        if (field.gameOver) {
            print("You stepped on a mine and failed!")
            break
        }
        if (field.isCompleted()) {
            print("Congratulations! You found all the mines!")
            break
        }
    }
}
