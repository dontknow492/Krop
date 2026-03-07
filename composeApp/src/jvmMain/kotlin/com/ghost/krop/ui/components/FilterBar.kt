package com.ghost.krop.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ghost.krop.models.DirectorySettings
import com.ghost.krop.viewModel.ImageSort
import com.ghost.krop.viewModel.SortDirection
import kotlinx.coroutines.launch
import java.nio.file.Path

@Composable
fun FilterBar(
    searchQuery: String,
    onSearch: (String) -> Unit,
    viewMode: ImageCardType,
    onViewModeChange: (ImageCardType) -> Unit,
    sortBy: ImageSort,
    onImageSortChange: (ImageSort) -> Unit,
    sortOrder: SortDirection,
    onSortDirectionChange: (SortDirection) -> Unit,
    currentDir: Path?,
    directorySettings: DirectorySettings,
    onDirectorySettingChange: (DirectorySettings) -> Unit,
    onClear: () -> Unit,
    onRefresh: () -> Unit,
    onFolderClick: () -> Unit,
    onLoadDirectory: (Path) -> Unit,
    modifier: Modifier = Modifier
) {

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        val isCompact = maxWidth < 600.dp

        if (isCompact) {

            // 👇 Small screen layout
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                ModernSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearch,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    SortMenuButton(
                        currentImageSort = sortBy,
                        currentSortDirection = sortOrder,
                        onImageSortChange = onImageSortChange,
                        onSortDirectionChange = onSortDirectionChange
                    )

                    ViewModeMenu(
                        viewMode = viewMode,
                        onModeChange = onViewModeChange
                    )
                    VerticalDivider(modifier = Modifier.height(24.dp))
                    DirectorySection(
                        currentDir = currentDir,
                        currentSettings = directorySettings,
                        onClear = onClear,
                        onRefresh = onRefresh,
                        onFolderClick = onFolderClick,
                        onLoadDirectory = onLoadDirectory,
                        onUpdateSettings = onDirectorySettingChange
                    )
                }
            }

        } else {

            // 👇 Large screen layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                ModernSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearch,
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                SortMenuButton(
                    currentImageSort = sortBy,
                    currentSortDirection = sortOrder,
                    onImageSortChange = onImageSortChange,
                    onSortDirectionChange = onSortDirectionChange
                )

                ViewModeMenu(
                    viewMode = viewMode,
                    onModeChange = onViewModeChange
                )
                VerticalDivider(modifier = Modifier.height(24.dp))
                DirectorySection(
                    currentDir = currentDir,
                    currentSettings = directorySettings,
                    onClear = onClear,
                    onRefresh = onRefresh,
                    onFolderClick = onFolderClick,
                    onLoadDirectory = onLoadDirectory,
                    onUpdateSettings = onDirectorySettingChange
                )
            }
        }
    }
}

