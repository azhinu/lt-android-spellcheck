package azhinu.languagetool.android

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Launch
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import azhinu.languagetool.android.api.EndpointValidator
import azhinu.languagetool.android.api.LanguageToolClient
import azhinu.languagetool.android.dictionary.DictionaryCodec
import azhinu.languagetool.android.dictionary.SystemDictionaryRepository
import azhinu.languagetool.android.logging.LogEntry
import azhinu.languagetool.android.logging.LogLevel
import azhinu.languagetool.android.logging.RuntimeLog
import azhinu.languagetool.android.model.LanguageToolMatch
import azhinu.languagetool.android.model.LanguageToolSettings
import azhinu.languagetool.android.model.SupportedLanguage
import azhinu.languagetool.android.model.SupportedLanguages
import azhinu.languagetool.android.settings.SettingsRepository
import azhinu.languagetool.android.text.TextCorrection
import azhinu.languagetool.android.ui.LanguageToolTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LanguageToolTheme {
                LanguageToolApp(openSpellCheckerSettings = ::openSpellCheckerSettings)
            }
        }
    }

    private fun openSpellCheckerSettings() {
        val directIntent = Intent().apply {
            component = ComponentName(
                "com.android.settings",
                "com.android.settings.Settings\$SpellCheckersSettingsActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(directIntent) }
            .onFailure { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }
}

private enum class AppScreen(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    PLAYGROUND("Check", Icons.Rounded.EditNote),
    DICTIONARY("Dictionary", Icons.Rounded.MenuBook),
    LOGS("Logs", Icons.Rounded.BugReport)
}

private data class OperationStatus(val message: String, val succeeded: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageToolApp(openSpellCheckerSettings: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val systemDictionary = remember { SystemDictionaryRepository(context) }
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf(repository.load()) }
    var spellCheckerSelected by remember { mutableStateOf(isCurrentSpellChecker(context)) }
    var screen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var dictionaryMenuExpanded by remember { mutableStateOf(false) }
    var dictionaryStatus by remember { mutableStateOf<OperationStatus?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        settings = repository.load()
        spellCheckerSelected = isCurrentSpellChecker(context)
    }

    fun update(newSettings: LanguageToolSettings) {
        settings = newSettings
        repository.save(newSettings)
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                            DictionaryCodec.decode(reader.readText())
                        } ?: error("The selected file could not be opened")
                    }
                }.fold(
                    onSuccess = { imported ->
                        systemDictionary.addWords(imported)
                        update(settings.copy(dictionary = settings.dictionary + imported))
                        dictionaryStatus = OperationStatus("Imported ${imported.size} words", true)
                    },
                    onFailure = {
                        dictionaryStatus = OperationStatus("Import failed: ${it.message.orEmpty()}", false)
                    }
                )
            }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                            writer.write(DictionaryCodec.encode(settings.dictionary))
                        } ?: error("The destination file could not be opened")
                    }
                }.fold(
                    onSuccess = {
                        dictionaryStatus = OperationStatus("Exported ${settings.dictionary.size} words", true)
                    },
                    onFailure = {
                        dictionaryStatus = OperationStatus("Export failed: ${it.message.orEmpty()}", false)
                    }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(screen.title, fontWeight = FontWeight.SemiBold)
                        Text(
                            "LanguageTool · Android",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (screen == AppScreen.DICTIONARY) {
                        Box {
                            IconButton(
                                onClick = { dictionaryMenuExpanded = true },
                                modifier = Modifier.testTag("dictionary_actions")
                            ) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Dictionary actions")
                            }
                            DropdownMenu(
                                expanded = dictionaryMenuExpanded,
                                onDismissRequest = { dictionaryMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Import") },
                                    leadingIcon = { Icon(Icons.Rounded.Download, null) },
                                    onClick = {
                                        dictionaryMenuExpanded = false
                                        importLauncher.launch(arrayOf("text/plain", "text/*"))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export") },
                                    leadingIcon = { Icon(Icons.Rounded.Upload, null) },
                                    enabled = settings.dictionary.isNotEmpty(),
                                    onClick = {
                                        dictionaryMenuExpanded = false
                                        exportLauncher.launch("languagetool-dictionary.txt")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear dictionary") },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                    enabled = settings.dictionary.isNotEmpty(),
                                    onClick = {
                                        dictionaryMenuExpanded = false
                                        systemDictionary.removeWords(settings.dictionary)
                                        update(settings.copy(dictionary = emptySet()))
                                        dictionaryStatus = OperationStatus("Dictionary cleared", true)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AppScreen.entries.forEach { item ->
                    NavigationBarItem(
                        selected = screen == item,
                        onClick = { screen = item },
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = screen,
            label = "screen",
            modifier = Modifier.padding(padding).fillMaxSize()
        ) { current ->
            when (current) {
                AppScreen.HOME -> HomeScreen(settings, ::update, spellCheckerSelected, openSpellCheckerSettings)
                AppScreen.PLAYGROUND -> PlaygroundScreen(settings, ::update)
                AppScreen.DICTIONARY -> DictionaryScreen(settings, ::update, dictionaryStatus)
                AppScreen.LOGS -> LogsScreen()
            }
        }
    }
}

@Composable
private fun HomeScreen(
    settings: LanguageToolSettings,
    update: (LanguageToolSettings) -> Unit,
    spellCheckerSelected: Boolean,
    openSpellCheckerSettings: () -> Unit
) {
    var endpoint by remember(settings.endpoint) { mutableStateOf(settings.endpoint) }
    var endpointError by remember { mutableStateOf<String?>(null) }
    var testStatus by remember { mutableStateOf<String?>(null) }
    var testSucceeded by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var languageDialog by remember { mutableStateOf(false) }
    var motherTongueDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun verifyEndpoint(saveAfterVerification: Boolean) {
        val normalized = EndpointValidator.normalize(endpoint).getOrElse {
            endpointError = it.message
            testSucceeded = false
            return
        }
        testing = true
        endpointError = null
        testSucceeded = false
        testStatus = "Connecting…"
        val candidate = settings.copy(endpoint = normalized)
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { LanguageToolClient().test(candidate) }
            }
            testing = false
            result.fold(
                onSuccess = {
                    endpoint = normalized
                    if (saveAfterVerification) update(candidate)
                    testSucceeded = true
                    testStatus = if (saveAfterVerification) {
                        "Server verified and saved · language ${it.detectedLanguage ?: "auto"}"
                    } else {
                        "Server is reachable · language ${it.detectedLanguage ?: "auto"}"
                    }
                },
                onFailure = {
                    testSucceeded = false
                    testStatus = it.message ?: "Connection failed"
                }
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (spellCheckerSelected) {
                ElevatedCard {
                    ListItem(
                        headlineContent = { Text("System spell checker is active") },
                        supportingContent = { Text("LanguageTool is selected for Android and Gboard.") },
                        leadingContent = {
                            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    )
                }
            } else {
                HeroCard(
                    title = "System spell checker",
                    body = "Enable LanguageTool in Android settings and select it for Gboard.",
                    icon = Icons.Rounded.Spellcheck,
                    action = {
                        Button(onClick = openSpellCheckerSettings) {
                            Icon(Icons.Rounded.Launch, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open settings")
                        }
                    }
                )
            }
        }

        item { SectionTitle("Server") }
        item {
            ElevatedCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = {
                            endpoint = it
                            endpointError = null
                            testStatus = null
                            testSucceeded = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("endpoint"),
                        label = { Text("Endpoint") },
                        placeholder = { Text("https://server.example") },
                        supportingText = {
                            Text(endpointError ?: "The /v2/check path is added automatically")
                        },
                        isError = endpointError != null,
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            enabled = !testing,
                            modifier = Modifier.testTag("save_endpoint"),
                            onClick = { verifyEndpoint(saveAfterVerification = true) }
                        ) {
                            Icon(Icons.Rounded.Save, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Save")
                        }
                        OutlinedButton(
                            enabled = !testing,
                            onClick = { verifyEndpoint(saveAfterVerification = false) }
                        ) {
                            Icon(Icons.Rounded.Refresh, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Test")
                        }
                    }
                    testStatus?.let { StatusText(it, testSucceeded) }
                }
            }
        }

        item { SectionTitle("Languages") }
        item {
            ElevatedCard {
                Column {
                    ListItem(
                        headlineContent = { Text("Preferred languages") },
                        supportingContent = {
                            Text(languageNames(settings.preferredLanguages).ifBlank { "None selected" })
                        },
                        leadingContent = { Icon(Icons.Rounded.Language, null) },
                        modifier = Modifier.fillMaxWidth().testTag("preferred_languages")
                            .clickable { languageDialog = true }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Limit language detection") },
                        supportingContent = { Text("Only check the selected languages") },
                        trailingContent = {
                            Switch(
                                checked = settings.forcePreferredLanguages,
                                enabled = settings.preferredLanguages.isNotEmpty(),
                                onCheckedChange = { update(settings.copy(forcePreferredLanguages = it)) }
                            )
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Mother tongue") },
                        supportingContent = {
                            Text(languageName(settings.motherTongue) ?: "Not specified")
                        },
                        modifier = Modifier.fillMaxWidth().clickable { motherTongueDialog = true }
                    )
                }
            }
        }

        item { SectionTitle("Checking") }
        item {
            ElevatedCard {
                ListItem(
                    headlineContent = { Text("Picky mode") },
                    supportingContent = { Text("Punctuation, typography, and additional rules") },
                    trailingContent = {
                        Switch(
                            checked = settings.pickyMode,
                            onCheckedChange = { update(settings.copy(pickyMode = it)) }
                        )
                    }
                )
            }
        }

        if (settings.preferredLanguages.any { it in SupportedLanguages.variants }) {
            item { SectionTitle("Language variants") }
            items(settings.preferredLanguages.filter { it in SupportedLanguages.variants }.sorted()) { language ->
                VariantCard(language, settings, update)
            }
        }
    }

    if (languageDialog) {
        MultiLanguageDialog(
            selected = settings.preferredLanguages,
            onDismiss = { languageDialog = false },
            onConfirm = {
                update(settings.copy(preferredLanguages = it))
                languageDialog = false
            }
        )
    }
    if (motherTongueDialog) {
        SingleLanguageDialog(
            selected = settings.motherTongue,
            onDismiss = { motherTongueDialog = false },
            onConfirm = {
                update(settings.copy(motherTongue = it))
                motherTongueDialog = false
            }
        )
    }
}

@Composable
private fun PlaygroundScreen(
    settings: LanguageToolSettings,
    update: (LanguageToolSettings) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val systemDictionary = remember { SystemDictionaryRepository(context) }
    var text by rememberSaveable { mutableStateOf("") }
    var matches by remember { mutableStateOf<List<LanguageToolMatch>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var hiddenMatches by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                label = { Text("Text to check") },
                placeholder = { Text("Type something suspicious…") }
            )
        }
        item {
            Button(
                enabled = text.isNotBlank() && !checking,
                onClick = {
                    checking = true
                    status = "Checking…"
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) { LanguageToolClient().check(text, settings) }
                        }.fold(
                            onSuccess = {
                                matches = it.matches
                                hiddenMatches = emptySet()
                                status = "Found: ${it.matches.size} · language ${it.detectedLanguage ?: "auto"}"
                            },
                            onFailure = { status = it.message ?: "Check failed" }
                        )
                        checking = false
                    }
                }
            ) {
                Icon(Icons.Rounded.Spellcheck, null)
                Spacer(Modifier.width(8.dp))
                Text("Check")
            }
        }
        status?.let { item { StatusText(it, matches.isEmpty() && it.startsWith("Found")) } }
        val visibleMatches = matches.filterNot { match -> match.identity() in hiddenMatches }
        items(visibleMatches, key = { it.identity() }) { match ->
            MatchCard(
                match = match,
                sourceText = text,
                onApplyReplacement = { replacement ->
                    text = TextCorrection.applyReplacement(text, match, replacement)
                    matches = emptyList()
                    hiddenMatches = emptySet()
                    status = "Correction applied · run the check again"
                },
                onAddWord = { word ->
                    systemDictionary.addWords(listOf(word))
                    update(settings.copy(dictionary = settings.dictionary + word))
                    matches = matches - match
                },
                onHideMatch = {
                    hiddenMatches = hiddenMatches + match.identity()
                    status = "Match hidden until the next check"
                },
                onIgnoreRule = {
                    update(settings.copy(ignoredRuleIds = settings.ignoredRuleIds + match.ruleId))
                    matches = matches.filterNot { it.ruleId == match.ruleId }
                }
            )
        }
    }
}

@Composable
private fun MatchCard(
    match: LanguageToolMatch,
    sourceText: String,
    onApplyReplacement: (String) -> Unit,
    onAddWord: (String) -> Unit,
    onHideMatch: () -> Unit,
    onIgnoreRule: () -> Unit
) {
    val end = (match.offset + match.length).coerceAtMost(sourceText.length)
    val excerpt = if (match.offset in 0..end) sourceText.substring(match.offset, end) else ""
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (match.isSpelling) Icons.Rounded.Spellcheck else Icons.Rounded.Warning,
                    null,
                    tint = if (match.isSpelling) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.width(10.dp))
                Text(excerpt.ifBlank { match.ruleId }, fontWeight = FontWeight.Bold)
            }
            Text(match.message)
            if (match.replacements.isNotEmpty()) {
                Text("Suggestions", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    match.replacements.take(3).forEach { replacement ->
                        AssistChip(
                            onClick = { onApplyReplacement(replacement) },
                            label = { Text(replacement, maxLines = 1) }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (match.isSpelling && excerpt.isNotBlank()) {
                    FilledTonalButton(onClick = { onAddWord(excerpt) }) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add to dictionary")
                    }
                }
                TextButton(onClick = onHideMatch) {
                    Icon(Icons.Rounded.Close, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Hide")
                }
            }
            Row {
                TextButton(onClick = onIgnoreRule, enabled = match.ruleId.isNotBlank()) {
                    Text("Disable this rule everywhere")
                }
            }
            Text(match.ruleId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun LanguageToolMatch.identity(): String = "$ruleId:$offset:$length:$message"

@Composable
private fun DictionaryScreen(
    settings: LanguageToolSettings,
    update: (LanguageToolSettings) -> Unit,
    operationStatus: OperationStatus?
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val systemDictionary = remember { SystemDictionaryRepository(context) }
    var word by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Personal dictionary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Words here are ignored only for spelling checks.")
                    OutlinedTextField(
                        value = word,
                        onValueChange = { word = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("New word") },
                        singleLine = true
                    )
                    Button(
                        enabled = word.trim().isNotEmpty(),
                        onClick = {
                            val newWord = word.trim()
                            systemDictionary.addWords(listOf(newWord))
                            update(settings.copy(dictionary = settings.dictionary + newWord))
                            word = ""
                        }
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add")
                    }
                    operationStatus?.let { StatusText(it.message, it.succeeded) }
                }
            }
        }
        if (settings.dictionary.isEmpty()) {
            item { EmptyState("The dictionary is empty", "Words added here or through Android will appear here.") }
        } else {
            items(settings.dictionary.sortedWith(String.CASE_INSENSITIVE_ORDER)) { item ->
                ElevatedCard {
                    ListItem(
                        headlineContent = { Text(item) },
                        trailingContent = {
                            IconButton(onClick = {
                                systemDictionary.removeWords(listOf(item))
                                update(settings.copy(dictionary = settings.dictionary - item))
                            }) {
                                Icon(Icons.Rounded.Delete, "Delete")
                            }
                        }
                    )
                }
            }
        }
        if (settings.ignoredRuleIds.isNotEmpty()) {
            item { SectionTitle("Ignored rules") }
            items(settings.ignoredRuleIds.sorted()) { rule ->
                AssistChip(
                    onClick = { update(settings.copy(ignoredRuleIds = settings.ignoredRuleIds - rule)) },
                    label = { Text(rule) },
                    trailingIcon = { Icon(Icons.Rounded.Delete, "Restore rule", Modifier.size(18.dp)) }
                )
            }
        }
    }
}

@Composable
private fun LogsScreen() {
    val entries by RuntimeLog.entries.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Current run only", style = MaterialTheme.typography.titleMedium)
                    Text("Up to 500 recent events", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = RuntimeLog::clear, enabled = entries.isNotEmpty()) {
                    Icon(Icons.Rounded.Delete, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Clear")
                }
            }
        }
        if (entries.isEmpty()) {
            item { EmptyState("The log is empty", "Test the endpoint or check some text.") }
        } else {
            items(entries.asReversed()) { entry -> LogCard(entry) }
        }
    }
}

