# Furniture Shape Editing Modal Bottom Sheet - Visual Guide

## Component Architecture

```
Page (FurnitureShapeEditExample)
    └── ModalBottomSheet (Material3)
        └── Organism (FurnitureShapeEditForm)
            ├── Header with Close Button (Atom: AppIconButton)
            ├── Name Input (Atom: AppTextField)
            ├── Dimensions (Molecule: DecimalDimensionInput)
            │   ├── Width Input (Atom: AppTextField)
            │   └── Height Input (Atom: AppTextField)
            ├── Shape Selector (Molecule: ShapeSelector)
            │   └── Shape Cards (Atom: AppCard + AppText)
            ├── Texture Selector (Molecule: TextureSelector)
            │   └── Texture Cards (Atom: AppCard + AppText)
            └── Action Buttons (Molecule: ActionButtons)
                ├── Cancel Button (Atom: AppOutlinedButton)
                └── Save Button (Atom: AppButton)
```

## Component Hierarchy (Atomic Design)

### Level 1: Atoms (Basic Building Blocks)
```kotlin
// Already existed in the project
- AppTextField      // Text input field
- AppButton         // Primary button
- AppOutlinedButton // Secondary button
- AppIconButton     // Icon button
- AppText           // Text display
- AppCard           // Card container
- VerticalSpacer    // Spacing
```

### Level 2: Molecules (Simple Combinations)
```kotlin
// NEW: Created for this feature
✨ DecimalDimensionInput    // Width + Height inputs
✨ ShapeSelector            // Horizontal list of shape templates
✨ TextureSelector          // Horizontal list of textures
✨ SelectionBorderUtils     // Utility for selection styling

// Already existed
- ActionButtons             // Cancel + Confirm buttons
```

### Level 3: Organisms (Complex Sections)
```kotlin
// NEW: Created for this feature
✨ FurnitureShapeEditForm   // Complete editing form
```

### Level 4: Pages (Complete Screens)
```kotlin
// NEW: Created for this feature
✨ FurnitureShapeEditExample // Demo/integration page
```

## Data Flow

```
User Action → State Update → UI Update
     ↓
[Click Edit Button]
     ↓
Set showBottomSheet = true
     ↓
ModalBottomSheet appears
     ↓
[User edits fields]
     ↓
FormData state updates
     ↓
Validation checks
     ↓
Save button enabled/disabled
     ↓
[User clicks Save]
     ↓
onSave callback with validated data
     ↓
Parent handles save
     ↓
Set showBottomSheet = false
     ↓
ModalBottomSheet dismisses
```

## State Management Pattern

```kotlin
// 1. State variables
var showBottomSheet by remember { mutableStateOf(false) }
val sheetState = rememberModalBottomSheetState()

// 2. Trigger
Button(onClick = { showBottomSheet = true })

// 3. Sheet
if (showBottomSheet) {
    ModalBottomSheet(
        onDismissRequest = { showBottomSheet = false },
        sheetState = sheetState
    ) {
        FurnitureShapeEditForm(...)
    }
}
```

## Validation Flow

```
Form Input
    ↓
Name: not blank? ──→ isNameValid
Width: > 0?       ──→ isWidthValid
Height: > 0?      ──→ isHeightValid
    ↓
isFormValid = all valid?
    ↓
Save button enabled = isFormValid
```

## User Interactions

### Opening the Sheet
1. User clicks "Edit" button
2. `showBottomSheet` set to `true`
3. Sheet animates up from bottom
4. Background dims (scrim)

### Editing
1. User edits name → updates `formData.name`
2. User edits width → updates `formData.width`
3. User edits height → updates `formData.height`
4. User selects shape → updates `formData.shape`
5. User selects texture → updates `formData.texture`
6. Each change triggers validation

### Closing the Sheet (4 ways)

#### 1. Save Button
```
User clicks "Save" → 
onSave(formData) called → 
Parent processes data → 
showBottomSheet = false → 
Sheet dismisses
```

#### 2. Cancel Button
```
User clicks "Cancel" → 
onDismiss() called → 
showBottomSheet = false → 
Sheet dismisses → 
Changes discarded
```

#### 3. Close Button (×)
```
User clicks Close icon → 
onDismiss() called → 
showBottomSheet = false → 
Sheet dismisses → 
Changes discarded
```

#### 4. Swipe Down / Tap Scrim
```
User swipes down or taps outside → 
onDismissRequest called → 
showBottomSheet = false → 
Sheet dismisses → 
Changes discarded
```

