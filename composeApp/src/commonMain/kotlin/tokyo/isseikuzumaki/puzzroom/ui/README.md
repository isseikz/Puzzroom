# UI Structure - Atomic Design

This directory contains the UI components organized following the Atomic Design methodology.

## Directory Structure

```
ui/
├── atoms/          # Smallest UI components (Button, Text, Icon, etc.)
├── molecules/      # Simple combinations of atoms
├── organisms/      # Complex sections combining molecules and atoms
├── templates/      # Page layout structures
├── pages/          # Complete pages with data (ViewModel integration)
├── theme/          # Theme configuration (colors, typography)
├── component/      # Legacy components (specialized/platform-specific)
├── screen/         # Legacy screens (to be migrated)
├── state/          # UI state classes
└── viewmodel/      # ViewModels
```

## Atomic Design Hierarchy

### Atoms
Basic UI elements that cannot be broken down further:
- `AppButton` - Styled buttons (Primary, Outlined, Text)
- `AppText` - Styled text
- `AppIcon` - Icons and icon buttons
- `AppSpacer` - Horizontal and vertical spacers
- `AppCard` - Cards with consistent styling

### Molecules
Small functional units combining atoms:
- `IconWithLabel` - Icon paired with text
- `TitleWithSubtitle` - Title and subtitle combination
- `ImageWithFallback` - Image with fallback display
- `ConfirmationDialog` - Confirmation dialog
- `SaveStateIndicator` - Save state indicator with icon and message

### Organisms
Larger sections combining molecules and atoms:
- `ProjectCardItem` - Complete project card with image, title, and actions
- `EmptyState` - Empty state display with message and action
- `ErrorDisplay` - Error display with retry option
- `LoadingIndicator` - Loading state display
- `ProjectList` - List of project cards
- `SaveLoadDialogs` - Save and load dialogs for JSON data
- `PolygonListPanel` - Panel displaying list of polygons/rooms
- `FurnitureLibraryPanel` - Furniture selection panel with categories
- `FurniturePlacementToolbar` - Toolbar for furniture placement controls

### Templates
Page layout structures without specific data:
- `ListScreenTemplate` - List screen layout with app bar
- `EditorScreenTemplate` - Editor layout with canvas and sidebar

### Pages
Complete pages combining templates with data and ViewModel:
- `ProjectListPage` - Project list page with state management

## Theme

### Color Scheme
The app uses a warm color palette:
- **Primary**: Warm terracotta (#D4856A)
- **Secondary**: Soft peach (#E8B4A0)
- **Tertiary**: Warm beige (#E5D4C1)
- **Background**: Warm white (#FAF4F0)
- **Surface**: White (#FFFFFF)
- **Error**: Muted warm red (#D67568)

### Usage
Wrap your app with `PuzzroomTheme`:
```kotlin
PuzzroomTheme {
    // Your app content
}
```

## Data Flow Pattern

- **State Down**: ViewModel → Pages → Templates → Organisms → Molecules → Atoms
- **Events Up**: Atoms → Molecules → Organisms → Templates → Pages → ViewModel

## Best Practices

1. **Atoms** should be generic and reusable across the entire app
2. **Molecules** should be simple combinations with a single purpose
3. **Organisms** can be more specific to certain features
4. **Templates** define layout only, no data
5. **Pages** handle data and ViewModel integration

## Migration Guide

To migrate existing screens to Atomic Design:

1. Identify basic UI elements → Create/use Atoms
2. Find small functional units → Create Molecules
3. Identify major sections → Create Organisms
4. Extract layout structure → Create Template
5. Integrate with ViewModel → Create Page

For detailed documentation in Japanese, see `docs/design/AtomicDesignGuide.md`.
