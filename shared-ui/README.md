# Shared UI Module

This module contains shared UI components used across all applications in the Puzzroom monorepo.

## Purpose

The `shared-ui` module provides:
- **Atomic Design Components**: Atoms, molecules, and organisms
- **Consistent Theme**: Warm color palette and typography
- **Reusability**: Shared across Puzzroom and NLT apps

## Structure

```
shared-ui/
└── src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/shared/ui/
    ├── atoms/          # Basic UI elements
    │   ├── AppButton.kt
    │   ├── AppText.kt
    │   ├── AppCard.kt
    │   ├── AppSpacer.kt
    │   ├── AppIcon.kt
    │   ├── AppSlider.kt
    │   └── AppTextField.kt
    │
    ├── molecules/      # Simple combinations (TBD)
    ├── organisms/      # Complex sections (TBD)
    └── theme/          # Theme configuration
        ├── Color.kt
        ├── Theme.kt
        └── Typography.kt
```

## Usage

### In Application Modules

Add dependency in `build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation(project(":shared-ui"))
}
```

### Import Components

```kotlin
import tokyo.isseikuzumaki.puzzroom.shared.ui.atoms.AppButton
import tokyo.isseikuzumaki.puzzroom.shared.ui.theme.PuzzroomTheme

@Composable
fun MyScreen() {
    PuzzroomTheme {
        AppButton(
            text = "Click Me",
            onClick = { /* action */ }
        )
    }
}
```

## Theme

### Color Palette

The shared UI uses a warm, calming color palette:

- **Primary**: Warm terracotta (#D4856A)
- **Secondary**: Soft peach (#E8B4A0)
- **Tertiary**: Warm beige (#E5D4C1)
- **Background**: Warm white (#FAF4F0)

### Usage

Always use theme colors via `MaterialTheme.colorScheme`:

```kotlin
Text(
    text = "Hello",
    color = MaterialTheme.colorScheme.primary
)
```

**Never hardcode colors!**

## Atomic Design Principles

### Atoms

Basic, indivisible UI elements:
- Single purpose
- No business logic
- Highly reusable

Example: `AppButton`, `AppText`

### Molecules

Simple combinations of 2-3 atoms:
- Single specific purpose
- Minimal logic

Example: `IconWithLabel`, `TitleWithSubtitle`

### Organisms

Complex sections combining molecules and atoms:
- Feature-specific
- Can contain logic

Example: `ProjectCardItem`, `EmptyState`

## Contributing

When adding new shared components:

1. **Determine the correct level**: Atom, Molecule, or Organism
2. **Follow naming conventions**: `App[Element]` for atoms, `[Descriptive][Purpose]` for molecules
3. **Use theme colors**: Never hardcode colors
4. **Add documentation**: KDoc comments for all public components
5. **Consider reusability**: Make components generic and configurable

## Examples

### Creating a Custom Button Variant

```kotlin
@Composable
fun WarningButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    )
}
```

### Using Shared Theme

```kotlin
@Composable
fun MyApp() {
    PuzzroomTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Your app content
        }
    }
}
```

## Design Philosophy

- **Consistency**: All apps share the same design language
- **Maintainability**: Changes to shared components affect all apps
- **Flexibility**: Components are configurable via parameters
- **Simplicity**: Keep components focused and simple