## Color Theme Application

```
Selected Items:
- Background: primaryContainer (#E8B4A0 area)
- Text: onPrimaryContainer
- Border: primary (#D4856A)

Unselected Items:
- Background: surface (white)
- Text: onSurface
- Secondary Text: onSurfaceVariant

Buttons:
- Primary (Save): primary background
- Secondary (Cancel): outlined with primary color
```

## File Structure

```
composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/
├── ui/
│   ├── atoms/                    (Already existed)
│   │   ├── AppTextField.kt
│   │   ├── AppButton.kt
│   │   ├── AppIconButton.kt
│   │   └── ...
│   ├── molecules/
│   │   ├── DecimalDimensionInput.kt    ✨ NEW
│   │   ├── ShapeSelector.kt            ✨ NEW
│   │   ├── TextureSelector.kt          ✨ NEW
│   │   ├── SelectionBorderUtils.kt     ✨ NEW
│   │   └── ActionButtons.kt            (Already existed)
│   ├── organisms/
│   │   └── FurnitureShapeEditForm.kt   ✨ NEW
│   └── pages/
│       └── FurnitureShapeEditExample.kt ✨ NEW
└── domain/
    └── Models.kt                        (Already existed)

docs/
├── FurnitureShapeEditModalGuide.md     ✨ NEW (Japanese)
├── README_IMPLEMENTATION.md             ✨ NEW (English)
└── VISUAL_GUIDE.md                      ✨ NEW (This file)
```

## Integration Example

To integrate into existing furniture management:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExistingFurnitureScreen(
    furniture: Furniture,
    onFurnitureUpdated: (Furniture) -> Unit
) {
    var showEditSheet by remember { mutableStateOf(false) }
    
    // Your existing UI
    Column {
        Text("Furniture: ${furniture.name}")
        Button(onClick = { showEditSheet = true }) {
            Text("Edit Shape")
        }
    }
    
    // Add the editing sheet
    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            FurnitureShapeEditForm(
                initialData = FurnitureShapeFormData(
                    name = furniture.name,
                    width = furniture.shape.width.toString(),
                    height = furniture.shape.height.toString()
                ),
                onDismiss = { showEditSheet = false },
                onSave = { formData ->
                    // Convert form data to Furniture model
                    val updated = furniture.copy(
                        name = formData.name,
                        // Update shape with new dimensions
                    )
                    onFurnitureUpdated(updated)
                    showEditSheet = false
                }
            )
        }
    }
}
```

## Testing Checklist

Manual testing steps:

- [ ] Sheet appears when triggered
- [ ] Close button (×) works
- [ ] Cancel button works
- [ ] Swipe down dismisses
- [ ] Tap outside (scrim) dismisses
- [ ] Back button/gesture dismisses
- [ ] Name validation (empty = disabled save)
- [ ] Width validation (non-positive = disabled save)
- [ ] Height validation (non-positive = disabled save)
- [ ] Decimal input accepted (e.g., "100.5")
- [ ] Shape selection works
- [ ] Texture selection works
- [ ] Selected items show border
- [ ] Selected items show primary container color
- [ ] Save button disabled until valid
- [ ] Save button calls onSave with data
- [ ] Form data preserved during editing
- [ ] Form resets when reopened

## Accessibility Features

✅ **Close Button**: Explicit close button for screen readers and keyboard navigation
✅ **Back Gesture**: Works with device back button/gesture
✅ **Scrim Dismiss**: Clear visual feedback with tap-to-dismiss
✅ **Labels**: All inputs have descriptive labels in Japanese
✅ **Validation Feedback**: Save button state indicates validity
✅ **Keyboard Types**: Appropriate keyboards for each input type

## Performance Considerations

- Lazy loading for shape/texture lists (uses LazyRow)
- State hoisting for form data
- Minimal recomposition with remember
- No nested bottom sheets (performance and UX)

## Future Enhancements

1. **Visual Improvements**
   - Replace text placeholders with actual SVG icons for shapes
   - Add texture preview images
   - Add shape preview in the form

2. **Functionality**
   - Custom shape drawing mode
   - Shape rotation support
   - Multiple texture layers
   - Undo/redo functionality

3. **Integration**
   - Connect to actual Furniture model
   - ViewModel integration
   - Persistence layer
   - Real-time preview on canvas

4. **Testing**
   - Unit tests for validation
   - UI tests for sheet behavior
   - Screenshot tests for visual regression
