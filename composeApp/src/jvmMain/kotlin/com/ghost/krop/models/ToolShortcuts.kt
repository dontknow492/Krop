package com.ghost.krop.models

import androidx.compose.ui.input.key.*
import com.ghost.krop.viewModel.CanvasEvent

sealed class UserAction {
    data class SwitchTool(val mode: CanvasMode) : UserAction()
    data object Undo : UserAction()
    data object Redo : UserAction()
    data object ZoomIn : UserAction()
    data object ZoomOut : UserAction()
    data object ResetZoom : UserAction()
    data object DeleteSelected : UserAction()
    data object CancelCurrent : UserAction() // For Esc key
    object NextImage : UserAction()
    object PreviousImage : UserAction()
}

/**
 * Registry for all shortcuts.
 * This can be moved to a DataStore or Database later if you want
 * users to customize their keys in a Settings menu.
 */
object ShortcutRegistry {
    val keybindings = mapOf(
        // Tool Switches
        Key.P to UserAction.SwitchTool(CanvasMode.Pan),
        Key.R to UserAction.SwitchTool(CanvasMode.Draw.Shape.Rectangle),
        Key.C to UserAction.SwitchTool(CanvasMode.Draw.Shape.Circle),
        Key.O to UserAction.SwitchTool(CanvasMode.Draw.Shape.Oval),
        Key.G to UserAction.SwitchTool(CanvasMode.Draw.Path.Polygon),
        Key.L to UserAction.SwitchTool(CanvasMode.Draw.Path.Line),

        // Zoom & View
        Key.Equals to UserAction.ZoomIn,  // Often same as Plus
        Key.Minus to UserAction.ZoomOut,
        Key.Zero to UserAction.ResetZoom,

        // Actions
        Key.Delete to UserAction.DeleteSelected,
        Key.Backspace to UserAction.DeleteSelected,
        Key.Escape to UserAction.CancelCurrent,

        Key.DirectionRight to UserAction.NextImage,
        Key.DirectionLeft to UserAction.PreviousImage,
        Key.RightBracket to UserAction.NextImage, // ] key
        Key.LeftBracket to UserAction.PreviousImage  // [ key
    )

    // Helper to get labels for Tooltips (e.g., "Rectangle (R)")
    fun getLabelForMode(mode: CanvasMode): String? {
        val entry = keybindings.entries.firstOrNull {
            (it.value as? UserAction.SwitchTool)?.mode == mode
        }
        return entry?.key?.let { "(${it.toString().takeLast(1)})" }
    }
}


fun handleKeyboardInput(
    event: KeyEvent,
    onEvent: (CanvasEvent) -> Unit
): Boolean {
    // 1. Only process the "Press" event (ignore release)
    if (event.type != KeyEventType.KeyDown) return false

    val isCtrl = event.isCtrlPressed || event.isMetaPressed
    val isShift = event.isShiftPressed

    // 2. Handle Modifier-based shortcuts first (Ctrl+Z, etc.)
    when {
        isCtrl && isShift && event.key == Key.Z -> {
            onEvent(CanvasEvent.Undo); return true
        }

        isCtrl && event.key == Key.Z -> {
            onEvent(CanvasEvent.Undo); return true
        }

        isCtrl && event.key == Key.Y -> {
            onEvent(CanvasEvent.Redo); return true
        }
    }

    // 3. Handle Registered UserActions (Single Key shortcuts)
    val action = ShortcutRegistry.keybindings[event.key]
    return if (action != null) {
        executeAction(action, onEvent)
        true
    } else {
        false
    }
}

private fun executeAction(action: UserAction, onEvent: (CanvasEvent) -> Unit) {
    when (action) {
        is UserAction.SwitchTool -> onEvent(CanvasEvent.ChangeMode(action.mode))
        UserAction.Undo -> onEvent(CanvasEvent.Undo)
        UserAction.Redo -> onEvent(CanvasEvent.Redo)
        UserAction.ZoomIn -> onEvent(CanvasEvent.ZoomIn)
        UserAction.ZoomOut -> onEvent(CanvasEvent.ZoomOut)
        UserAction.ResetZoom -> onEvent(CanvasEvent.ResetZoom)
        UserAction.CancelCurrent -> onEvent(CanvasEvent.ChangeMode(CanvasMode.Pan))
        UserAction.DeleteSelected -> { /* Trigger delete logic */
        }

        UserAction.NextImage -> onEvent(CanvasEvent.NextImage)
        UserAction.PreviousImage -> onEvent(CanvasEvent.PreviousImage)
    }
}