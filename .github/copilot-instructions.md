# GitHub Copilot Instructions for Puzzroom Project

## Project Overview
Puzzroom is a Kotlin Multiplatform project (Android, iOS, Web, Desktop) using Compose Multiplatform with **Atomic Design** architecture.

## 🎨 UI Architecture - Atomic Design Principles

### Mandatory Component Hierarchy
When creating or modifying UI components, **ALWAYS** follow this hierarchy:

1. **Atoms** (`ui/atoms/`) - Indivisible UI elements
   - Basic components: Button, Text, Icon, Spacer, Card
   - Must be generic and reusable across the entire app
   - No business logic, only presentation
   - Examples: `AppButton`, `AppText`, `AppIcon`, `AppSpacer`, `AppCard`

2. **Molecules** (`ui/molecules/`) - Simple combinations of atoms
   - Combine 2-3 atoms for a single purpose
   - Minimal logic, focused on presentation
   - Examples: `IconWithLabel`, `TitleWithSubtitle`, `ImageWithFallback`

3. **Organisms** (`ui/organisms/`) - Complex sections
   - Combine molecules and atoms into major UI sections
   - Can contain feature-specific logic
   - Examples: `ProjectCardItem`, `EmptyState`, `ProjectList`, `PolygonListPanel`

4. **Templates** (`ui/templates/`) - Page layout structures
   - Define layout and composition without data
   - Receive content via parameters/slots
   - Examples: `ListScreenTemplate`, `EditorScreenTemplate`

5. **Pages** (`ui/pages/`) - Complete pages with data
   - Integrate ViewModels and state management
   - Compose templates with organisms
   - Examples: `ProjectListPage`, `RoomEditorPage`

### Component Creation Rules

#### When creating a new UI component:
1. **Determine the correct level** based on complexity:
   - Single element? → Atom
   - 2-3 atoms together? → Molecule
   - Multiple molecules? → Organism
   - Full layout structure? → Template
   - Needs ViewModel? → Page

2. **Follow naming conventions**:
   - Atoms: `App[Element].kt` (e.g., `AppButton.kt`)
   - Molecules: `[Descriptive][Purpose].kt` (e.g., `IconWithLabel.kt`)
   - Organisms: `[Feature][Component].kt` (e.g., `ProjectCardItem.kt`)
   - Templates: `[Purpose]Template.kt` (e.g., `ListScreenTemplate.kt`)
   - Pages: `[Feature]Page.kt` (e.g., `ProjectListPage.kt`)

3. **Use the warm color theme**:
   ```kotlin
   // Primary: Warm terracotta (#D4856A)
   // Secondary: Soft peach (#E8B4A0)
   // Tertiary: Warm beige (#E5D4C1)
   // Background: Warm white (#FAF4F0)
   ```
   Access via: `MaterialTheme.colorScheme.primary`, etc.

#### Component Structure Template:
```kotlin
// For Atoms/Molecules
@Composable
fun ComponentName(
    // Required parameters first
    // Optional parameters with defaults
    modifier: Modifier = Modifier
) {
    // Implementation
}

// For Organisms/Templates/Pages
@Composable
fun ComponentName(
    // State/data parameters
    // Callback parameters (onEvent pattern)
    modifier: Modifier = Modifier
) {
    // Implementation
}
```

### Data Flow Pattern
- **State Down**: ViewModel → Pages → Templates → Organisms → Molecules → Atoms
- **Events Up**: User interactions flow back through callbacks to ViewModel

### Code Organization

#### File locations:
```
composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/
├── ui/
│   ├── atoms/          # Place all atomic components here
│   ├── molecules/      # Place all molecule components here
│   ├── organisms/      # Place all organism components here
│   ├── templates/      # Place all template layouts here
│   ├── pages/          # Place all page components here
│   ├── theme/          # Theme files (PuzzroomTheme.kt, Color.kt, Type.kt)
│   ├── viewmodel/      # ViewModels for state management
│   └── state/          # UI state classes
├── data/               # Data layer
└── domain/             # Domain layer
```

### When Refactoring Existing Code

1. **Identify reusable patterns**: Look for repeated UI patterns that can become molecules/organisms
2. **Extract step by step**:
   - First: Extract atoms from complex components
   - Second: Group atoms into molecules
   - Third: Compose molecules into organisms
   - Fourth: Create templates from page layouts
   - Fifth: Simplify pages to use templates

3. **Maintain backwards compatibility** during migration

### Common Patterns

#### Button Usage:
```kotlin
// Primary action
AppButton(text = "Save", onClick = { })

// Secondary action
AppButton(text = "Cancel", onClick = { }, type = ButtonType.Outlined)

// Tertiary action
AppButton(text = "Delete", onClick = { }, type = ButtonType.Text)
```

#### Empty State:
```kotlin
EmptyState(
    title = "No items found",
    message = "Create your first item to get started",
    actionText = "Create",
    onAction = { }
)
```

#### List with Items:
```kotlin
// Use organisms for list items
ProjectList(
    projects = projects,
    onProjectClick = { },
    onProjectDelete = { }
)
```

### Testing Guidelines
- Atoms: Test appearance and basic interactions
- Molecules: Test composition and simple logic
- Organisms: Test feature-specific behavior
- Templates: Test layout structure
- Pages: Test integration with ViewModels

### Documentation Requirements
When creating new components:
1. Add KDoc comments explaining purpose
2. Document all parameters
3. Include usage examples for organisms and above
4. Update relevant README files if introducing new patterns

### Don't:
- ❌ Create components outside the atomic hierarchy
- ❌ Mix business logic into atoms/molecules
- ❌ Use hardcoded colors (always use theme)
- ❌ Create deeply nested component structures
- ❌ Bypass the hierarchy (e.g., Page directly using Atoms without Organisms)

### Do:
- ✅ Start with the smallest component needed
- ✅ Compose upward through the hierarchy
- ✅ Keep components focused and single-purpose
- ✅ Use preview functions for development
- ✅ Follow the warm color theme consistently

## Additional Context

### Platform-Specific Code
- Use `expect`/`actual` for platform-specific implementations
- Place in appropriate source sets: `androidMain`, `iosMain`, `jsMain`, etc.

### State Management
- Use ViewModels for business logic and state
- Keep UI components pure and stateless when possible
- Use remember/mutableStateOf for local UI state only

### Resources
- See `/docs/design/AtomicDesignGuide.md` for detailed Japanese documentation
- See `/docs/design/ComponentMigrationExamples.md` for migration examples
- See `/composeApp/src/commonMain/kotlin/tokyo/isseikuzumaki/puzzroom/ui/README.md` for UI structure

---

**Remember**: Every UI component must fit into the Atomic Design hierarchy. When in doubt, start smaller (Atom/Molecule) and compose upward.

