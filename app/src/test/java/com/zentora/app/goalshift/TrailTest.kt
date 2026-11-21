package com.zentora.app.goalshift

import java.util.ArrayDeque
import org.junit.Assert.*
import org.junit.Test

class TrailTest {
    @Test
    fun everyGardenCanBeCompletedAndRestoredAfterEveryMove() {
        for ((index, trail) in gardens.withIndex()) {
            val queue = ArrayDeque<Position>()
            val parents = mutableMapOf<Position, Pair<Position, Int>>()
            val seen = mutableSetOf(trail.start)
            queue.add(trail.start)
            var solution: Position? = null
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                if (trail.tiles[current.cell] == 'G') {
                    solution = current
                    break
                }
                for (action in Trail.ROAR until trail.tiles.length) {
                    val next = trail.step(current, action) ?: continue
                    if (seen.add(next)) {
                        parents[next] = current to action
                        queue.add(next)
                    }
                }
            }
            assertNotNull("Garden ${index+1} must have a route", solution)
            val path = mutableListOf<Int>()
            var cursor = solution!!
            while (cursor != trail.start) {
                val (previous, action) = parents.getValue(cursor)
                path.add(action)
                cursor = previous
            }
            val game = Journey(index)
            for (action in path.asReversed()) {
                assertTrue(game.act(action))
                val restored = Journey.decode(game.encode())!!
                assertEquals(game.current, restored.current)
                assertEquals(game.moves, restored.moves)
            }
            assertTrue(game.complete)
            while (game.undo()) {}
            assertEquals(trail.start, game.current)
        }
    }

    @Test
    fun lockedBridgesAndGateRejectMovesAndUndoRestoresPhaseAndShards() {
        val game = Journey(0)
        assertFalse(game.act(Trail.ROAR))
        assertFalse(game.act(24))
        assertTrue(game.act(6))
        assertTrue(game.act(7))
        assertFalse(game.act(12))
        assertTrue(game.act(Trail.ROAR))
        assertTrue(game.undo())
        assertFalse(game.current.night)
        for (action in listOf(Trail.ROAR, 12, 17, 18)) assertTrue(game.act(action))
        assertEquals(7, game.current.shards)
        assertTrue(game.undo())
        assertFalse(game.trail.passable(19, game.current))
        assertTrue(game.trail.hasShard(18, game.current))
        assertNull(Journey.decode("0,24"))
    }
}
