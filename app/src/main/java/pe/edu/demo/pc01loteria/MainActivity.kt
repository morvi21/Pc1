package pe.edu.demo.pc01loteria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { LotteryApp() } }
    }
}

@Composable
fun LotteryApp(vm: LotteryViewModel = viewModel()) {
    val uid by vm.uid.collectAsStateWithLifecycle()
    val email by vm.email.collectAsStateWithLifecycle()
    val draw by vm.draw.collectAsStateWithLifecycle()
    val play by vm.play.collectAsStateWithLifecycle()
    val balls by vm.balls.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf("login") }
    LaunchedEffect(uid) { page = if (uid == null) "login" else "home" }
    DisposableEffect(page, uid) {
        if (uid != null && page == "tracking") vm.startTracking() else vm.stopTracking()
        onDispose { vm.stopTracking() }
    }

    Scaffold(topBar = {
        Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Lotería PC01", fontWeight = FontWeight.Bold)
                if (uid != null) TextButton(onClick = { vm.signOut() }) { Text("Salir") }
            }
        }
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).padding(16.dp)) {
            if (error.isNotBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = vm::clearError) { Text("Cerrar mensaje") }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (uid == null) {
                AuthScreen(register = page == "register", busy = busy,
                    onSubmit = { e, p -> if (page == "register") vm.register(e, p) else vm.login(e, p) },
                    toggle = { page = if (page == "register") "login" else "register" })
            } else {
                when (page) {
                    "tracking" -> TrackingScreen(draw, play, balls, email, onBack = { page = "home" })
                    "choose" -> ChooseScreen(draw, play, busy,
                        onSave = vm::save, onBack = { page = "home" },
                        onTracking = { page = "tracking" })
                    else -> HomeScreen(email, draw, play,
                        onChoose = { page = "choose" },
                        onTracking = { page = "tracking" })
                }
            }
        }
    }
}

@Composable
fun AuthScreen(register: Boolean, busy: Boolean, onSubmit: (String, String) -> Unit, toggle: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (register) "Crear cuenta" else "Iniciar sesión", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(email, { email = it }, label = { Text("Correo") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(), singleLine = true,
            modifier = Modifier.fillMaxWidth())
        Button(onClick = { onSubmit(email, password) }, enabled = !busy && email.isNotBlank() && password.isNotBlank()) {
            Text(if (register) "Registrarme" else "Ingresar")
        }
        TextButton(onClick = toggle) {
            Text(if (register) "¿Ya tienes cuenta? Inicia sesión" else "¿Sin cuenta? Regístrate")
        }
    }
}

@Composable
fun HomeScreen(email: String, draw: Draw?, play: Play?, onChoose: () -> Unit, onTracking: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Participante: $email")
        if (draw == null) {
            Text("No se encontró el sorteo. Créalo en la consola de Firestore siguiendo el README.")
            return@Column
        }
        Text(draw.nombre, style = MaterialTheme.typography.headlineSmall)
        Text("Fecha: ${draw.fecha}")
        Text("Estado: ${draw.estado}")
        Text(if (play == null) "Aún no tienes jugada registrada" else "Tu jugada: ${play.numeros.joinToString()}")
        if (play == null && draw.estado == "abierto") Button(onClick = onChoose) { Text("Registrar jugada") }
        Button(onClick = onTracking, enabled = play != null) { Text("Seguir sorteo") }
        if (play == null && draw.estado != "abierto") Text("El sorteo ya comenzó; no se aceptan nuevas jugadas")
    }
}

@Composable
fun ChooseScreen(draw: Draw?, play: Play?, busy: Boolean,
    onSave: (List<Int>) -> Unit, onBack: () -> Unit, onTracking: () -> Unit) {
    var selected by remember { mutableStateOf(setOf<Int>()) }
    LaunchedEffect(play) { if (play != null) onTracking() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Selecciona exactamente 6 números", style = MaterialTheme.typography.titleLarge)
        Text("${selected.size}/6 seleccionados · números del 1 al 45")
        LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            items((1..45).toList()) { n ->
                val chosen = n in selected
                OutlinedButton(onClick = {
                    selected = if (chosen) selected - n else if (selected.size < 6) selected + n else selected
                }, contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (chosen) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )) { Text("$n") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Volver") }
            Button(onClick = { onSave(selected.sorted()) },
                enabled = selected.size == 6 && draw?.estado == "abierto" && !busy && play == null) {
                Text("Guardar jugada")
            }
        }
    }
}

@Composable
fun TrackingScreen(draw: Draw?, play: Play?, balls: List<Ball>, email: String, onBack: () -> Unit) {
    val numbers = play?.numeros.orEmpty()
    val sortedBalls = balls.sortedBy { it.orden }
    val matches = LotteryLogic.hits(numbers, sortedBalls)
    val state = LotteryLogic.status(draw?.estado.orEmpty(), sortedBalls, matches)
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Seguimiento en tiempo real", style = MaterialTheme.typography.headlineSmall)
        Text("Usuario: $email")
        Text("${draw?.nombre.orEmpty()} · ${draw?.fecha.orEmpty()}")
        Text("Estado: ${draw?.estado ?: "Cargando..."}")
        Text("Bolillas publicadas: ${sortedBalls.size}/6")
        Text("Bolillas (orden de salida): ${sortedBalls.joinToString(" · ") { it.numero.toString() }.ifBlank { "Ninguna todavía" }}")
        Text("Mi jugada:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            numbers.forEach { number ->
                val hit = sortedBalls.any { it.numero == number }
                Surface(color = if (hit) Color(0xFFB4E7C2) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, Color.Gray)) {
                    Text(number.toString(), Modifier.padding(10.dp), color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("Aciertos: $matches")
        Text(state, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Los resultados se calculan localmente. Se actualizan solos cuando cambian los documentos de Firestore.")
        OutlinedButton(onClick = onBack) { Text("Volver") }
    }
}
