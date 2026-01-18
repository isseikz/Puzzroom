# Macro DSL Specification

## Overview

This document defines the Domain Specific Language (DSL) for defining macro packs in a human-readable text format. The DSL is designed to be:

- Easy to read and write
- Suitable for marketplace display
- Shareable via copy/paste
- Convertible to/from JSON internal format

## File Extension

`.macro`

## Basic Structure

```
@pack "Pack Name"
@version 1.0.0
@author @username
@description "Description of the pack"
@tags tag1, tag2, tag3
@license MIT
@delay 50ms

## Tab Name

key_label : action
key_label : action  # comment
```

## Metadata Directives

| Directive | Required | Description |
|-----------|----------|-------------|
| `@pack` | Yes | Pack name (quoted string) |
| `@version` | No | Semantic version (default: 1.0.0) |
| `@author` | No | Author name or handle |
| `@description` | No | Pack description (quoted string) |
| `@tags` | No | Comma-separated tags |
| `@license` | No | License type |
| `@delay` | No | Default delay between sequence actions (default: 50ms) |

## Tab Definition

Tabs are defined with `##` followed by tab name:

```
## Tab Name
## 🐳 Docker        # with emoji icon
```

If an emoji is present at the start, it becomes the tab icon.

## Key Definition

```
label : action
label : action  # description
```

- **label**: Display text on the button (no quotes needed unless contains special chars)
- **action**: The action to perform
- **description**: Optional comment after `#`

### Quoted Labels

Use quotes for labels with spaces or special characters:

```
"Save & Quit" : {ESC} :wq {ENTER}
"CTRL+C"      : {C-c}
```

## Action Types

### 1. Text Insert (Buffer)

Plain text is inserted into the buffer (user presses Enter to execute):

```
ps       : docker ps
status   : git status
```

### 2. Direct Send

Text prefixed with `!` is sent immediately:

```
up       : !docker compose up -d
quit     : !exit
```

### 3. Special Keys

Special keys are enclosed in `{}`:

```
escape   : {ESC}
enter    : {ENTER}
tab      : {TAB}
```

**Available special keys:**

| Key | Aliases |
|-----|---------|
| `{ESC}` | `{ESCAPE}` |
| `{ENTER}` | `{RETURN}`, `{CR}` |
| `{TAB}` | |
| `{SPACE}` | `{SP}` |
| `{BACKSPACE}` | `{BS}` |
| `{DELETE}` | `{DEL}` |
| `{UP}` | `{ARROW_UP}` |
| `{DOWN}` | `{ARROW_DOWN}` |
| `{LEFT}` | `{ARROW_LEFT}` |
| `{RIGHT}` | `{ARROW_RIGHT}` |
| `{HOME}` | |
| `{END}` | |
| `{PAGEUP}` | `{PGUP}` |
| `{PAGEDOWN}` | `{PGDN}` |
| `{INSERT}` | `{INS}` |
| `{F1}` - `{F12}` | |

### 4. Modifier Keys

Modifier keys use short notation:

| Notation | Meaning |
|----------|---------|
| `C-` | Ctrl |
| `A-` | Alt |
| `S-` | Shift |
| `M-` | Meta (Super/Win/Cmd) |

**Examples:**

```
copy     : {C-c}           # Ctrl+C
paste    : {C-v}           # Ctrl+V
redo     : {C-S-z}         # Ctrl+Shift+Z
terminal : {C-A-t}         # Ctrl+Alt+T
word_fwd : {A-f}           # Alt+F
```

**Long form also supported:**

```
copy     : {Ctrl+C}
paste    : {Ctrl+V}
redo     : {Ctrl+Shift+Z}
```

### 5. Sequences

Multiple actions separated by space execute in order:

```
save_quit    : {ESC} :wq {ENTER}
force_quit   : {ESC} :q! {ENTER}
new_line     : {ESC} o
```

**Sequence with text and keys:**

```
git_commit   : git commit -m "" {LEFT}
search       : {ESC} /
```

### 6. Delays

Insert delays between actions:

```
slow_save    : {ESC} [100ms] :wq {ENTER}
very_slow    : {C-c} [500ms] {C-c}
```

### 7. Repetition

Repeat an action multiple times:

```
delete_5     : {DEL}*5
left_3       : {LEFT}*3
indent       : {TAB}*4
```

### 8. Cursor Position

Use `|` to indicate where the cursor should be placed:

