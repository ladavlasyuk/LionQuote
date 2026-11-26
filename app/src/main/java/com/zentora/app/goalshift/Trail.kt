package com.zentora.app.goalshift

import kotlin.math.abs

data class Position(val cell: Int, val night: Boolean = false, val shards: Int = 0)

class Trail(vararg rows: String) {
    val size = rows.size
    val tiles = rows.joinToString("")
    val start = Position(tiles.indexOf('L'))
    private val shards = tiles.indices.filter { tiles[it] == '*' }

    init {
        require(rows.all { it.length == size } && shards.size == 3)
        require(tiles.count { it == 'L' } == 1 && tiles.count { it == 'G' } == 1)
    }

    fun hasShard(cell: Int, state: Position): Boolean {
        val index = shards.indexOf(cell)
        return index >= 0 && state.shards and (1 shl index) == 0
    }

    fun passable(cell: Int, state: Position) =
        when (tiles[cell]) {
            '#' -> false
            's' -> !state.night
            'm' -> state.night
            'G' -> state.shards == 7
            else -> true
        }

    fun step(state: Position, target: Int): Position? {
        if (tiles[state.cell] == 'G') return null
        if (target == ROAR)
            return if (tiles[state.cell] == 'R') state.copy(night = !state.night) else null
        if (target !in tiles.indices || !passable(target, state)) return null
        val distance =
            abs(target % size - state.cell % size) + abs(target / size - state.cell / size)
        if (distance != 1) return null
        val shard = shards.indexOf(target)
        val mask = if (shard < 0) state.shards else state.shards or (1 shl shard)
        return Position(target, state.night, mask)
    }

    companion object {
        const val ROAR = -1
    }
}

class Journey(val index: Int) {
    val trail = gardens[index]
    private val states = mutableListOf(trail.start)
    private val actions = mutableListOf<Int>()
    val current
        get() = states.last()

    val moves
        get() = actions.size

    val complete
        get() = trail.tiles[current.cell] == 'G'

    fun act(target: Int): Boolean {
        val next = trail.step(current, target) ?: return false
        states.add(next)
        actions.add(target)
        return true
    }

    fun undo(): Boolean {
        if (moves == 0) return false
        actions.removeAt(actions.lastIndex)
        states.removeAt(states.lastIndex)
        return true
    }

    fun encode() = (listOf(index) + actions).joinToString(",")

    companion object {
        fun decode(encoded: String?): Journey? {
            if (encoded == null) return null
            val parts = encoded.split(',')
            val index = parts.first().toIntOrNull()?.takeIf { it in gardens.indices } ?: return null
            val journey = Journey(index)
            for (part in parts.drop(1)) if (!journey.act(part.toIntOrNull() ?: return null))
                return null
            return journey
        }
    }
}

val gardens =
    listOf(
        Trail("#####", "L*R##", "##m##", "##**G", "#####"),
        Trail("#*###", "#s###", "LRm*#", "###.#", "##G*."),
        Trail("L*R##", "##m##", "*mR##", "##s##", "##.*G"),
        Trail("*sR.*", "##m##", "L.R##", "#s###", "#*.G#"),
        Trail("#*#*##", "#s#m##", "LRmR.#", "###s.#", "#*.R.#", "#G####"),
        Trail("##*###", "##s###", "L.Rm*#", "##s#.#", "##*mR#", "####G#"),
        Trail("*sR###", "##m#*#", "L.RmR#", "#s##s#", "#*..R#", "####G#"),
        Trail("L.Rs*#", "##m###", "*mRmR#", "####s#", "#G.*.#", "######"),
        Trail("L.Rs*##", "##m####", "*mR.R##", "####s##", "##GmRm*", "#######", "#######"),
        Trail("##*#*##", "##s#m##", "L.RmR.#", "####s.#", "#*s.RmG", "#######", "#######"),
        Trail("#*sR###", "###m###", "L.sRm*#", "#s###.#", "#Rmm.R#", "#s###s#", "#*.##G#"),
        Trail("##*#*##", "##s#m##", "L.RmR.#", "##m##s#", "*mR.sR#", "#####m#", "####.G#"),
    )
