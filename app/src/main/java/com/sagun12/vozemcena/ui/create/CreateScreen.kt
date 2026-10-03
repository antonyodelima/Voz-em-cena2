package com.sagun12.vozemcena.ui.create

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sagun12.vozemcena.domain.model.AspectRatio
import com.sagun12.vozemcena.domain.model.DateLabels
import com.sagun12.vozemcena.domain.model.ProjectStatus
import com.sagun12.vozemcena.domain.model.TransitionType
import com.sagun12.vozemcena.ui.components.AppTopBar
import com.sagun12.vozemcena.ui.components.AudioWaveformVisualizer
import com.sagun12.vozemcena.ui.components.ConfirmDeleteDialog
import com.sagun12.vozemcena.ui.components.ExportProgressDialog
import com.sagun12.vozemcena.ui.theme.CyanSecondary
import com.sagun12.vozemcena.ui.theme.GoldPrimary
import com.sagun12.vozemcena.ui.theme.StatusError
import com.sagun12.vozemcena.video.MediaStoreExporter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    viewModel: CreateViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()
    val project = uiState.project

    val snackbarHostState = remember { SnackbarHostState() }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var tempTitle by remember { mutableStateOf("") }
    var showDeleteSceneDialog by remember { mutableStateOf<String?>(null) }
    var voiceDropdownExpanded by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberMediaPickerLauncher { uri ->
        val savedPath = MediaImportHelper.copyUriToAppStorage(context, uri)
        if (savedPath != null) {
            val selectedScene = project?.scenes?.getOrNull(uiState.selectedSceneIndex)
            if (selectedScene != null) {
                viewModel.updateScene(selectedScene.copy(mediaUri = savedPath))
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = project?.title ?: "Novo Projeto",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(onClick = {
                        tempTitle = project?.title ?: ""
                        showEditTitleDialog = true
                    }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar Título")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (project == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Carregando projeto...")
            }
            return@Scaffold
        }

        val scenes = project.scenes
        val currentSceneIndex = uiState.selectedSceneIndex.coerceIn(0, (scenes.size - 1).coerceAtLeast(0))
        val currentScene = scenes.getOrNull(currentSceneIndex)

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Timeline Scenes Carousel (Horizontal)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cenas (${scenes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Duração Total: ${DateLabels.formatDuration(project.totalDurationSec)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldPrimary
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(scenes) { index, scene ->
                    val isSelected = index == currentSceneIndex
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) GoldPrimary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .width(110.dp)
                            .height(84.dp)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) GoldPrimary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectScene(index) }
                            .testTag("scene_tab_$index")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cena ${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${scene.durationSec.toInt()}s",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = scene.textScript.ifBlank { "Sem roteiro" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                item {
                    // Add Scene Button
                    Surface(
                        onClick = { viewModel.addScene() },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .width(60.dp)
                            .height(84.dp)
                            .testTag("add_scene_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Adicionar Cena",
                                tint = GoldPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Aspect Ratio & Format Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AspectRatio.entries.forEach { ratio ->
                    val isSelected = project.aspectRatio == ratio
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateAspectRatio(ratio) },
                        label = { Text(ratio.label.split(" ").first()) },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            } else {
                                Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Visual Stage / Preview Card
            if (currentScene != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Media Display Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentScene.mediaUri != null) {
                                AsyncImage(
                                    model = currentScene.mediaUri,
                                    contentDescription = "Mídia da cena",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = GoldPrimary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Nenhuma imagem selecionada",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Subtitle preview overlay
                            if (project.includeSubtitles && currentScene.textScript.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = currentScene.textScript,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Scene Tag Badge
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Cena ${currentSceneIndex + 1}/${scenes.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Media action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("pick_image_button")
                            ) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Escolher Imagem", style = MaterialTheme.typography.labelMedium)
                            }

                            IconButton(
                                onClick = { showDeleteSceneDialog = currentScene.id },
                                modifier = Modifier.testTag("delete_scene_button")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir Cena", tint = StatusError)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Narration & Voice Controls
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Roteiro e Narração (Cena ${currentSceneIndex + 1})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Script text field
                        OutlinedTextField(
                            value = currentScene.textScript,
                            onValueChange = { newScript ->
                                viewModel.updateScene(currentScene.copy(textScript = newScript))
                            },
                            placeholder = { Text("Digite o que a voz neural deve falar nesta cena...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("scene_script_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Voice Selector
                        ExposedDropdownMenuBox(
                            expanded = voiceDropdownExpanded,
                            onExpandedChange = { voiceDropdownExpanded = !voiceDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = currentScene.voiceName ?: "Helena (Narradora)",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Voz Selecionada") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceDropdownExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = voiceDropdownExpanded,
                                onDismissRequest = { voiceDropdownExpanded = false }
                            ) {
                                availableVoices.forEach { voice ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(voice.name, fontWeight = FontWeight.Bold)
                                                Text(voice.description, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                            }
                                        },
                                        onClick = {
                                            viewModel.updateScene(
                                                currentScene.copy(
                                                    voiceId = voice.id,
                                                    voiceName = voice.name
                                                )
                                            )
                                            voiceDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Voice Preview Button
                        val isPlayingThis = uiState.playingSceneId == currentScene.id
                        Button(
                            onClick = { viewModel.previewSceneVoice(currentScene) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlayingThis) StatusError else CyanSecondary.copy(alpha = 0.2f),
                                contentColor = if (isPlayingThis) Color.White else CyanSecondary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("preview_scene_voice_button")
                        ) {
                            Icon(
                                imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isPlayingThis) "Parar Prévia" else "Ouvir Narração com IA",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Duration Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Duração da Cena", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${currentScene.durationSec.toInt()} segundos",
                                style = MaterialTheme.typography.labelLarge,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = currentScene.durationSec,
                            onValueChange = { newDuration ->
                                viewModel.updateScene(currentScene.copy(durationSec = newDuration))
                            },
                            valueRange = 2f..20f,
                            steps = 17,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldPrimary,
                                activeTrackColor = GoldPrimary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Main Action Export Button
            Button(
                onClick = { viewModel.exportVideo() },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp)
                    .testTag("export_video_button")
            ) {
                Icon(imageVector = Icons.Default.Movie, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Renderizar e Exportar Vídeo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Dialogs
    if (showEditTitleDialog) {
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            title = { Text("Nome do Projeto") },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateProjectTitle(tempTitle)
                    showEditTitleDialog = false
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    showDeleteSceneDialog?.let { sceneId ->
        ConfirmDeleteDialog(
            title = "Excluir Cena",
            message = "Tem certeza que deseja excluir esta cena do projeto?",
            onConfirm = { viewModel.deleteScene(sceneId) },
            onDismiss = { showDeleteSceneDialog = null }
        )
    }

    uiState.exportProgress?.let { progress ->
        ExportProgressDialog(
            exportProgress = progress,
            onDismiss = { viewModel.dismissExportDialog() }
        )
    }
}
