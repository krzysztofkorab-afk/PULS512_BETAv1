package pl.lab512.puls512

import android.Manifest
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.lab512.puls512.data.Article
import pl.lab512.puls512.data.NewsCategory
import pl.lab512.puls512.data.NewsRepository
import pl.lab512.puls512.data.NarratorProfile
import pl.lab512.puls512.data.SettingsStore
import pl.lab512.puls512.data.UserSettings
import pl.lab512.puls512.data.VerificationStatus
import pl.lab512.puls512.feedback.VoiceRecorder
import pl.lab512.puls512.schedule.AlarmScheduler
import pl.lab512.puls512.speech.PolishSpeech
import pl.lab512.puls512.ui.Border
import pl.lab512.puls512.ui.Coral
import pl.lab512.puls512.ui.Ice
import pl.lab512.puls512.ui.Ink
import pl.lab512.puls512.ui.Mint
import pl.lab512.puls512.ui.Muted
import pl.lab512.puls512.ui.PulsTheme
import pl.lab512.puls512.ui.Surface
import pl.lab512.puls512.ui.SurfaceRaised
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PulsTheme { PulsApp() } }
    }
}

private enum class Screen(val label: String, val glyph: String) {
    BRIEF("Brief", "●"),
    PLAN("Plan", "◷"),
    FEEDBACK("Uwagi", "✦")
}

