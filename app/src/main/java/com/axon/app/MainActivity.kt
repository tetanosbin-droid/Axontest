package com.axon.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = AxonLocalStore(this)
        setContent { AxonApp(store) }
    }
}

private enum class AxonTheme(val label: String, val description: String) {
    CYBER("Cyber", "Cyan électrique et violet"),
    MIDNIGHT("Minuit", "Bleu profond et argent"),
    LIGHT("Clair", "Une interface lumineuse")
}

private enum class AppPage(val label: String, val icon: String) {
    HOME("Accueil", "⌂"), CHAT("Chat", "◌"), SETTINGS("Réglages", "⚙")
}

private data class ChatMessage(val text: String, val fromUser: Boolean, val timestamp: Long)

private class AxonLocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("axon_local", Context.MODE_PRIVATE)

    fun readTheme(): AxonTheme = runCatching {
        AxonTheme.valueOf(prefs.getString("theme", AxonTheme.CYBER.name) ?: AxonTheme.CYBER.name)
    }.getOrDefault(AxonTheme.CYBER)

    fun writeTheme(theme: AxonTheme) {
        prefs.edit().putString("theme", theme.name).apply()
    }

    fun readMessages(): List<ChatMessage> = runCatching {
        val array = JSONArray(prefs.getString("messages", "[]") ?: "[]")
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            ChatMessage(
                text = item.optString("text"),
                fromUser = item.optBoolean("user"),
                timestamp = item.optLong("time")
            ).takeIf { it.text.isNotBlank() }
        }
    }.getOrDefault(emptyList())

    fun writeMessages(messages: List<ChatMessage>) {
        val array = JSONArray()
        messages.forEach { message ->
            array.put(JSONObject().put("text", message.text).put("user", message.fromUser).put("time", message.timestamp))
        }
        prefs.edit().putString("messages", array.toString()).apply()
    }
}

private data class AxonPalette(
    val background: Color,
    val surface: Color,
    val raised: Color,
    val accent: Color,
    val secondary: Color,
    val text: Color,
    val muted: Color,
    val border: Color
)

@Composable
private fun palette(theme: AxonTheme): AxonPalette = when (theme) {
    AxonTheme.CYBER -> AxonPalette(
        Color(0xFF080A12), Color(0xFF101421), Color(0xFF171C2B), Color(0xFF65F5FF),
        Color(0xFFB18AFF), Color(0xFFF2F5FF), Color(0xFF8E98AD), Color(0xFF293247)
    )
    AxonTheme.MIDNIGHT -> AxonPalette(
        Color(0xFF080D18), Color(0xFF101A2A), Color(0xFF18263B), Color(0xFF73B7FF),
        Color(0xFF8DA6C5), Color(0xFFF0F5FC), Color(0xFF92A0B5), Color(0xFF2A3A51)
    )
    AxonTheme.LIGHT -> AxonPalette(
        Color(0xFFF4F7FC), Color(0xFFFFFFFF), Color(0xFFEAF0F8), Color(0xFF007F91),
        Color(0xFF7350B5), Color(0xFF142033), Color(0xFF63718A), Color(0xFFD7E0EC)
    )
}

@Composable
private fun AxonApp(store: AxonLocalStore) {
    var theme by remember { mutableStateOf(store.readTheme()) }
    var page by remember { mutableStateOf(AppPage.HOME) }
    val messages = remember { mutableStateListOf<ChatMessage>().apply { addAll(store.readMessages()) } }
    val colors = palette(theme)

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(colors.background, colors.surface)))
                    .padding(WindowInsets.navigationBars.asPaddingValues())
            ) {
                TopBar(colors)
                when (page) {
                    AppPage.HOME -> HomeScreen(colors, onOpenChat = { page = AppPage.CHAT })
                    AppPage.CHAT -> ChatScreen(colors, messages, onSend = { text ->
                        messages.add(ChatMessage(text, true, System.currentTimeMillis()))
                        store.writeMessages(messages)
                    })
                    AppPage.SETTINGS -> SettingsScreen(colors, theme, onThemeSelected = {
                        theme = it
                        store.writeTheme(it)
                    })
                }
                BottomNav(page, colors, onSelect = { page = it })
            }
        }
    }
}

