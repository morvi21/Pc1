package pe.edu.demo.pc01loteria

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

const val DRAW_ID = "sorteo-prueba-2026"

class FirebaseRepository(
    val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun drawRef() = db.collection("sorteos").document(DRAW_ID)

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun signUp(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    fun signOut() = auth.signOut()

    fun observeDraw(): Flow<Draw?> = callbackFlow {
        val registration = drawRef().addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snapshot?.takeIf { it.exists() }?.let {
                Draw(it.id, it.getString("nombre").orEmpty(), it.getString("fecha").orEmpty(),
                    it.getString("estado").orEmpty())
            })
        }
        awaitClose { registration.remove() }
    }

    fun observePlay(uid: String): Flow<Play?> = callbackFlow {
        val registration = drawRef().collection("jugadas").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val numbers = (snapshot?.get("numeros") as? List<*>)
                    ?.mapNotNull { (it as? Number)?.toInt() }.orEmpty()
                trySend(if (snapshot?.exists() == true) Play(uid, numbers) else null)
            }
        awaitClose { registration.remove() }
    }

    fun observeBalls(): Flow<List<Ball>> = callbackFlow {
        val registration = drawRef().collection("bolillas")
            .orderBy("orden", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val balls = snapshot?.documents.orEmpty().mapNotNull { document ->
                    val numero = document.getLong("numero")?.toInt()
                    val orden = document.getLong("orden")?.toInt()
                    if (numero != null && orden != null) Ball(numero, orden) else null
                }
                trySend(balls)
            }
        awaitClose { registration.remove() }
    }

    suspend fun savePlay(uid: String, numbers: List<Int>) {
        require(LotteryLogic.isValid(numbers)) { "Seleccione 6 números distintos entre 1 y 45" }
        // Transaction ensures only one play and protects against the draw closing midway.
        db.runTransaction { transaction ->
            val draw = transaction.get(drawRef())
            require(draw.exists() && draw.getString("estado") == "abierto") {
                "El sorteo ya no está abierto"
            }
            val playRef = drawRef().collection("jugadas").document(uid)
            val oldPlay = transaction.get(playRef)
            require(!oldPlay.exists()) { "Ya registraste una jugada" }
            transaction.set(playRef, mapOf(
                "sorteoId" to DRAW_ID,
                "uid" to uid,
                "numeros" to numbers.sorted()
            ))
        }.await()
    }
}
