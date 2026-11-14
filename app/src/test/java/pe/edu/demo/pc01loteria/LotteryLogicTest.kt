package pe.edu.demo.pc01loteria

import org.junit.Assert.*
import org.junit.Test

class LotteryLogicTest {
    private val balls = listOf(5,12,18,23,34,41).mapIndexed { i,n -> Ball(n,i+1) }
    @Test fun validPlay() {
        assertTrue(LotteryLogic.isValid(listOf(5,12,18,23,34,41)))
        assertFalse(LotteryLogic.isValid(listOf(5,12,18,23,34,34)))
        assertFalse(LotteryLogic.isValid(listOf(0,12,18,23,34,41)))
    }
    @Test fun winningAndLosing() {
        assertEquals(6, LotteryLogic.hits(listOf(5,12,18,23,34,41), balls))
        assertEquals(5, LotteryLogic.hits(listOf(5,12,18,23,34,42), balls))
        assertEquals("GANADOR", LotteryLogic.status("finalizado", balls, 6))
        assertEquals("SIGUE INTENTANDO", LotteryLogic.status("finalizado", balls, 5))
    }
    @Test fun doesNotRevealBeforeFinalized() {
        assertEquals("Sorteo en curso", LotteryLogic.status("en_curso", balls, 6))
        assertEquals("Esperando resultados completos", LotteryLogic.status("finalizado", balls.take(5), 5))
    }
}