@Composable
private fun TopBar(colors: AxonPalette) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).border(1.dp, colors.accent.copy(alpha = .6f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text("A", color = colors.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(11.dp))
            Column {
                Text("AXON", color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("INTELLIGENCE PERSONNELLE", color = colors.muted, fontSize = 8.sp, letterSpacing = 1.4.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(Color(0xFF56E39A), CircleShape))
            Spacer(Modifier.width(7.dp))
            Text("SYSTÈME PRÊT", color = colors.muted, fontSize = 9.sp, letterSpacing = .8.sp)
        }
    }
}

@Composable
private fun HomeScreen(colors: AxonPalette, onOpenChat: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "axon-pulse")
        .animateFloat(0.94f, 1.04f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 26.dp)) {
            Text("VOTRE ESPACE", color = colors.muted, fontSize = 10.sp, letterSpacing = 2.6.sp)
            Spacer(Modifier.height(7.dp))
            Text("Intelligence en éveil", color = colors.text, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
        }
        AxonCore(colors, Modifier.scale(pulse.value))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 24.dp)) {
            Text("En attente de votre prochain signal", color = colors.muted, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            QuickAction("CHAT", "Écrire à AXON", colors, onOpenChat)
            Spacer(Modifier.height(18.dp))
            Text("MODE LOCAL · AUCUNE IA CONNECTÉE", color = colors.muted.copy(alpha = .75f), fontSize = 9.sp, letterSpacing = 1.1.sp)
        }
    }
}

