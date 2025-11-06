JavaFX Migration Plan
=====================

Overview
--------
- Replace all Swing-based UI with JavaFX counterparts while preserving existing layouts, dialog flows, and behaviours.
- Maintain localisation keys and business logic; only UI toolkit changes.
- Ensure concurrency callbacks target the JavaFX application thread.

Primary Entry Points
--------------------
- `com.skcraft.launcher.Launcher`
  - Currently initialises Swing look and feel and launches `LauncherFrame` on the EDT.
- `com.skcraft.launcher.dialog.LauncherFrame`
  - Main window constructed with Swing components, MigLayout, `InstanceTable`, and `WebpagePanel`.

Dialogs & Windows to Port
-------------------------
- `LauncherFrame` (main window with instances table, split pane, toolbar buttons, context menu).
- `ConfigurationDialog` (tabbed settings window with form panels and button bar).
- `InstanceSettingsDialog` (instance-specific form grouped by tabs and checkboxes).
- `FeatureSelectionDialog` (feature list with checkbox table and progress interactions).
- `AccountSelectDialog` (list selector for accounts with option buttons).
- `LoginDialog` (credential prompt and password recovery link).
- `ProgressDialog` (modal progress indicator binding to `ObservableFuture`).
- `ConsoleFrame` & `ProcessConsoleFrame` (log consoles with auto-scroll and copy support).
- `AboutDialog` (application information with hyperlink support).

Swing Helper Components & Utilities
-----------------------------------
- `com.skcraft.launcher.swing` package:
  - Layout/containers: `FormPanel`, `LinedBoxPanel`, `HeaderPanel`, `LinedBoxPanel`, `WebpagePanel`, `WebpageLayoutManager`.
  - Tables & models: `InstanceTable`, `InstanceTableModel`, `CheckboxTable`, `FeatureTableModel`, `DefaultTable`, `TableColumnAdjuster`.
  - Input helpers: `DirectoryField`, `FileField`, `TextFieldPopupMenu`, `LinkButton`.
  - Misc: `ActionListeners`, `DoubleClickToButtonAdapter`, `PopupMouseAdapter`, `SelectionKeeper`, `MessageLog`, `ObjectSwingMapper`.
- `com.skcraft.launcher.util.SwingExecutor` and `SwingHelper`.

Other Swing References
----------------------
- `LaunchSupervisor`, `LaunchProcessHandler`, `UpdateManager`, `AccountList`, `BaseUpdater`, `LimitLinesDocumentListener` (console log), all relying on Swing constructs for callbacks or document models.

Migration Considerations
------------------------
- Adopt `javafx.application.Application` with `Launcher` delegating to it.
- Introduce `FxExecutor` (similar responsibilities to `SwingExecutor`) backed by `Platform.runLater`.
- Mirror `InstanceTableModel` behaviour with `TableView<Instance>` and observable lists.
- Replace `WebpagePanel` with JavaFX `WebView` (or `HTMLEditor`-like component) while maintaining lazy loading and hyperlink handling.
- Rebuild form layouts with `GridPane`, `VBox`, `HBox`, and `BorderPane` to approximate current spacing.
- Provide JavaFX equivalents for `ObjectSwingMapper` (reflection-based binding) or leverage property bindings directly.
- Ensure modal behaviour and blocking semantics (e.g., `ProgressDialog.showProgress`) are replicated using JavaFX dialogs or custom stages.
- Update Gradle build to include JavaFX modules (`controls`, `graphics`, `web`) with platform-specific runtime dependencies.

Next Steps
----------
1. Add JavaFX dependencies and initialise application scaffolding.
2. Port helper utilities (`SwingExecutor` → `FxExecutor`, `SwingHelper` → `FxUiHelper`, etc.).
3. Recreate main window UI in JavaFX, followed by individual dialogs.
4. Remove remaining Swing usage and run integration checks.
