# Atomic Design Architecture Diagram

## Overall Structure

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Puzzroom App                               │
│                         (PuzzroomTheme)                              │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                ┌────────────────┴────────────────┐
                │                                 │
                ▼                                 ▼
        ┌───────────────┐               ┌───────────────┐
        │  ViewModel    │               │   AppState    │
        │  (State +     │               │  (Temporary   │
        │   Logic)      │               │   UI State)   │
        └───────┬───────┘               └───────────────┘
                │
                │ State Flow
                ▼
        ┌───────────────────────────────────────────────────┐
        │                   PAGES                            │
        │  ┌─────────────┐        ┌──────────────────┐     │
        │  │ProjectList  │        │DesignSystem      │     │
        │  │   Page      │        │  Showcase        │     │
        │  └─────────────┘        └──────────────────┘     │
        └───────────────────────┬───────────────────────────┘
                                │
                                │ Uses
                                ▼
        ┌───────────────────────────────────────────────────┐
        │                 TEMPLATES                          │
        │  ┌──────────────┐        ┌──────────────────┐    │
        │  │ListScreen    │        │EditorScreen      │    │
        │  │  Template    │        │  Template        │    │
        │  └──────────────┘        └──────────────────┘    │
        └───────────────────────┬───────────────────────────┘
                                │
                                │ Composes
                                ▼
        ┌───────────────────────────────────────────────────┐
        │                 ORGANISMS                          │
        │  ┌──────────┐ ┌──────────┐ ┌──────────────┐     │
        │  │Project   │ │Empty     │ │Error         │     │
        │  │CardItem  │ │State     │ │Display       │     │
        │  └──────────┘ └──────────┘ └──────────────┘     │
        │  ┌──────────┐ ┌──────────┐                      │
        │  │Loading   │ │Project   │                      │
        │  │Indicator │ │List      │                      │
        │  └──────────┘ └──────────┘                      │
        └───────────────────────┬───────────────────────────┘
                                │
                                │ Uses
                                ▼
        ┌───────────────────────────────────────────────────┐
        │                 MOLECULES                          │
        │  ┌──────────┐ ┌──────────────┐ ┌──────────┐     │
        │  │IconWith  │ │TitleWith     │ │ImageWith │     │
        │  │Label     │ │Subtitle      │ │Fallback  │     │
        │  └──────────┘ └──────────────┘ └──────────┘     │
        │  ┌──────────────────────┐                        │
        │  │Confirmation          │                        │
        │  │Dialog                │                        │
        │  └──────────────────────┘                        │
        └───────────────────────┬───────────────────────────┘
                                │
                                │ Built from
                                ▼
        ┌───────────────────────────────────────────────────┐
        │                    ATOMS                           │
        │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐    │
        │  │App     │ │App     │ │App     │ │App     │    │
        │  │Button  │ │Text    │ │Icon    │ │Card    │    │
        │  └────────┘ └────────┘ └────────┘ └────────┘    │
        │  ┌────────────────┐                              │
        │  │App             │                              │
        │  │Spacer          │                              │
        │  └────────────────┘                              │
        └───────────────────────┬───────────────────────────┘
                                │
                                │ Styled by
                                ▼
        ┌───────────────────────────────────────────────────┐
        │                    THEME                           │
        │  ┌────────────┐ ┌────────────┐ ┌────────────┐   │
        │  │Color.kt    │ │Typography  │ │Theme.kt    │   │
        │  │(Warm       │ │.kt         │ │(Puzzroom   │   │
        │  │ Palette)   │ │            │ │  Theme)    │   │
        │  └────────────┘ └────────────┘ └────────────┘   │
        └───────────────────────────────────────────────────┘
```

## Data Flow: State Down

```
┌──────────────┐
│  ViewModel   │ ← Source of Truth
└──────┬───────┘
       │ StateFlow<Project?>
       ▼
┌──────────────────┐
│  ProjectListPage │ ← Collects State
└──────┬───────────┘
       │ List<Project>
       ▼
┌──────────────────────┐
│ ListScreenTemplate   │ ← Defines Layout
└──────┬───────────────┘
       │ projects: List<Project>
       ▼
┌──────────────────┐
│  ProjectList     │ ← Organism
└──────┬───────────┘
       │ For each project
       ▼
┌──────────────────┐
│ ProjectCardItem  │ ← Organism
└──────┬───────────┘
       │ project.name, project.layoutUrl
       ▼
┌──────────────────┐     ┌──────────────────┐
│ ImageWithFallback│     │ TitleWithSubtitle│ ← Molecules
└──────┬───────────┘     └──────┬───────────┘
       │                        │
       ▼                        ▼
┌──────────┐              ┌──────────┐
│ AppIcon  │              │ AppText  │ ← Atoms
└──────────┘              └──────────┘
```

## Event Flow: Events Up

```
┌──────────┐
│ AppButton│ ← User clicks
└────┬─────┘
     │ onClick: () -> Unit
     ▼
┌──────────────────┐
│ EmptyState       │ ← Organism handles click
└────┬─────────────┘
     │ onAction: () -> Unit
     ▼
┌──────────────────────┐
│ ListScreenTemplate   │ ← Template passes event
└────┬─────────────────┘
     │ onCreateNew: () -> Unit
     ▼
┌──────────────────┐
│ ProjectListPage  │ ← Page defines handler
└────┬─────────────┘
     │ onCreateNew = { 
     │   viewModel.createNewProject()
     │   navController.navigate(...)
     │ }
     ▼
