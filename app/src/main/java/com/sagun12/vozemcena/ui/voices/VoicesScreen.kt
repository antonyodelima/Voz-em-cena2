package com.sagun12.vozemcena.ui.voices

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagun12.vozemcena.ui.components.AppTopBar
import com.sagun12.vozemcena.ui.components.AudioWaveformVisualizer
import com.sagun12.vozemcena.ui.components.VoiceCard
import com.sagun12.vozemcena.ui.theme.CyanSecondary
import com.sagun12.vozemcena.ui.theme.GoldPrimary
import com.sagun12.vozemcena.ui.theme.StatusError
import com.sagun12.vozemcena.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicesScreen(
    viewModel: VoicesViewModel,
    onNavigateToVoiceClone: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val voices by viewModel.filteredVoices.collectAsStateWithLifecycle()
    val isRecording by viewModel.audioRecorder.isRecording.collectAsStateWithLifecycle()
    val recordDuration by viewModel.audioRecorder.recordDurationSec.collectAsStateWithLifecycle()
    val amplitude by viewModel.audioRecorder.amplitude.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showRecordSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var customVoiceName by remember { mutableStateOf("") }
    var customVoiceDesc by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onNavigateToVoiceClone()
        }
    }

    LaunchedEffect(uiState.cloneSuccessMessage) {
        uiState.cloneSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedbackMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedbackMessages()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Estúdio de Vozes",
                actions = {}
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToVoiceClone,
                containerColor = GoldPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("record_new_voice_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Gravar Voz")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Criar Voz", fontWeight = FontWeight.Bold)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Buscar voz por nome, gênero ou estilo...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GoldPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("voice_search_input")
            )

            // Language & Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "ALL" to "Todas as Vozes",
                    "pt-BR" to "Português (Brasil)",
                    "en-US" to "English (US)"
                )
                items(filters) { (key, label) ->
                    val selected = uiState.selectedLanguageFilter == key
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.onLanguageFilterChange(key) },
                        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Voices List
            if (voices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nenhuma voz encontrada",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tente alterar os filtros de busca ou crie uma voz personalizada.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(voices, key = { it.id }) { voice ->
                        VoiceCard(
                            voice = voice,
                            isSelected = false,
                            isPlaying = uiState.playingVoiceId == voice.id,
                            onSelect = {
                                viewModel.playPreview(voice)
                            },
                            onPlayPreview = {
                                viewModel.playPreview(voice)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Recording / Creating Custom Voice
    if (showRecordSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!isRecording) {
                    showRecordSheet = false
                    customVoiceName = ""
                    customVoiceDesc = ""
                }
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Criar Perfil de Voz",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Grave uma amostra de 10 a 30 segundos com voz clara e sem ruídos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = customVoiceName,
                    onValueChange = { customVoiceName = it },
                    label = { Text("Nome da Voz (ex: Minha Voz Principal)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = customVoiceDesc,
                    onValueChange = { customVoiceDesc = it },
                    label = { Text("Descrição ou estilo (opcional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Record Button & Visualizer
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) StatusError else GoldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                viewModel.audioRecorder.stopRecording()
                            } else {
                                viewModel.startRecordingVoice()
                            }
                        },
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Parar" else "Gravar",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isRecording) {
                    Text(
                        text = "Gravando: ${String.format("%.1f", recordDuration)}s",
                        style = MaterialTheme.typography.titleMedium,
                        color = StatusError,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AudioWaveformVisualizer(isPlaying = true, color = StatusError)
                } else if (recordDuration > 0f) {
                    Text(
                        text = "Amostra gravada: ${String.format("%.1f", recordDuration)}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusSuccess,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (customVoiceName.isNotBlank() && recordDuration > 2.0f) {
                            viewModel.stopAndSaveRecordedVoice(customVoiceName, customVoiceDesc)
                            showRecordSheet = false
                            customVoiceName = ""
                            customVoiceDesc = ""
                        }
                    },
                    enabled = customVoiceName.isNotBlank() && recordDuration > 2.0f && !uiState.isCloning,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (uiState.isCloning) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processando Voz...")
                    } else {
                        Text("Salvar Voz", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