@Composable
private fun LogCard(entry: LogEntry) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault()) }
    val color = when (entry.level) {
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
        LogLevel.WARN -> MaterialTheme.colorScheme.tertiary
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.DEBUG -> MaterialTheme.colorScheme.outline
    }
    ElevatedCard {
        SelectionContainer {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.level.name, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(formatter.format(entry.timestamp), style = MaterialTheme.typography.labelSmall)
                }
                Text(entry.message)
                entry.details?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun VariantCard(
    language: String,
    settings: LanguageToolSettings,
    update: (LanguageToolSettings) -> Unit
) {
    var dialog by remember { mutableStateOf(false) }
    val current = settings.preferredVariants[language] ?: SupportedLanguages.defaultVariants.getValue(language)
    ElevatedCard(onClick = { dialog = true }) {
        ListItem(
            headlineContent = { Text("${languageName(language)}") },
            supportingContent = { Text(current) },
            leadingContent = { Icon(Icons.Rounded.Language, null) }
        )
    }
    if (dialog) {
        ChoiceDialog(
            title = "Variant: ${languageName(language)}",
            choices = SupportedLanguages.variants.getValue(language),
            selected = current,
            onDismiss = { dialog = false },
            onSelect = { variant ->
                update(settings.copy(preferredVariants = settings.preferredVariants + (language to variant)))
                dialog = false
            }
        )
    }
}

@Composable
private fun MultiLanguageDialog(
    selected: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var draft by remember(selected) { mutableStateOf(selected) }
    var query by rememberSaveable { mutableStateOf("") }
    val visibleLanguages = SupportedLanguages.all.filter {
        it.name.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Preferred languages") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().testTag("preferred_language_search"),
                    label = { Text("Search languages") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.height(390.dp)) {
                    items(visibleLanguages, key = SupportedLanguage::code) { language ->
                        val checked = language.code in draft
                        ListItem(
                            headlineContent = { Text(language.name) },
                            supportingContent = { Text(language.code) },
                            trailingContent = {
                                Switch(
                                    checked = checked,
                                    onCheckedChange = { value ->
                                        draft = if (value) draft + language.code else draft - language.code
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                draft = if (checked) draft - language.code else draft + language.code
                            }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(draft) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SingleLanguageDialog(
    selected: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleLanguages = SupportedLanguages.all.filter {
        it.name.contains(query, ignoreCase = true) || it.code.contains(query, ignoreCase = true)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mother tongue") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().testTag("mother_tongue_search"),
                    label = { Text("Search languages") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.height(390.dp)) {
                    if (query.isBlank()) {
                        item {
                            ListItem(
                                headlineContent = { Text("Not specified") },
                                trailingContent = {
                                    if (selected == null) Icon(Icons.Rounded.CheckCircle, null)
                                },
                                modifier = Modifier.fillMaxWidth().clickable { onConfirm(null) }
                            )
                        }
                    }
                    items(visibleLanguages, key = SupportedLanguage::code) { language ->
                        ListItem(
                            headlineContent = { Text(language.name) },
                            supportingContent = { Text(language.code) },
                            trailingContent = {
                                if (selected == language.code) Icon(Icons.Rounded.CheckCircle, null)
                            },
                            modifier = Modifier.fillMaxWidth().clickable { onConfirm(language.code) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun ChoiceDialog(
    title: String,
    choices: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                choices.forEach { choice ->
                    TextButton(onClick = { onSelect(choice) }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (choice == selected) "✓  $choice" else choice, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun HeroCard(title: String, body: String, icon: ImageVector, action: @Composable () -> Unit) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onPrimaryContainer)
            action()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun StatusText(text: String, success: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (success) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            null,
            tint = if (success) Color(0xFF28834F) else MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun languageName(code: String?): String? = SupportedLanguages.all.firstOrNull { it.code == code }?.name
private fun languageNames(codes: Set<String>): String = SupportedLanguages.all.filter { it.code in codes }.joinToString(", ") { it.name }

private fun isCurrentSpellChecker(context: Context): Boolean {
    val selected = Settings.Secure.getString(context.contentResolver, "selected_spell_checker")
    return ComponentName.unflattenFromString(selected.orEmpty())?.packageName == context.packageName
}
