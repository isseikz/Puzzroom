<!--
SYNC IMPACT REPORT
==================
Version Change: 1.0.0 → 1.1.0 (MINOR - new principle added)

Modified Principles: None

Added Sections:
- Principle VI: Worktree-First Development
- Quality Gate #6: Worktree Isolation

Removed Sections: None

Templates Requiring Updates:
- .specify/templates/plan-template.md: ✅ Compatible (no changes needed - workflow principle)
- .specify/templates/spec-template.md: ✅ Compatible (no changes needed - workflow principle)
- .specify/templates/tasks-template.md: ✅ Compatible (no changes needed - workflow principle)

Follow-up TODOs: None
-->

# Puzzroom Constitution

## Core Principles

### I. Atomic Design Architecture

All UI components MUST follow the Atomic Design hierarchy without exception.

**Rules**:
- Components are organized strictly: Atoms → Molecules → Organisms → Templates → Pages
- Atoms (`ui/atoms/`): Indivisible elements with no business logic (AppButton, AppText, AppIcon, AppSpacer, AppCard)
- Molecules (`ui/molecules/`): Combine 2-3 atoms for a single purpose with minimal logic
- Organisms (`ui/organisms/`): Feature-specific sections combining molecules and atoms
- Templates (`ui/templates/`): Layout structures receiving content via parameters
- Pages (`ui/pages/`): Complete pages integrating ViewModels and state management
- Bypass of hierarchy levels is prohibited (e.g., Pages directly using Atoms)

**Rationale**: Ensures scalable, maintainable component structure across a multi-app monorepo.

### II. Kotlin Multiplatform Integrity

Code MUST maintain platform independence in `commonMain` while properly isolating platform-specific implementations.

**Rules**:
- Shared business logic resides exclusively in `commonMain`
- Platform-specific code uses `expect`/`actual` declarations in appropriate source sets (`androidMain`, `iosMain`, `jsMain`, `jvmMain`)
- No platform-specific imports or dependencies in common code
- All apps in the monorepo (`composeApp`, `nlt-app`, `quick-deploy-app`) share `shared-ui` components

**Rationale**: Enables true code sharing across Android, iOS, Web, and Desktop while respecting platform capabilities.

### III. Theme Consistency

All visual styling MUST use the established warm color theme via MaterialTheme.

**Rules**:
- Colors accessed exclusively via `MaterialTheme.colorScheme` properties
- No hardcoded color values in component implementations
- Primary: Warm terracotta (#D4856A)
- Secondary: Soft peach (#E8B4A0)
- Tertiary: Warm beige (#E5D4C1)
- Background: Warm white (#FAF4F0)
- Typography uses theme-defined styles only

**Rationale**: Maintains cohesive user experience across the calm, comfortable visual identity.

### IV. Unidirectional Data Flow

State management MUST follow the State Down / Events Up pattern.

**Rules**:
- State flows: ViewModel → Pages → Templates → Organisms → Molecules → Atoms
- User interactions propagate upward through callbacks to ViewModel
- ViewModels own business logic and application state
- UI components remain pure and stateless where possible
- Local UI state (`remember`/`mutableStateOf`) permitted only for transient presentation concerns

**Rationale**: Predictable state management prevents UI bugs and simplifies debugging.

### V. Simplicity First

Solutions MUST be the simplest that satisfy requirements. Complexity requires explicit justification.

**Rules**:
- YAGNI: Do not implement features until explicitly needed
- Start with the smallest component level required, compose upward
- Avoid premature abstraction; three similar lines of code are better than an unnecessary helper
- Every violation of simplicity MUST be documented in Complexity Tracking sections
- Refactoring and cleanup beyond immediate scope requires user approval

**Rationale**: Reduces maintenance burden and keeps the codebase approachable.

### VI. Worktree-First Development

All feature development MUST occur in isolated git worktrees, not in the main repository directory.

**Rules**:
- New features MUST be developed in `worktrees/<feature-name>/` directory
- Worktree naming: Use branch name with slashes replaced by hyphens (e.g., `feature/foo` → `foo/`)
- `local.properties` MUST be symlinked from root to worktree before Gradle builds
- Feature specification via `/specify` command MUST precede code implementation
- Main repository directory reserved for: releases, hotfixes, and administrative tasks

**Setup Commands**:
```bash
# Create worktree for new feature
git worktree add -b feature/<name> worktrees/<name> main

# Symlink local.properties
ln -s ../../local.properties worktrees/<name>/local.properties
```

**Rationale**: Isolates experimental work, enables parallel feature development, and keeps the main directory stable for builds and releases.

## Development Standards

**Naming Conventions**:
- Atoms: `App[Element].kt` (e.g., `AppButton.kt`)
- Molecules: `[Descriptive][Purpose].kt` (e.g., `IconWithLabel.kt`)
- Organisms: `[Feature][Component].kt` (e.g., `ProjectCardItem.kt`)
- Templates: `[Purpose]Template.kt` (e.g., `ListScreenTemplate.kt`)
- Pages: `[Feature]Page.kt` (e.g., `ProjectListPage.kt`)

**Documentation**:
- KDoc comments required for all public components
- All parameters MUST be documented
- Usage examples required for Organisms and above
- README updates required when introducing new patterns

**Platform Targets**:
- Android (primary)
- iOS
- Web (Wasm and JS)
- Desktop (JVM)

## Quality Gates

All changes MUST pass these gates before merge:

1. **Atomic Design Compliance**: Component placed in correct hierarchy level with proper naming
2. **Theme Usage**: No hardcoded colors; all styling via MaterialTheme
3. **Platform Independence**: Common code contains no platform-specific imports
4. **Data Flow**: State and events follow unidirectional pattern
5. **Simplicity Check**: No unnecessary complexity introduced
6. **Worktree Isolation**: Feature development occurs in dedicated worktree, not main directory

## Governance

**Supremacy**: This constitution supersedes all other practices. Conflicts MUST be resolved in favor of constitutional principles.

**Amendment Process**:
1. Document proposed change with rationale
2. Analyze backward compatibility impact
3. Create migration plan for any breaking changes
4. Obtain PR approval from project maintainer
5. Update version according to semantic versioning

**Version Policy**:
- MAJOR: Principle removal, redefinition, or backward-incompatible governance changes
- MINOR: New principle added or existing principle materially expanded
- PATCH: Clarifications, wording improvements, non-semantic refinements

**Compliance Review**:
- All PRs MUST verify constitutional compliance
- Complexity violations MUST be justified in Complexity Tracking
- Runtime development guidance in `.github/copilot-instructions.md`

**Version**: 1.1.0 | **Ratified**: 2026-02-07 | **Last Amended**: 2026-02-07
