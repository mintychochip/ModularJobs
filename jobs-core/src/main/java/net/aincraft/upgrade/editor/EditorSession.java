package net.aincraft.upgrade.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.aincraft.upgrade.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks the editing state for a player's upgrade tree editing session.
 * Supports undo/redo and draft saving.
 */
public final class EditorSession {

  private final UUID playerId;
  private final EditorTree tree;
  private int scrollOffsetY = 0;
  private int scrollOffsetX = 0;

  // Selection state
  @Nullable
  private String selectedNodeId;
  private boolean isDragging = false;
  private boolean pathEditMode = false;

  // Undo/redo stacks
  private final List<EditorTree> undoStack = new ArrayList<>();
  private final List<EditorTree> redoStack = new ArrayList<>();
  private static final int MAX_UNDO_STACK = 50;

  public EditorSession(@NotNull UUID playerId, @NotNull EditorTree tree) {
    this.playerId = playerId;
    this.tree = tree;
    saveSnapshot(); // Initial state
  }

  public @NotNull UUID playerId() {
    return playerId;
  }

  public @NotNull EditorTree tree() {
    return tree;
  }

  public int scrollOffsetY() {
    return scrollOffsetY;
  }

  public int scrollOffsetX() {
    return scrollOffsetX;
  }

  public void setScrollOffsetY(int offset) {
    this.scrollOffsetY = Math.max(0, offset);
  }

  public void setScrollOffsetX(int offset) {
    this.scrollOffsetX = Math.max(0, offset);
  }

  public @Nullable String selectedNodeId() {
    return selectedNodeId;
  }

  public void selectNode(@Nullable String nodeId) {
    this.selectedNodeId = nodeId;
    this.isDragging = false;
  }

  public boolean isDragging() {
    return isDragging;
  }

  public void setDragging(boolean dragging) {
    this.isDragging = dragging;
  }

  public boolean isPathEditMode() {
    return pathEditMode;
  }

  public void setPathEditMode(boolean pathEditMode) {
    this.pathEditMode = pathEditMode;
  }

  /**
   * Save current tree state for undo.
   */
  public void saveSnapshot() {
    undoStack.add(tree.copy());
    if (undoStack.size() > MAX_UNDO_STACK) {
      undoStack.remove(0);
    }
    redoStack.clear(); // Clear redo on new action
  }

  /**
   * Undo the last action.
   * @return true if undo was performed
   */
  public boolean undo() {
    if (undoStack.size() <= 1) {
      return false; // Need at least initial state
    }
    // Current state goes to redo
    redoStack.add(undoStack.remove(undoStack.size() - 1));
    // Restore previous state
    EditorTree previous = undoStack.get(undoStack.size() - 1);
    tree.restoreFrom(previous);
    return true;
  }

  /**
   * Redo the last undone action.
   * @return true if redo was performed
   */
  public boolean redo() {
    if (redoStack.isEmpty()) {
      return false;
    }
    EditorTree next = redoStack.remove(redoStack.size() - 1);
    undoStack.add(tree.copy());
    tree.restoreFrom(next);
    return true;
  }

  public boolean canUndo() {
    return undoStack.size() > 1;
  }

  public boolean canRedo() {
    return !redoStack.isEmpty();
  }
}
