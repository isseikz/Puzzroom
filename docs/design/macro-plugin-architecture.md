# Macro Plugin Architecture Design Document

## Overview

This document describes the architecture for a customizable macro tab system that enables users to create, organize, and share macro configurations. The long-term vision includes a marketplace feature similar to LINE stickers, where users can buy and sell macro packs.

## Goals

### Short-term Goals
- Allow users to create custom macros
- Enable tab reordering and customization
- Persist user-created macros locally
- Support import/export of macro configurations

### Long-term Goals
- Build a macro marketplace platform
- Enable creators to publish and monetize macro packs
- Implement user reviews and ratings
- Support versioning and updates for published macro packs

## Current Architecture Analysis

### Existing Components
```
MacroDefinition.kt          # Data models (MacroTab enum, MacroKey, MacroAction)
MacroButton.kt              # Individual button UI
MacroButtonGrid.kt          # Button grid layout
MacroTabRow.kt              # Tab selector
MacroInputPanel.kt          # Complete control panel
TerminalScreenModel.kt      # State management
```

### Current Limitations
- Macro definitions are hardcoded in `MacroConfig` object
- Fixed 4 tabs (BASIC, NAV, VIM, FUNCTION) as enum
- No persistence mechanism
- No user customization capability

## Proposed Architecture

### 1. Core Domain Model

