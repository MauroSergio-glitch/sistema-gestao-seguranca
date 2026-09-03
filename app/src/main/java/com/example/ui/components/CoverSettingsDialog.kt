package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.SafetyGreenPrimary
import com.example.util.CoverConfig
import com.example.util.CoverImageManager
import com.example.util.CoverPreset
import com.example.util.CoverType
import java.io.File

@Composable
fun CoverSettingsDialog(
    currentConfig: CoverConfig,
    onDismiss: () -> Unit,
    onSelectPreset: (Int) -> Unit,
    onSelectCustomUri: (Uri) -> Unit,
    onResetToDefault: () -> Unit
) {
    val context = LocalContext.current
    var selectedPresetResId by remember { mutableStateOf(currentConfig.presetResId) }
    var selectedCustomUri by remember { mutableStateOf<Uri?>(null) }
    var isCustomMode by remember { mutableStateOf(currentConfig.type == CoverType.CUSTOM) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedCustomUri = uri
            isCustomMode = true
        }
    }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(SafetyGreenPrimary.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Wallpaper,
                    contentDescription = "Personalizar Capa",
                    tint = SafetyGreenPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "Imagem de Capa do Aplicativo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Escolha um dos modelos visuais inclusos ou carregue uma imagem personalizada da galeria:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Current Live Preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isCustomMode && selectedCustomUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(selectedCustomUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Pré-visualização da Capa Personalizada",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (isCustomMode && currentConfig.customFilePath != null && File(currentConfig.customFilePath).exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(currentConfig.customFilePath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Pré-visualização da Capa Personalizada",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(selectedPresetResId)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Pré-visualização da Capa Padrão",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Gradient overlay with label
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(10.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                text = if (isCustomMode) "Capa Personalizada (Galeria/Arquivo)" else "Tema Visual Selecionado",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Section: Built-in Presets
                Text(
                    text = "Modelos Visuais Inclusos no Aplicativo",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                CoverImageManager.AVAILABLE_PRESETS.forEach { preset ->
                    val isSelected = !isCustomMode && selectedPresetResId == preset.resId
                    PresetOptionCard(
                        preset = preset,
                        isSelected = isSelected,
                        onSelect = {
                            selectedPresetResId = preset.resId
                            isCustomMode = false
                        }
                    )
                }

                // Section: Custom gallery image
                Text(
                    text = "Carregar Foto Própria",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedButton(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_pick_custom_cover"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCustomMode && (selectedCustomUri != null || currentConfig.customFilePath != null)) {
                            "Alterar Imagem da Galeria"
                        } else {
                            "Selecionar Foto da Galeria"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Reset to Default button
                TextButton(
                    onClick = {
                        isCustomMode = false
                        selectedPresetResId = CoverImageManager.DEFAULT_PRESET_RES_ID
                        selectedCustomUri = null
                        onResetToDefault()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restaurar Capa Padrão Original", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isCustomMode && selectedCustomUri != null) {
                        onSelectCustomUri(selectedCustomUri!!)
                    } else if (!isCustomMode) {
                        onSelectPreset(selectedPresetResId)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SafetyGreenPrimary),
                modifier = Modifier.testTag("btn_confirm_cover_change")
            ) {
                Text("Salvar e Aplicar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun PresetOptionCard(
    preset: CoverPreset,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    val borderColor = if (isSelected) SafetyGreenPrimary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isSelected) {
        SafetyGreenPrimary.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Card(
                modifier = Modifier
                    .size(width = 64.dp, height = 48.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(preset.resId)
                        .crossfade(true)
                        .build(),
                    contentDescription = preset.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) SafetyGreenPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selecionado",
                    tint = SafetyGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