@Composable
fun DirectorySection(
    modifier: Modifier = Modifier,
    currentDir: Path?,
    // Use the simplified data class
    currentSettings: DirectorySettings = DirectorySettings(),
    onClear: () -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean = false,
    onFolderClick: () -> Unit,
    onLoadDirectory: (Path) -> Unit,
    onUpdateSettings: (DirectorySettings) -> Unit
) {
    var isFileDialogOpened by remember { mutableStateOf(false) }
    var isSettingsDialogOpened by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val rotationAngle by animateFloatAsState(
        targetValue = if (isFileDialogOpened) 360f else 0f,
    )

    val openDir = rememberDirectoryPicker(title = "Select Folder") { file ->
        if (file != null) {
            onLoadDirectory(file.toPath())
            isFileDialogOpened = false
        }
    }

    if (isSettingsDialogOpened) {
        DirectorySettingsDialog(
            currentDir = currentDir,
            settings = currentSettings,
            onDismiss = { isSettingsDialogOpened = false },
            onApply = onUpdateSettings,
            onFolderClick = onFolderClick,
        )
    }

    // UI Layout remains the same as before...
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Settings Button
        if (currentDir != null) {
            FilledTonalIconButton(
                onClick = onRefresh,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    "Refresh",
                    modifier = Modifier.rotate(rotationAngle)
                )

            }
        }

        FilledTonalIconButton(
            onClick = { isSettingsDialogOpened = true },
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Tune, contentDescription = "Folder Options")
        }


        // Open Button
        Button(
            onClick = { scope.launch { isFileDialogOpened = true; openDir() } },
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open Folder", maxLines = 1)
        }

        // Clear Button (only if directory loaded)
        if (currentDir != null) {
            OutlinedIconButton(
                onClick = onClear,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Rounded.DeleteSweep, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}


@Composable
fun DirectorySettingsDialog(
    currentDir: Path?,
    settings: DirectorySettings,
    onDismiss: () -> Unit,
    onFolderClick: () -> Unit,
    onApply: (DirectorySettings) -> Unit
) {
    var tempSettings by remember { mutableStateOf(settings) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(380.dp),
            shape = RoundedCornerShape(28.dp), // Extra rounded for modern feel
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // --- Header ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Using a tonal container for the icon makes it pop
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.FolderSpecial,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Folder Options",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // --- 1. Current Path Indicator ---
                // Only show if a directory is actually selected
                if (currentDir != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                        Text(
                            text = "SELECTED PATH",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PathSelector(
                                label = null,
                                path = currentDir,
                                onClick = onFolderClick,
                                onFolderClick = onFolderClick,
                            )

                        }
                    }
                }

                // --- 2. Recursion Logic (The Main Logic) ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Toggle Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Recursive Scan", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Look inside subfolders",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = tempSettings.isRecursive,
                            onCheckedChange = { tempSettings = tempSettings.copy(isRecursive = it) }
                        )
                    }

                    // Depth Slider (Animated)
                    AnimatedVisibility(
                        visible = tempSettings.isRecursive,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Max Depth",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = "${tempSettings.maxDepth}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Slider(
                                value = tempSettings.maxDepth.toFloat(),
                                onValueChange = { tempSettings = tempSettings.copy(maxDepth = it.toInt()) },
                                valueRange = 1f..10f,
                                steps = 9
                            )
                        }
                    }
                }

                // --- 3. Hidden Files ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = tempSettings.includeHiddenFiles,
                        onCheckedChange = { tempSettings = tempSettings.copy(includeHiddenFiles = it) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Include hidden folders (.git, .temp)", style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // --- Actions ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onApply(tempSettings); onDismiss() }) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewModeMenu(
    viewMode: ImageCardType,
    onModeChange: (ImageCardType) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        // 1. Grid Mode
        SegmentedButton(
            selected = viewMode == ImageCardType.POSTER,
            onClick = { onModeChange(ImageCardType.POSTER) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            // We pass empty lambda to 'icon' to hide the checkmark
            // because for view toggles, the color change is enough context.
            icon = {}
        ) {
            Icon(
                imageVector = Icons.Rounded.GridView,
                contentDescription = "Grid View",
                modifier = Modifier.size(20.dp)
            )
        }

        // 2. Landscape Mode
        SegmentedButton(
            selected = viewMode == ImageCardType.LIST,
            onClick = { onModeChange(ImageCardType.LIST) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = {}
        ) {
            Icon(
                // AutoMirrored handles RTL layouts automatically
                imageVector = Icons.AutoMirrored.Rounded.List,
                contentDescription = "Landscape View",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SortMenuButton(
    currentImageSort: ImageSort,
    currentSortDirection: SortDirection,
    onImageSortChange: (ImageSort) -> Unit,
    onSortDirectionChange: (SortDirection) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        // The Trigger Button
        FilledTonalIconButton(
            onClick = { expanded = true },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort Options")
        }

        // The Dropdown Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(200.dp).background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            // Section 1: Sort Criteria
            DropdownMenuItem(
                text = {
                    Text(
                        "Date Modified",
                        fontWeight = if (currentImageSort == ImageSort.DATE) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onImageSortChange(ImageSort.DATE); expanded = false },
                leadingIcon = { Icon(Icons.Rounded.CalendarToday, null) },
                trailingIcon = { if (currentImageSort == ImageSort.DATE) Icon(Icons.Rounded.Check, null) }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "File Name",
                        fontWeight = if (currentImageSort == ImageSort.NAME) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onImageSortChange(ImageSort.NAME); expanded = false },
                leadingIcon = { Icon(Icons.Rounded.Abc, null) },
                trailingIcon = { if (currentImageSort == ImageSort.NAME) Icon(Icons.Rounded.Check, null) }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "File Size",
                        fontWeight = if (currentImageSort == ImageSort.SIZE) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onImageSortChange(ImageSort.SIZE); expanded = false },
                leadingIcon = { Icon(Icons.Rounded.SdStorage, null) },
                trailingIcon = { if (currentImageSort == ImageSort.SIZE) Icon(Icons.Rounded.Check, null) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Section 2: Order
            DropdownMenuItem(
                text = {
                    Text(
                        "Ascending",
                        fontWeight = if (currentSortDirection == SortDirection.ASCENDING) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onSortDirectionChange(SortDirection.ASCENDING); expanded = false },
                leadingIcon = { Icon(Icons.Rounded.ArrowUpward, null) },
                trailingIcon = { if (currentSortDirection == SortDirection.ASCENDING) Icon(Icons.Rounded.Check, null) }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        "Descending",
                        fontWeight = if (currentSortDirection == SortDirection.DESCENDING) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onSortDirectionChange(SortDirection.DESCENDING); expanded = false },
                leadingIcon = { Icon(Icons.Rounded.ArrowDownward, null) },
                trailingIcon = { if (currentSortDirection == SortDirection.DESCENDING) Icon(Icons.Rounded.Check, null) }
            )
        }
    }
}