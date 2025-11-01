# Puzzroom Monorepo - Development Guide

## Overview

This repository is organized as a **monorepo** containing multiple Kotlin Multiplatform applications that share common UI components and architecture patterns.

## Repository Structure

```
Puzzroom/
├── composeApp/           # Original Puzzroom application (room layout planning)
├── nlt-app/             # NLT application (notification & location tracker)
├── shared-ui/           # Shared UI components library
├── docs/                # Documentation
│   ├── design/          # Design documents for Puzzroom
│   └── nlt/             # Design documents for NLT
├── gradle/              # Gradle configuration
├── settings.gradle.kts  # Multi-module project settings
└── build.gradle.kts     # Root build configuration
```

## Applications

### 1. Puzzroom (composeApp)

**Purpose**: Room layout and furniture placement planning application

**Features**:
- Project management
- Room creation and editing
- Furniture placement with 2D visualization
- Atomic Design UI architecture

**Package**: `tokyo.isseikuzumaki.puzzroom`

### 2. NLT (nlt-app)

**Purpose**: Notification and Location Tracker for monitoring payment notifications with location data

**Features**:
- Passive notification monitoring
- Location capture on notification
- Payment information parsing
- Firestore data persistence
- Planned: Calendar and map visualization

**Package**: `tokyo.isseikuzumaki.nolotracker`

**Status**: Core features implemented, UI visualization pending

**Documentation**: See [nlt-app/README.md](../nlt-app/README.md) and [docs/nlt/ImplementationSummary.md](../docs/nlt/ImplementationSummary.md)

## Shared Components

### shared-ui Module

**Purpose**: Provides shared UI components following Atomic Design principles

**Contents**:
- **Atoms**: Basic UI elements (AppButton, AppText, AppCard, AppSpacer, AppIcon, AppSlider, AppTextField)
- **Theme**: Warm color palette and typography
- **Future**: Molecules and organisms to be added as needed

**Package**: `tokyo.isseikuzumaki.puzzroom.shared.ui`

**Documentation**: See [shared-ui/README.md](../shared-ui/README.md)

## Build System

### Gradle Modules

The project uses Gradle's multi-module setup:

```kotlin
// settings.gradle.kts
include(":composeApp")
include(":shared-ui")
include(":nlt-app")
```

### Dependencies

Each application module depends on `shared-ui`:

```kotlin
commonMain.dependencies {
    implementation(project(":shared-ui"))
    // other dependencies...
}
```

## Development Workflow

### 1. Working on Existing Apps

#### Puzzroom (composeApp)
```bash
# Build
./gradlew :composeApp:assembleDebug

# Run on desktop
./gradlew :composeApp:run

# Run tests
./gradlew :composeApp:test
```

#### NLT (nlt-app)
```bash
# Build Android
./gradlew :nlt-app:assembleDebug

# Run tests
./gradlew :nlt-app:test

# Note: Requires google-services.json for Firebase
```

### 2. Working on Shared Components

When modifying shared-ui components:

```bash
# Build shared-ui
./gradlew :shared-ui:build

# Rebuild dependent apps
./gradlew :composeApp:build :nlt-app:build
```

**Impact**: Changes to shared-ui affect ALL applications!

### 3. Adding a New Application

To add a new KMP application to the monorepo:

1. **Create module directory**:
   ```bash
   mkdir -p new-app/src/{commonMain,androidMain,iosMain}/kotlin
   ```

2. **Create build.gradle.kts**:
   ```kotlin
   plugins {
       alias(libs.plugins.kotlinMultiplatform)
       alias(libs.plugins.androidApplication)
       alias(libs.plugins.composeMultiplatform)
       alias(libs.plugins.composeCompiler)
   }
   
   kotlin {
       // KMP targets...
       sourceSets {
           commonMain.dependencies {
               implementation(project(":shared-ui"))
               // other deps...
           }
       }
   }
   ```

3. **Update settings.gradle.kts**:
   ```kotlin
   include(":composeApp")
   include(":shared-ui")
   include(":nlt-app")
   include(":new-app")  // Add this line
   ```

4. **Follow Atomic Design principles** using shared-ui components

## Design Principles

### Atomic Design

All applications MUST follow Atomic Design methodology:

1. **Atoms** (shared-ui/atoms): Basic, reusable elements
2. **Molecules** (app-specific or shared): Simple combinations
3. **Organisms** (app-specific): Complex feature components
4. **Templates** (app-specific): Page layouts
5. **Pages** (app-specific): Complete pages with ViewModels

### Theme Consistency

All applications share the **warm color palette**:
- Primary: Warm terracotta (#D4856A)
- Secondary: Soft peach (#E8B4A0)
- Tertiary: Warm beige (#E5D4C1)
- Background: Warm white (#FAF4F0)

**Rule**: Always use `MaterialTheme.colorScheme`, never hardcode colors!

### Code Organization

```
app-module/src/
├── commonMain/kotlin/tokyo/isseikuzumaki/[app]/
│   ├── data/           # Data models, repositories
│   ├── domain/         # Business logic, use cases
│   ├── ui/             # UI components (organisms, templates, pages)
│   │   ├── pages/
│   │   ├── viewmodel/
│   │   └── state/
│   └── [App].kt        # Main app composable
│
├── androidMain/        # Android-specific code
├── iosMain/            # iOS-specific code
└── commonTest/         # Shared tests
```

## Testing

### Run All Tests

```bash
./gradlew test
```

### Run Module-Specific Tests

```bash
./gradlew :composeApp:test
./gradlew :nlt-app:test
./gradlew :shared-ui:test
```

### Coverage Report

```bash
./gradlew koverHtmlReport
```

## Common Issues

### Build Failures

1. **Missing google-services.json** (NLT only):
   - Copy `nlt-app/google-services.json.example`
   - Fill with your Firebase project details
   - Rename to `google-services.json`

2. **AGP version errors**:
   - Ensure network access for dependency downloads
   - Current version: AGP 8.11.2 (in libs.versions.toml)

3. **Shared-ui changes not reflecting**:
   - Clean and rebuild: `./gradlew clean build`

### IDE Setup

1. **Import as Gradle project** in IntelliJ IDEA or Android Studio
2. **Trust the project** when prompted
3. **Sync Gradle** after any build.gradle.kts changes
4. **Invalidate caches** if build issues persist

## Best Practices

### DO ✅

- Use shared-ui components for consistency
- Follow Atomic Design hierarchy
- Add KDoc comments to public APIs
- Write unit tests for business logic
- Use `expect`/`actual` for platform-specific code
- Keep modules focused and cohesive

### DON'T ❌

- Duplicate UI components across apps
- Hardcode colors or dimensions
- Mix business logic with UI code
- Add platform-specific code in commonMain
- Break the Atomic Design hierarchy

## Contributing

1. Create feature branch from `main`
2. Follow existing code structure and conventions
3. Add tests for new functionality
4. Update documentation if needed
5. Submit PR with clear description

## Resources

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Atomic Design](https://atomicdesign.bradfrost.com/)
- [Project README](../README.md)

## Support

For questions or issues:
- Check module-specific README files
- Review docs/ directory
- Create GitHub issue
