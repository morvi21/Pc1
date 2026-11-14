package pe.edu.demo.pc01loteria

data class Draw(val id: String, val nombre: String, val fecha: String, val estado: String)
data class Ball(val numero: Int, val orden: Int)
data class Play(val uid: String, val numeros: List<Int>)

object LotteryLogic {
    fun isValid(numbers: List<Int>): Boolean =
        numbers.size == 6 && numbers.distinct().size == 6 && numbers.all { it in 1..45 }

    fun hits(numbers: List<Int>, balls: List<Ball>): Int =
        numbers.toSet().intersect(balls.map { it.numero }.toSet()).size

    fun status(drawState: String, balls: List<Ball>, hits: Int): String = when {
        drawState == "abierto" -> "Esperando el inicio del sorteo"
        drawState == "finalizado" && balls.size == 6 ->
            if (hits == 6) "GANADOR" else "SIGUE INTENTANDO"
        drawState == "finalizado" -> "Esperando resultados completos"
        else -> "Sorteo en curso"
    }
}