@Composable
private fun PulsApp() {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    var settings by remember { mutableStateOf(settingsStore.load()) }
    var screen by remember { mutableStateOf(Screen.BRIEF) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        AlarmScheduler.scheduleAll(context, settings)
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun updateSettings(newSettings: UserSettings) {
        settings = newSettings
        settingsStore.save(newSettings)
        AlarmScheduler.scheduleAll(context, newSettings)
    }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Surface, tonalElevation = 0.dp) {
                Screen.entries.forEach { item ->
                    NavigationBarItem(
                        selected = screen == item,
                        onClick = { screen = item },
                        icon = { Text(item.glyph, fontSize = 18.sp) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(screen, label = "screen") { selected ->
            when (selected) {
                Screen.BRIEF -> BriefScreen(settings, Modifier.padding(padding))
                Screen.PLAN -> PlanScreen(settings, ::updateSettings, Modifier.padding(padding))
                Screen.FEEDBACK -> FeedbackScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun BrandHeader(kicker: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Ice, Mint))),
            contentAlignment = Alignment.Center
        ) { Text("ϟ", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("PULS 512", fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.2.sp)
            Text(kicker, color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(8.dp).clip(CircleShape).background(Mint))
        Spacer(Modifier.width(6.dp))
        Text("BETA", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BriefScreen(settings: UserSettings, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember { NewsRepository() }
    var articles by remember { mutableStateOf(repository.demoBriefing(settings.categories, settings.briefingLength)) }
    var loading by remember { mutableStateOf(false) }
    var refreshToken by remember { mutableStateOf(0) }
    var speaking by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(settings.narratorProfile) {
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) PolishSpeech.configure(engine, settings.narratorProfile)
        }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }

    LaunchedEffect(refreshToken, settings.categories, settings.briefingLength, settings.verifiedOnly) {
        loading = true
        articles = withContext(Dispatchers.IO) { repository.fetchBriefing(settings.categories, settings.briefingLength, settings.verifiedOnly) }
        loading = false
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { BrandHeader("Twój świat. Dwa razy dziennie.") }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(greeting(), color = Muted, fontSize = 14.sp)
                Text("Najważniejsze. Bez szumu.", fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(if (loading) Ice else Mint))
                            Spacer(Modifier.width(8.dp))
                            Text(if (loading) "Aktualizuję źródła…" else "Briefing gotowy", color = if (loading) Ice else Mint, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text(SimpleDateFormat("HH:mm", Locale("pl")).format(Date()), color = Muted)
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("${articles.size} informacji • około ${estimateMinutes(articles)} min słuchania", color = Muted)
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val engine = tts ?: return@Button
                                    if (speaking) {
                                        engine.stop(); speaking = false
                                    } else {
                                        speaking = PolishSpeech.speak(
                                            engine,
                                            PolishSpeech.briefingText(articles),
                                            settings.narratorProfile,
                                            "puls512_brief"
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Ice, contentColor = Ink),
                                modifier = Modifier.weight(1f)
                            ) { Text(if (speaking) "■  Zatrzymaj" else "▶  Odsłuchaj", fontWeight = FontWeight.Bold) }
                            OutlinedButton(onClick = { refreshToken++ }) { Text("Odśwież") }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("DZISIAJ", color = Ice, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(10.dp))
            }
        }
        if (loading && articles.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        items(articles) { article -> ArticleCard(article) }
        item {
            Text(
                "PULS 512 pokazuje krótkie streszczenia i odsyła do źródeł. Wersja beta może czasami pominąć materiał lub błędnie ocenić jego znaczenie.",
                color = Muted, fontSize = 11.sp, lineHeight = 16.sp,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

@Composable
private fun ArticleCard(article: Article) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth()
            .clickable(enabled = article.url.isNotBlank()) {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url))) }
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(article.category.label.uppercase(Locale("pl")), color = Ice, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .background(verificationColor(article.verificationStatus).copy(alpha = 0.14f))
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text("●  ${article.verificationStatus.label}", color = verificationColor(article.verificationStatus), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(article.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 23.sp)
            if (article.translatedToPolish) {
                Spacer(Modifier.height(5.dp))
                Text("Przetłumaczono na język polski", color = Ice, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(article.summary, color = Muted, fontSize = 14.sp, lineHeight = 20.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            if (article.whyItMatters.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceRaised).padding(12.dp)) {
                    Column {
                        Text("DLACZEGO TO WAŻNE", color = Ice, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(article.whyItMatters, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(article.verificationReason, color = verificationColor(article.verificationStatus), fontSize = 11.sp, lineHeight = 16.sp)
            if (article.sources.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("ORYGINALNE ŹRÓDŁA (${article.sources.size})", color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                article.sources.forEach { source ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 9.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceRaised)
                            .clickable(enabled = source.url.isNotBlank()) {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url))) }
                            }
                            .padding(10.dp)
                    ) {
                        Text(
                            "${if (source.primary) "◆" else "↗"}  ${source.name} — otwórz artykuł",
                            color = if (source.primary) Ice else Mint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (source.originalTitle.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(source.originalTitle, color = Muted, fontSize = 11.sp, lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanScreen(settings: UserSettings, onChange: (UserSettings) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { BrandHeader("Ustawienia briefingu") }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Twój rytm dnia", fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("Wybierz, kiedy i o czym chcesz wiedzieć.", color = Muted)
                Spacer(Modifier.height(20.dp))
                TimeCard("PORANNY PULS", "Dobry start bez przewijania portali", settings.morningEnabled, settings.morningHour, settings.morningMinute,
                    onToggle = { onChange(settings.copy(morningEnabled = it)) },
                    onTime = { h, m -> onChange(settings.copy(morningHour = h, morningMinute = m)) })
                Spacer(Modifier.height(12.dp))
                TimeCard("WIECZORNY PULS", "Najważniejsze wydarzenia całego dnia", settings.eveningEnabled, settings.eveningHour, settings.eveningMinute,
                    onToggle = { onChange(settings.copy(eveningEnabled = it)) },
                    onTime = { h, m -> onChange(settings.copy(eveningHour = h, eveningMinute = m)) })
                Spacer(Modifier.height(28.dp))
                SectionTitle("AUTOMATYCZNY ODSŁUCH", "O wybranej godzinie telefon przeczyta briefing po polsku.")
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Czytaj wiadomości automatycznie", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Godziny ustawiasz powyżej. Telefon pokaże też powiadomienie.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                        Switch(checked = settings.autoReadEnabled, onCheckedChange = { onChange(settings.copy(autoReadEnabled = it)) })
                    }
                }
                Spacer(Modifier.height(28.dp))
                NarratorSelector(
                    selected = settings.narratorProfile,
                    onSelected = { onChange(settings.copy(narratorProfile = it)) }
                )
                Spacer(Modifier.height(28.dp))
                SectionTitle("ZAKRES WIADOMOŚCI", "Zalecamy co najmniej Polskę, Europę i Świat.")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        NewsCategory.entries.filterIndexed { i, _ -> i % 2 == 0 }.forEach { category ->
                            CategoryChip(category, category in settings.categories) {
                                val updated = if (category in settings.categories) settings.categories - category else settings.categories + category
                                if (updated.isNotEmpty()) onChange(settings.copy(categories = updated))
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        NewsCategory.entries.filterIndexed { i, _ -> i % 2 == 1 }.forEach { category ->
                            CategoryChip(category, category in settings.categories) {
                                val updated = if (category in settings.categories) settings.categories - category else settings.categories + category
                                if (updated.isNotEmpty()) onChange(settings.copy(categories = updated))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(28.dp))
                SectionTitle("FILTR WIARYGODNOŚCI", "W trybie ścisłym ukrywamy wiadomości bez mocnego potwierdzenia.")
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Tylko potwierdzone", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Oficjalne dane albo zgodność co najmniej dwóch niezależnych źródeł.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
                        }
                        Switch(checked = settings.verifiedOnly, onCheckedChange = { onChange(settings.copy(verifiedOnly = it)) })
                    }
                }
                Spacer(Modifier.height(28.dp))
                SectionTitle("DŁUGOŚĆ BRIEFINGU", "Liczba najważniejszych informacji w jednym wydaniu.")
                Spacer(Modifier.height(14.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp)) {
                        Row {
                            Text("${settings.briefingLength} informacji", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("około ${settings.briefingLength / 2 + 1} min", color = Mint)
                        }
                        Slider(
                            value = settings.briefingLength.toFloat(),
                            onValueChange = { onChange(settings.copy(briefingLength = it.toInt())) },
                            valueRange = 4f..10f,
                            steps = 5
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Ustawienia zapisują się automatycznie.", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NarratorSelector(selected: NarratorProfile, onSelected: (NarratorProfile) -> Unit) {
    val context = LocalContext.current
    SectionTitle("POLSKI LEKTOR", "Wybierz głos i odsłuchaj próbkę przed zapisaniem.")
    Spacer(Modifier.height(12.dp))
    NarratorProfile.entries.forEach { profile ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                .clickable { onSelected(profile) },
            colors = CardDefaults.cardColors(containerColor = if (selected == profile) SurfaceRaised else Surface),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(30.dp).clip(CircleShape)
                        .background(if (selected == profile) Ice else Border),
                    contentAlignment = Alignment.Center
                ) { Text(if (selected == profile) "✓" else "♪", color = Ink, fontWeight = FontWeight.Black) }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.displayName, fontWeight = FontWeight.Bold)
                    Text("${profile.genderLabel} • ${profile.description}", color = Muted, fontSize = 11.sp)
                }
                OutlinedButton(onClick = { PolishSpeech.preview(context, profile) }) {
                    Text("Próbka")
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        "Aplikacja korzysta z polskich głosów zainstalowanych w telefonie. Jeśli urządzenie ma mniej głosów, profile nadal różnią się barwą i tempem.",
        color = Muted,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
}

@Composable
private fun TimeCard(title: String, subtitle: String, enabled: Boolean, hour: Int, minute: Int, onToggle: (Boolean) -> Unit, onTime: (Int, Int) -> Unit) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Ice, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(5.dp))
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Switch(checked = enabled, onCheckedChange = onToggle)
                Text(
                    String.format(Locale.ROOT, "%02d:%02d", hour, minute),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable {
                        TimePickerDialog(context, { _, h, m -> onTime(h, m) }, hour, minute, true).show()
                    }.background(SurfaceRaised).padding(horizontal = 12.dp, vertical = 7.dp),
                    fontWeight = FontWeight.Black, fontSize = 18.sp, color = if (enabled) Mint else Muted
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChip(category: NewsCategory, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(category.label) },
        leadingIcon = { Text(if (selected) "✓" else "+") },
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    )
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Text(title, color = Ice, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
    Spacer(Modifier.height(5.dp))
    Text(subtitle, color = Muted, fontSize = 13.sp)
}

@Composable
private fun FeedbackScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context) }
    var message by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    val pulse by animateFloatAsState(if (recording) 1.15f else 1f, label = "record")
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) runCatching { recorder.start(); recording = true }
            .onFailure { Toast.makeText(context, "Nie udało się uruchomić mikrofonu.", Toast.LENGTH_LONG).show() }
    }
    DisposableEffect(Unit) { onDispose { recorder.release() } }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { BrandHeader("Pomóż nam ulepszyć PULS 512") }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Co możemy poprawić?", fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("Napisz albo nagraj uwagę. Wiadomość zostanie przygotowana do wysłania na lab512512@gmail.com.", color = Muted, lineHeight = 20.sp)
                Spacer(Modifier.height(22.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Twoja uwaga") },
                    placeholder = { Text("Np. za dużo polityki, chcę więcej wiadomości lokalnych…") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { sendTextFeedback(context, message) },
                    enabled = message.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Ice, contentColor = Ink)
                ) { Text("Wyślij uwagę tekstową", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(26.dp))
                HorizontalDivider(color = Border)
                Spacer(Modifier.height(22.dp))
                Text("WIADOMOŚĆ GŁOSOWA", color = Ice, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size((76 * pulse).dp).clip(CircleShape)
                                .background(if (recording) Coral else SurfaceRaised)
                                .clickable {
                                    if (recording) {
                                        recordedFile = recorder.stop(); recording = false
                                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        runCatching { recorder.start(); recording = true }
                                            .onFailure { Toast.makeText(context, "Nie udało się uruchomić mikrofonu.", Toast.LENGTH_LONG).show() }
                                    } else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(if (recording) "■" else "●", color = if (recording) Ink else Coral, fontSize = 30.sp) }
                        Spacer(Modifier.height(12.dp))
                        Text(if (recording) "Nagrywanie… dotknij, aby zakończyć" else "Dotknij, aby nagrać", fontWeight = FontWeight.Bold)
                        AnimatedVisibility(recordedFile != null && !recording) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(Modifier.height(12.dp))
                                Text("Nagranie gotowe", color = Mint, fontSize = 13.sp)
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = { recordedFile?.let { sendVoiceFeedback(context, it, message) } }) {
                                    Text("Wyślij nagranie e-mailem")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("Aplikacja otworzy domyślną pocztę. To Ty zatwierdzasz wysłanie wiadomości i załącznika.", color = Muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

private fun sendTextFeedback(context: android.content.Context, body: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:lab512512@gmail.com")
        putExtra(Intent.EXTRA_SUBJECT, "PULS 512 beta — uwaga użytkownika")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    safeStart(context, intent)
}

private fun sendVoiceFeedback(context: android.content.Context, file: File, note: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/mp4"
        putExtra(Intent.EXTRA_EMAIL, arrayOf("lab512512@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, "PULS 512 beta — uwaga głosowa")
        putExtra(Intent.EXTRA_TEXT, note.ifBlank { "W załączniku przesyłam uwagę głosową." })
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    safeStart(context, Intent.createChooser(intent, "Wyślij uwagę przez…"))
}

private fun safeStart(context: android.content.Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Nie znaleziono aplikacji pocztowej.", Toast.LENGTH_LONG).show()
    }
}

private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Dzień dobry"
        in 12..17 -> "Dzień dobry"
        else -> "Dobry wieczór"
    }
}

private fun estimateMinutes(articles: List<Article>): Int = (articles.size / 2 + 1).coerceAtLeast(1)

private fun verificationColor(status: VerificationStatus): Color = when (status) {
    VerificationStatus.CONFIRMED -> Mint
    VerificationStatus.OFFICIAL_SOURCE -> Ice
    VerificationStatus.DEVELOPING -> Coral
}
