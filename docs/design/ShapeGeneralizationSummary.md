# Shape Generalization Summary

## Overview
This document summarizes the generalization of FurnitureCreationPage to support both furniture placement and room creation with a unified architecture.

## Objectives
1. Generalize furniture placement components to work with room shapes (walls, doors, windows)
2. Create a unified canvas for placing both furniture and room elements
3. Maintain the existing furniture placement functionality while adding room creation capability

## Architecture

### Domain Models

#### RoomShape.kt (New)
- **RoomShapeType**: Enum for room element types
  - `WALL`: 壁 (wall) - represented as a line segment
  - `DOOR`: 扉 (door) - represented as a sector/arc
  - `WINDOW`: 窓 (window) - represented as a line segment

- **RoomShapeElement**: Data class representing room elements
  - Wraps a Shape with type information and width
  - Provides factory methods for creating walls and doors

### UI Components

#### Organisms

##### ShapeLayoutCanvas.kt (New)
Generalized canvas component that replaces furniture-specific logic with shape-agnostic logic.

**Key Features:**
- Works with any Polygon shapes
- Supports placing, selecting, and moving shapes
- Uses `PlacedShape` data class that wraps Polygon with position, rotation, color, and name
- Reuses existing geometry utilities from `ui.state.Geometry`

**Differences from FurnitureLayoutCanvas:**
- Generic shape handling instead of furniture-specific
- `PlacedShape` instead of `PlacedFurniture`
- Supports custom colors for different shape types

##### RoomShapeSelector.kt (New)
Selector for room shape types (Wall, Door, Window).

**Key Features:**
- Displays room shape types in a horizontal scrollable list
- Selection indication with visual feedback
- Follows the same pattern as FurnitureSelector

##### ShapeAttributeForm.kt (New)
Form for editing attributes of room shapes.

**Key Features:**
- Edit shape type (Wall/Door/Window)
- Edit dimensions (width, length for walls, angle for doors)
- Validation of input values
- Modal bottom sheet integration example

#### Templates

##### RoomCreationTemplate.kt (New)
Template for room creation UI that uses the generalized components.

**Structure:**
- `ShapeLayoutCanvas` for the canvas
- `RoomShapeSelector` for selecting shape types
- `ShapeAttributeForm` in a modal bottom sheet for editing
- `ButtonToCreate` for triggering shape attribute editing

**Relationship:**
Similar to `FurniturePlacementTemplate` but for room creation.

#### Pages

##### RoomCreationPage.kt (New)
Page component that manages state for room creation.

**State Management:**
- Selected shape type
- Rotation angle
- Placed shapes list
- Selected shape index
- Current position

**Pattern:**
Follows the same pattern as `FurniturePlacementPage` for consistency.

## Component Hierarchy

```
Room Creation Flow:
RoomCreationPage (state management)
  └── RoomCreationTemplate (layout template)
      ├── ShapeLayoutCanvas (canvas organism)
      ├── RoomShapeSelector (selector organism)
      └── ShapeAttributeForm (form organism, in modal)

Furniture Placement Flow (existing, unchanged):
FurniturePlacementPage (state management)
  └── FurniturePlacementTemplate (layout template)
      ├── FurnitureLayoutCanvas (canvas organism)
      ├── FurnitureSelector (selector organism)
      └── FurnitureCreationForm (form organism, in modal)
```

## Design Decisions

### Using Polygon Consistently
- All shapes are represented as `Polygon` for consistency
- The existing `Shape` interface was available but using `Polygon` directly simplifies implementation
- Future extensions can add other shape types by extending the `Shape` interface

### PlacedShape vs PlacedFurniture
- Created a new `PlacedShape` data class instead of modifying `PlacedFurniture`
- `PlacedShape` is more generic and can represent any shape
- Includes color property for distinguishing different shape types visually

### Separate Selectors
- `FurnitureSelector` remains specific to furniture items
- `RoomShapeSelector` is specific to room shape types
- This maintains type safety and clarity over a single generic selector

### Reusing Geometry Utilities
- All geometry operations use existing utilities from `ui.state.Geometry`
- `rotateAroundCenterOffsets`, `toCanvasOffset`, `toPoint` are reused
- Ensures consistency with existing furniture placement behavior

## Preview Composables

All new UI components include `@Preview` composables for visual verification in Android Studio:
- `ShapeLayoutCanvasPreview`
- `RoomShapeSelectorPreview`
- `ShapeAttributeFormPreview`
- `ShapeAttributeFormWithBottomSheetPreview`
- `RoomCreationTemplatePreview`
- `RoomCreationPagePreview`

## Future Enhancements

1. **Enhanced Door Representation**
   - Currently doors are simplified as rectangles
   - Could be enhanced to show actual arc/sector shapes

2. **Wall Editing**
   - Add ability to snap walls together
   - Add ability to adjust wall endpoints after placement

3. **Shape Library**
   - Create predefined room shapes (rectangular rooms, L-shaped rooms, etc.)
   - Similar to furniture templates

4. **Integration with Project**
   - Persist created rooms to the project
   - Load and edit existing rooms

## Testing Considerations

Since there's limited existing test infrastructure, manual verification is recommended:
1. Visual inspection using Preview composables
2. Interactive testing in the application
3. Verify that existing furniture placement functionality is not affected

## Files Created

1. `composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/domain/RoomShape.kt`
2. `composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/ui/organisms/ShapeLayoutCanvas.kt`
3. `composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/ui/organisms/RoomShapeSelector.kt`
4. `composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/ui/organisms/ShapeAttributeForm.kt`
5. `composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/ui/templates/RoomCreationTemplate.kt`
6. `composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/ui/pages/RoomCreationPage.kt`
7. `docs/design/ShapeGeneralizationSummary.md` (this document)

## Files Modified

None. All existing functionality remains unchanged to maintain backward compatibility.