@Composable
private fun AxonCore(colors: AxonPalette, modifier: Modifier = Modifier) {
    Box(modifier.size(258.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawCircle(colors.accent.copy(alpha = .08f), radius = size.minDimension * .49f, style = Stroke(width = 1.dp.toPx()))
            drawCircle(colors.secondary.copy(alpha = .19f), radius = size.minDimension * .40f, style = Stroke(width = 1.dp.toPx()))
            drawCircle(colors.accent.copy(alpha = .28f), radius = size.minDimension * .31f, style = Stroke(width = 2.dp.toPx()))
            drawArc(
                brush = Brush.sweepGradient(listOf(colors.accent.copy(alpha = .05f), colors.accent, colors.secondary, colors.accent.copy(alpha = .05f))),
                startAngle = 205f, sweepAngle = 255f, useCenter = false,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(size.width * .1f, size.height * .1f),
                size = androidx.compose.ui.geometry.Size(size.width * .8f, size.height * .8f)
            )
            drawCircle(colors.accent, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .08f, size.height * .46f))
            drawCircle(colors.secondary, radius = 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .86f, size.height * .29f))
        }
        Box(
            Modifier.size(150.dp)
                .background(Brush.radialGradient(listOf(colors.accent.copy(alpha = .17f), colors.secondary.copy(alpha = .07f), Color.Transparent)), CircleShape)
                .border(1.dp, Brush.linearGradient(listOf(colors.accent.copy(alpha = .7f), colors.secondary.copy(alpha = .55f))), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("A", fontSize = 61.sp, fontWeight = FontWeight.Light, color = colors.accent, lineHeight = 64.sp)
                Text("N O Y A U", fontSize = 8.sp, color = colors.secondary, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun QuickAction(label: String, detail: String, colors: AxonPalette, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(145.dp).background(colors.surface, RoundedCornerShape(16.dp))
            .border(1.dp, colors.border, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(14.dp)
    ) {
        Text("✦  $label", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Text(detail, color = colors.text, fontSize = 12.sp)
    }
}

@Composable
private fun ChatScreen(colors: AxonPalette, messages: List<ChatMessage>, onSend: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    Column(modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 18.dp)) {
        Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp)) {
            Text("CONVERSATION", color = colors.muted, fontSize = 9.sp, letterSpacing = 2.sp)
            Text("Chat avec AXON", color = colors.text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.accent.copy(alpha = .09f), RoundedCornerShape(12.dp))
                .border(1.dp, colors.accent.copy(alpha = .2f), RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("i", color = colors.accent, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(9.dp))
            Text("Mode local : vos messages sont enregistrés sur cet appareil. Aucun fournisseur IA n'est configuré.", color = colors.muted, fontSize = 11.sp, lineHeight = 15.sp)
        }
        if (messages.isEmpty()) {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("◌", color = colors.accent, fontSize = 32.sp)
                Spacer(Modifier.height(10.dp))
                Text("La conversation commence ici", color = colors.text, fontSize = 15.sp)
                Spacer(Modifier.height(5.dp))
                Text("Votre historique restera disponible sur cet appareil.", color = colors.muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                reverseLayout = true
            ) {
                items(messages.asReversed()) { message ->
                    MessageBubble(message, colors)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp)
                .background(colors.surface, RoundedCornerShape(18.dp)).border(1.dp, colors.border, RoundedCornerShape(18.dp))
                .padding(start = 15.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).padding(vertical = 9.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = colors.text, fontSize = 14.sp),
                cursorBrush = Brush.linearGradient(listOf(colors.accent, colors.secondary)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    val text = input.trim()
                    if (text.isNotEmpty()) { onSend(text); input = ""; focusManager.clearFocus() }
                }),
                decorationBox = { inner ->
                    Box {
                        if (input.isEmpty()) Text("Écrire un message…", color = colors.muted, fontSize = 14.sp)
                        inner()
                    }
                }
            )
            Box(
                Modifier.size(40.dp).background(colors.accent.copy(alpha = if (input.isBlank()) .2f else 1f), CircleShape)
                    .clickable {
                        val text = input.trim()
                        if (text.isNotEmpty()) { onSend(text); input = ""; focusManager.clearFocus() }
                    },
                contentAlignment = Alignment.Center
            ) { Text("↑", color = colors.background, fontSize = 21.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, colors: AxonPalette) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.fillMaxWidth(.86f).background(
                if (message.fromUser) colors.accent.copy(alpha = .13f) else colors.surface,
                RoundedCornerShape(16.dp)
            ).border(1.dp, if (message.fromUser) colors.accent.copy(alpha = .25f) else colors.border, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(if (message.fromUser) "VOUS" else "AXON", color = if (message.fromUser) colors.accent else colors.secondary, fontSize = 9.sp, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(5.dp))
            Text(message.text, color = colors.text, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun SettingsScreen(colors: AxonPalette, selected: AxonTheme, onThemeSelected: (AxonTheme) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("PERSONNALISATION", color = colors.muted, fontSize = 9.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 8.dp))
        Text("Réglages", color = colors.text, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(22.dp))
        Text("APPARENCE", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        Text("Choisissez le thème de votre cockpit AXON.", color = colors.muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        AxonTheme.entries.forEach { theme ->
            ThemeOption(theme, selected == theme, colors) { onThemeSelected(theme) }
            Spacer(Modifier.height(9.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("ÉTAT DES SERVICES", color = colors.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        SettingStatus("Fournisseur IA", "Non configuré", colors)
        SettingStatus("Mode vocal Live", "Non disponible dans ce jalon", colors)
        SettingStatus("Stockage du chat", "Local · cet appareil", colors)
        Spacer(Modifier.height(16.dp))
        Text("Les thèmes et l'historique sont conservés localement sur cet appareil.", color = colors.muted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun ThemeOption(theme: AxonTheme, selected: Boolean, colors: AxonPalette, onClick: () -> Unit) {
    val preview = when (theme) {
        AxonTheme.CYBER -> listOf(Color(0xFF080A12), Color(0xFF65F5FF), Color(0xFFB18AFF))
        AxonTheme.MIDNIGHT -> listOf(Color(0xFF080D18), Color(0xFF73B7FF), Color(0xFF8DA6C5))
        AxonTheme.LIGHT -> listOf(Color(0xFFF4F7FC), Color(0xFF007F91), Color(0xFF7350B5))
    }
    Row(
        Modifier.fillMaxWidth().background(if (selected) colors.accent.copy(alpha = .08f) else colors.surface, RoundedCornerShape(15.dp))
            .border(1.dp, if (selected) colors.accent.copy(alpha = .65f) else colors.border, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-7).dp)) {
            preview.forEach { swatch -> Box(Modifier.size(25.dp).background(swatch, CircleShape).border(1.dp, colors.border, CircleShape)) }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(theme.label, color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(theme.description, color = colors.muted, fontSize = 11.sp)
        }
        if (selected) Text("✓", color = colors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingStatus(label: String, value: String, colors: AxonPalette) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.text, fontSize = 13.sp)
        Text(value, color = colors.muted, fontSize = 11.sp, modifier = Modifier.alpha(.9f))
    }
}

@Composable
private fun BottomNav(selected: AppPage, colors: AxonPalette, onSelect: (AppPage) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp)
            .background(colors.surface.copy(alpha = .96f), RoundedCornerShape(20.dp))
            .border(1.dp, colors.border, RoundedCornerShape(20.dp)).padding(5.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AppPage.entries.forEach { page ->
            val active = selected == page
            Column(
                Modifier.weight(1f).background(if (active) colors.accent.copy(alpha = .11f) else Color.Transparent, RoundedCornerShape(15.dp))
                    .clickable { onSelect(page) }.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(page.icon, color = if (active) colors.accent else colors.muted, fontSize = 17.sp)
                Text(page.label, color = if (active) colors.accent else colors.muted, fontSize = 10.sp)
            }
        }
    }
}
