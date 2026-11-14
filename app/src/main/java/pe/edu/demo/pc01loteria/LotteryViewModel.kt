package pe.edu.demo.pc01loteria

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LotteryViewModel : ViewModel() {
    private val repo = FirebaseRepository()
    private val _uid = MutableStateFlow(repo.auth.currentUser?.uid)
    val uid: StateFlow<String?> = _uid
    private val _email = MutableStateFlow(repo.auth.currentUser?.email.orEmpty())
    val email: StateFlow<String> = _email
    private val _draw = MutableStateFlow<Draw?>(null)
    val draw: StateFlow<Draw?> = _draw
    private val _play = MutableStateFlow<Play?>(null)
    val play: StateFlow<Play?> = _play
    private val _balls = MutableStateFlow<List<Ball>>(emptyList())
    val balls: StateFlow<List<Ball>> = _balls
    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private var drawJob: Job? = null
    private var playJob: Job? = null
    private var ballsJob: Job? = null
    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        updateSession(auth.currentUser?.uid, auth.currentUser?.email.orEmpty())
    }

    init {
        repo.auth.addAuthStateListener(authListener)
        updateSession(repo.auth.currentUser?.uid, repo.auth.currentUser?.email.orEmpty())
    }

    private fun updateSession(nextUid: String?, nextEmail: String) {
        if (_uid.value == nextUid && drawJob != null) return
        stopTracking()
        drawJob?.cancel(); playJob?.cancel()
        _uid.value = nextUid
        _email.value = nextEmail
        _draw.value = null; _play.value = null; _balls.value = emptyList()
        if (nextUid != null) {
            drawJob = viewModelScope.launch {
                try { repo.observeDraw().collect { _draw.value = it } }
                catch (e: Exception) { _error.value = e.localizedMessage ?: "Error leyendo sorteo" }
            }
            playJob = viewModelScope.launch {
                try { repo.observePlay(nextUid).collect { _play.value = it } }
                catch (e: Exception) { _error.value = e.localizedMessage ?: "Error leyendo jugada" }
            }
        }
    }

    fun login(email: String, password: String) = doAsync { repo.signIn(email, password) }
    fun register(email: String, password: String) = doAsync { repo.signUp(email, password) }
    fun save(numbers: List<Int>) = doAsync {
        val currentUid = _uid.value ?: error("Inicia sesión")
        repo.savePlay(currentUid, numbers)
    }

    private fun doAsync(action: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            _error.value = ""
            try { action() }
            catch (e: Exception) { _error.value = e.localizedMessage ?: "Ocurrió un error" }
            finally { _busy.value = false }
        }
    }

    fun startTracking() {
        if (ballsJob != null || _uid.value == null) return
        ballsJob = viewModelScope.launch {
            try { repo.observeBalls().collect { _balls.value = it } }
            catch (e: Exception) { _error.value = e.localizedMessage ?: "Error leyendo bolillas" }
        }
    }

    fun stopTracking() { ballsJob?.cancel(); ballsJob = null }
    fun signOut() { repo.signOut(); updateSession(null, "") }
    fun clearError() { _error.value = "" }

    override fun onCleared() {
        repo.auth.removeAuthStateListener(authListener)
        stopTracking()
        super.onCleared()
    }
}