```
┌─────────────────────────────────────────────────────────────────┐
│                        MacroPack                                │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  id: UUID                                                │   │
│  │  metadata: MacroPackMetadata                             │   │
│  │  tabs: List<MacroTabDefinition>                          │   │
│  │  source: MacroPackSource (Builtin | User | Marketplace)  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              MacroTabDefinition                          │   │
│  │  ┌────────────────────────────────────────────────────┐  │   │
│  │  │  id: UUID                                          │  │   │
│  │  │  name: String                                      │  │   │
│  │  │  icon: String? (emoji or icon reference)           │  │   │
│  │  │  keys: List<MacroKeyDefinition>                    │  │   │
│  │  │  displayOrder: Int                                 │  │   │
│  │  └────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                              │                                  │
│                              ▼                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              MacroKeyDefinition                          │   │
│  │  ┌────────────────────────────────────────────────────┐  │   │
│  │  │  id: UUID                                          │  │   │
│  │  │  label: String                                     │  │   │
│  │  │  action: MacroAction                               │  │   │
│  │  │  description: String?                              │  │   │
│  │  │  displayOrder: Int                                 │  │   │
│  │  └────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 2. Package Structure

```
vibeterminal/
├── macro/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── MacroPack.kt           # Core domain entities
│   │   │   ├── MacroTabDefinition.kt
│   │   │   ├── MacroKeyDefinition.kt
│   │   │   ├── MacroAction.kt         # Existing, enhanced
│   │   │   └── MacroPackSource.kt     # Builtin/User/Marketplace
│   │   └── repository/
│   │       └── MacroRepository.kt     # Repository interface
│   │
│   ├── data/
│   │   ├── local/
│   │   │   ├── MacroLocalDataSource.kt
│   │   │   ├── MacroDatabase.kt       # Room/SQLDelight
│   │   │   └── entity/
│   │   │       ├── MacroPackEntity.kt
│   │   │       ├── MacroTabEntity.kt
│   │   │       └── MacroKeyEntity.kt
│   │   ├── builtin/
│   │   │   └── BuiltinMacroPacks.kt   # Default macro packs
│   │   └── MacroRepositoryImpl.kt
│   │
│   ├── usecase/
│   │   ├── GetActiveMacroPacksUseCase.kt
│   │   ├── CreateMacroPackUseCase.kt
│   │   ├── UpdateMacroPackUseCase.kt
│   │   ├── DeleteMacroPackUseCase.kt
│   │   ├── ReorderTabsUseCase.kt
│   │   ├── ReorderKeysUseCase.kt
│   │   ├── ExportMacroPackUseCase.kt
│   │   └── ImportMacroPackUseCase.kt
│   │
│   ├── ui/
│   │   ├── components/
│   │   │   ├── MacroButton.kt         # Existing, keep
│   │   │   ├── MacroButtonGrid.kt     # Updated for dynamic macros
│   │   │   ├── MacroTabRow.kt         # Updated for dynamic tabs
│   │   │   ├── MacroInputPanel.kt     # Updated
│   │   │   └── editor/
│   │   │       ├── MacroPackEditor.kt
│   │   │       ├── MacroTabEditor.kt
│   │   │       ├── MacroKeyEditor.kt
│   │   │       └── MacroActionPicker.kt
│   │   └── viewmodel/
│   │       ├── MacroViewModel.kt
│   │       └── MacroEditorViewModel.kt
│   │
│   └── serialization/
│       ├── MacroPackSerializer.kt     # JSON serialization
│       └── MacroPackFormat.kt         # Format version handling
```

### 3. Data Persistence Layer

#### Database Schema (SQLDelight/Room)

```sql
-- Macro Packs table
CREATE TABLE macro_packs (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    version TEXT NOT NULL DEFAULT '1.0.0',
    author TEXT,
    source TEXT NOT NULL DEFAULT 'user',  -- 'builtin', 'user', 'marketplace'
    marketplace_id TEXT,                   -- Reference to marketplace item
    is_active INTEGER NOT NULL DEFAULT 1,
    display_order INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Macro Tabs table
CREATE TABLE macro_tabs (
    id TEXT PRIMARY KEY NOT NULL,
    pack_id TEXT NOT NULL REFERENCES macro_packs(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    icon TEXT,
    display_order INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Macro Keys table
CREATE TABLE macro_keys (
    id TEXT PRIMARY KEY NOT NULL,
    tab_id TEXT NOT NULL REFERENCES macro_tabs(id) ON DELETE CASCADE,
    label TEXT NOT NULL,
    action_type TEXT NOT NULL,     -- 'direct_send' or 'buffer_insert'
    action_value TEXT NOT NULL,    -- The sequence or text
    description TEXT,
    display_order INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- User preferences for tab ordering
CREATE TABLE user_tab_order (
    id INTEGER PRIMARY KEY,
    tab_id TEXT NOT NULL,
    global_order INTEGER NOT NULL,
    is_visible INTEGER NOT NULL DEFAULT 1
);
```

### 4. State Management

```kotlin
// MacroState for ViewModel
data class MacroState(
    val activePacks: List<MacroPack> = emptyList(),
    val allTabs: List<MacroTabDefinition> = emptyList(),  // Flattened, ordered
    val selectedTabIndex: Int = 0,
    val isEditing: Boolean = false,
    val editingPack: MacroPack? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

// MacroIntent for user actions
sealed class MacroIntent {
    data class SelectTab(val index: Int) : MacroIntent()
    data class ExecuteMacro(val key: MacroKeyDefinition) : MacroIntent()
    object OpenEditor : MacroIntent()
    data class CreatePack(val name: String) : MacroIntent()
    data class ReorderTabs(val fromIndex: Int, val toIndex: Int) : MacroIntent()
    data class ReorderKeys(val tabId: String, val fromIndex: Int, val toIndex: Int) : MacroIntent()
    data class ImportPack(val json: String) : MacroIntent()
    data class ExportPack(val packId: String) : MacroIntent()
    data class DeletePack(val packId: String) : MacroIntent()
}
```

### 5. Serialization Format

For import/export and future marketplace compatibility:

```json
{
  "format_version": "1.0",
  "pack": {
    "id": "uuid-string",
    "metadata": {
      "name": "My Custom Macros",
      "description": "Useful macros for Docker development",
      "version": "1.0.0",
      "author": "username",
      "tags": ["docker", "devops"],
      "license": "MIT",
      "created_at": "2024-01-15T10:30:00Z",
      "updated_at": "2024-01-15T10:30:00Z"
    },
    "tabs": [
      {
        "id": "tab-uuid",
        "name": "Docker",
        "icon": "🐳",
        "keys": [
          {
            "id": "key-uuid",
            "label": "ps",
            "action": {
              "type": "buffer_insert",
              "value": "docker ps"
            },
            "description": "List running containers"
          },
          {
            "id": "key-uuid-2",
            "label": "logs",
            "action": {
              "type": "buffer_insert",
              "value": "docker logs -f "
            },
            "description": "Follow container logs"
          }
        ]
      }
    ]
  }
}
```

### 6. Plugin Architecture for Future Marketplace

```
┌─────────────────────────────────────────────────────────────────┐
│                      Macro System Architecture                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐  │
│  │   Builtin   │    │    User     │    │    Marketplace      │  │
│  │   Packs     │    │   Packs     │    │       Packs         │  │
│  └──────┬──────┘    └──────┬──────┘    └──────────┬──────────┘  │
│         │                  │                      │             │
│         └──────────────────┼──────────────────────┘             │
│                            ▼                                    │
│              ┌─────────────────────────┐                        │
│              │    MacroPackLoader      │                        │
│              │   (Unified Interface)   │                        │
│              └────────────┬────────────┘                        │
│                           │                                     │
│                           ▼                                     │
│              ┌─────────────────────────┐                        │
│              │   MacroPackRegistry     │                        │
│              │   (Active Pack Cache)   │                        │
│              └────────────┬────────────┘                        │
│                           │                                     │
│                           ▼                                     │
│              ┌─────────────────────────┐                        │
│              │   MacroTabOrchestrator  │                        │
│              │ (Tab Ordering & Display)│                        │
│              └────────────┬────────────┘                        │
│                           │                                     │
│                           ▼                                     │
│              ┌─────────────────────────┐                        │
│              │      UI Components      │                        │
│              │  (MacroInputPanel etc.) │                        │
│              └─────────────────────────┘                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 7. Marketplace Data Model (Future)

```kotlin
// For future marketplace integration
data class MarketplaceMacroPack(
    val id: String,
    val pack: MacroPack,
    val pricing: Pricing,
    val statistics: PackStatistics,
    val reviews: List<Review>,
    val publishedAt: Instant,
    val updatedAt: Instant
)

data class Pricing(
    val type: PricingType,  // FREE, PAID, SUBSCRIPTION
    val price: BigDecimal?,
    val currency: String?
)

data class PackStatistics(
    val downloads: Int,
    val rating: Float,
    val reviewCount: Int
)
```

## Implementation Plan

### Phase 1: Core Infrastructure (Current Focus)
1. Create new domain models (`MacroPack`, `MacroTabDefinition`, `MacroKeyDefinition`)
2. Implement database layer with SQLDelight
3. Create `MacroRepository` interface and implementation
4. Migrate existing hardcoded macros to builtin pack format
5. Update UI components to use new models

### Phase 2: User Customization
1. Implement macro pack editor UI
2. Add tab reordering with drag-and-drop
3. Add key reordering within tabs
4. Implement import/export functionality

### Phase 3: Enhanced Features
1. Add macro sequencing (multiple actions per key)
2. Implement conditional macros (based on terminal state)
3. Add macro pack templates

### Phase 4: Marketplace (Future)
1. Design marketplace API
2. Implement authentication and user accounts
3. Build publishing workflow
4. Implement purchase and download flow
5. Add review and rating system

## Security Considerations

### Macro Execution Safety
- Validate all macro actions before execution
- Sanitize imported macro packs
- Implement action whitelisting for marketplace packs

### Marketplace Security
- Code signing for published packs
- Content moderation for published packs
- Rate limiting for API access
- Secure payment processing

## Migration Strategy

### From Current to New Architecture
1. Create builtin pack from existing `MacroConfig`
2. Keep existing enum `MacroTab` as reference during transition
3. Add compatibility layer in `MacroInputPanel`
4. Gradually migrate UI components to use new `MacroTabDefinition`
5. Remove old enum-based system after full migration

## API Design Guidelines

### Repository Interface
```kotlin
interface MacroRepository {
    // Pack operations
    fun getAllPacks(): Flow<List<MacroPack>>
    fun getActivePacks(): Flow<List<MacroPack>>
    fun getPackById(id: String): Flow<MacroPack?>
    suspend fun savePack(pack: MacroPack)
    suspend fun deletePack(id: String)

    // Tab operations
    suspend fun updateTabOrder(packId: String, tabIds: List<String>)
    suspend fun updateGlobalTabOrder(tabOrders: List<UserTabOrder>)

    // Key operations
    suspend fun updateKeyOrder(tabId: String, keyIds: List<String>)

    // Import/Export
    suspend fun importPack(json: String): Result<MacroPack>
    suspend fun exportPack(packId: String): Result<String>
}
```

## Conclusion

This architecture provides:
- Flexible macro customization for users
- Clean separation between UI, domain, and data layers
- Extensible design for future marketplace integration
- Safe migration path from current hardcoded system
- Robust serialization format for sharing and marketplace

The plugin-like architecture allows macro packs to be:
- Created locally by users
- Shared via import/export
- Distributed through a future marketplace
- Updated independently without app updates