┌──────────────┐
│  ViewModel   │ ← State update
└──────────────┘
```

## Component Relationships

```
ProjectListPage
    ├── ListScreenTemplate
    │   ├── TopAppBar (Material3)
    │   │   ├── AppText (title)
    │   │   └── AppIconButton (Add)
    │   │
    │   └── Content (based on state)
    │       ├── LoadingIndicator
    │       │   └── CircularProgressIndicator (Material3)
    │       │
    │       ├── EmptyState
    │       │   ├── AppText (title)
    │       │   ├── AppText (message)
    │       │   └── AppButton (action)
    │       │
    │       ├── ProjectList
    │       │   └── LazyColumn
    │       │       └── ProjectCardItem (for each)
    │       │           ├── AppCard
    │       │           ├── ImageWithFallback
    │       │           │   ├── AsyncImage or
    │       │           │   └── AppText (fallback)
    │       │           ├── TitleWithSubtitle
    │       │           │   ├── AppText (title)
    │       │           │   └── AppText (subtitle)
    │       │           ├── AppIconButton (Delete)
    │       │           └── ConfirmationDialog (when shown)
    │       │               ├── AppText (title)
    │       │               ├── AppText (message)
    │       │               ├── AppTextButton (Confirm)
    │       │               └── AppTextButton (Cancel)
    │       │
    │       └── ErrorDisplay
    │           ├── AppIcon (Warning)
    │           ├── AppText (error message)
    │           └── AppButton (Retry)
```

## Theme Application

```
┌──────────────────────────────────┐
│        PuzzroomTheme             │
│  ┌────────────────────────────┐  │
│  │   MaterialTheme            │  │
│  │  ┌──────────────────────┐  │  │
│  │  │ colorScheme:         │  │  │
│  │  │  - primary           │  │  │
│  │  │  - secondary         │  │  │
│  │  │  - background        │  │  │
│  │  │  - surface           │  │  │
│  │  │  - error             │  │  │
│  │  └──────────────────────┘  │  │
│  │  ┌──────────────────────┐  │  │
│  │  │ typography:          │  │  │
│  │  │  - displayLarge      │  │  │
│  │  │  - headlineMedium    │  │  │
│  │  │  - titleLarge        │  │  │
│  │  │  - bodyMedium        │  │  │
│  │  │  - labelSmall        │  │  │
│  │  └──────────────────────┘  │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
           │
           │ Applied to
           ▼
    ┌─────────────┐
    │  All Atoms  │
    │  - AppButton uses colorScheme.primary
    │  - AppText uses typography styles
    │  - AppIcon uses colorScheme.onSurface
    └─────────────┘
```

## Color Palette

```
Warm Color Theme
├── Primary Colors
│   ├── Primary (#D4856A) ────────────► Buttons, headers
│   ├── OnPrimary (#FFFFFF) ──────────► Text on primary
│   └── PrimaryContainer (#C06849) ───► Containers, AppBar
│
├── Secondary Colors
│   ├── Secondary (#E8B4A0) ──────────► Accent elements
│   ├── OnSecondary (#3E2723) ────────► Text on secondary
│   └── SecondaryContainer (#D89B81) ─► Secondary containers
│
├── Tertiary Colors
│   ├── Tertiary (#E5D4C1) ───────────► Neutral accents
│   ├── OnTertiary (#3E2723) ─────────► Text on tertiary
│   └── TertiaryContainer (#CFBEAB) ──► Tertiary containers
│
├── Background & Surface
│   ├── Background (#FAF4F0) ─────────► Screen background
│   ├── Surface (#FFFFFF) ────────────► Card background
│   └── SurfaceVariant (#F5EDE8) ─────► Alternative surface
│
└── Feedback Colors
    ├── Error (#D67568) ──────────────► Error states
    └── OnError (#FFFFFF) ────────────► Text on error
```

## Directory Structure

```
ui/
├── atoms/
│   ├── AppButton.kt       (Primary, Outlined, Text buttons)
│   ├── AppText.kt         (Styled text component)
│   ├── AppIcon.kt         (Icons and icon buttons)
│   ├── AppSpacer.kt       (Horizontal/Vertical spacers)
│   └── AppCard.kt         (Card component)
│
├── molecules/
│   ├── IconWithLabel.kt          (Icon + Text)
│   ├── TitleWithSubtitle.kt      (Title + Subtitle)
│   ├── ImageWithFallback.kt      (Image with fallback)
│   └── ConfirmationDialog.kt     (Confirmation dialog)
│
├── organisms/
│   ├── ProjectCardItem.kt        (Complete project card)
│   ├── EmptyState.kt             (Empty state display)
│   ├── ErrorDisplay.kt           (Error display)
│   ├── LoadingIndicator.kt       (Loading state)
│   └── ProjectList.kt            (List of projects)
│
├── templates/
│   ├── ListScreenTemplate.kt     (List screen layout)
│   └── EditorScreenTemplate.kt   (Editor screen layout)
│
├── pages/
│   ├── ProjectListPage.kt        (Project list with data)
│   └── DesignSystemShowcase.kt   (Visual showcase)
│
└── theme/
    ├── Color.kt            (Color palette)
    ├── Typography.kt       (Typography scale)
    └── Theme.kt            (PuzzroomTheme)
```

This architecture provides:
- **Clear separation of concerns** at each level
- **Unidirectional data flow** (State Down, Events Up)
- **High reusability** of components
- **Easy maintenance** and modification
- **Consistent design** across the application