```
docker_log   : docker logs -f |
git_checkout : git checkout |
search       : :%s/|//g
```

When executed, cursor is positioned at `|` for user input.

## Comments

```
# This is a line comment

key : action  # This is an inline comment
```

## Complete Example

```
@pack "Docker Pro"
@version 2.0.0
@author @isseikz
@description "Professional Docker workflow macros"
@tags docker, devops, container
@license MIT
@delay 50ms

## 🐳 Container

ps           : docker ps                    # List running
"ps -a"      : docker ps -a                 # List all
logs         : docker logs -f |             # Follow logs
stop         : docker stop |
exec         : docker exec -it | /bin/bash

## 📦 Image

images       : docker images
build        : docker build -t |
pull         : docker pull |
prune        : !docker image prune -f       # Direct execute

## 🎼 Compose

up           : !docker compose up -d
down         : !docker compose down
restart      : !docker compose restart
logs         : !docker compose logs -f

## ⌨ Shortcuts

# Ctrl combinations
"Ctrl+C"     : {C-c}
"Ctrl+D"     : {C-d}
"Ctrl+Z"     : {C-z}

# Sequences
clear_quit   : {C-c} [100ms] exit {ENTER}
```

## Grammar (EBNF)

```ebnf
macro_file    = { directive | tab | key_def | comment | blank_line } ;

directive     = "@" directive_name value ;
directive_name = "pack" | "version" | "author" | "description"
               | "tags" | "license" | "delay" ;

tab           = "##" [ emoji ] tab_name ;
emoji         = unicode_emoji ;
tab_name      = text_until_eol ;

key_def       = label ":" action [ "#" comment ] ;
label         = quoted_string | identifier ;
quoted_string = '"' { any_char } '"' ;
identifier    = letter { letter | digit | "_" | "-" } ;

action        = { action_part } ;
action_part   = special_key | modifier_key | delay | repeat | text | cursor ;

special_key   = "{" key_name "}" ;
modifier_key  = "{" [ modifier "-" ] key_name "}" ;
modifier      = "C" | "A" | "S" | "M" | "Ctrl" | "Alt" | "Shift" | "Meta" ;
key_name      = "ESC" | "ENTER" | "TAB" | ... | letter | digit ;

delay         = "[" number "ms" "]" ;
repeat        = action_part "*" number ;
cursor        = "|" ;
text          = { printable_char } ;

comment       = "#" text_until_eol ;
blank_line    = [ whitespace ] newline ;
```

## Conversion to Domain Model

### DSL to Kotlin

```kotlin
// DSL text
val dsl = """
Save : {ESC} :wq {ENTER}
"""

// Converts to:
MacroKeyDefinition(
    id = "generated-uuid",
    label = "Save",
    action = MacroAction.Sequence(
        actions = listOf(
            MacroAction.SpecialKey(Key.ESC),
            MacroAction.BufferInsert(":wq"),
            MacroAction.SpecialKey(Key.ENTER)
        ),
        delayMs = 50
    ),
    description = null,
    displayOrder = 0
)
```

### DSL to JSON

```json
{
  "id": "generated-uuid",
  "label": "Save",
  "action": {
    "type": "sequence",
    "actions": [
      {"type": "special_key", "key": "ESC"},
      {"type": "buffer_insert", "text": ":wq"},
      {"type": "special_key", "key": "ENTER"}
    ],
    "delay_ms": 50
  }
}
```

## Error Handling

Parser should provide helpful error messages:

```
Error at line 15, column 8:
  unknown_key : {INVALID}
               ^^^^^^^^^
  Unknown special key: INVALID
  Did you mean: {ENTER}?
```

## Escaping

| Sequence | Meaning |
|----------|---------|
| `\\` | Literal backslash |
| `\{` | Literal `{` |
| `\}` | Literal `}` |
| `\|` | Literal `|` |
| `\#` | Literal `#` |
| `\"` | Literal `"` in quoted string |

## Compatibility

The DSL format is versioned implicitly through the `@version` directive of the pack. Parsers should:

1. Accept all valid syntax from this specification
2. Warn on deprecated syntax
3. Fail gracefully on unknown directives with helpful messages

## Implementation Priority

1. **P0**: Basic keys, special keys, modifier keys
2. **P0**: Sequences
3. **P1**: Delays, repetition
4. **P1**: Cursor position
5. **P2**: Full error recovery and suggestions
