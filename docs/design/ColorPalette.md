# カラーパレット (Color Palette)

## 暖色系カラースキーム (Warm Color Scheme)

### Primary Colors (プライマリカラー)
暖かいオレンジ系 - メインのアクションに使用

- **WarmOrange** `#E07A5F` 
  - 明るさ: Medium
  - 用途: Primary buttons, active states, emphasis
  
- **WarmOrangeDark** `#C85A3F`
  - 明るさ: Dark
  - 用途: Hover states, dark theme primary
  
- **WarmOrangeLight** `#F2A889`
  - 明るさ: Light
  - 用途: Container backgrounds, light accents

### Secondary Colors (セカンダリカラー)
落ち着いたベージュ系 - サポート要素に使用

- **WarmBeige** `#F4D3B5`
  - 明るさ: Light
  - 用途: Secondary elements, soft backgrounds
  
- **WarmBeigeDark** `#E8C19E`
  - 明るさ: Medium
  - 用途: Borders, dividers
  
- **WarmBeigeLight** `#F8E3CB`
  - 明るさ: Very Light
  - 用途: Subtle backgrounds, highlights

### Tertiary Colors (ターシャリカラー)
アクセント用の暖色

- **WarmTerracotta** `#D4A574`
  - 明るさ: Medium
  - 用途: Tertiary actions, decorative accents
  
- **WarmTerracottaDark** `#B88A5D`
  - 明るさ: Dark
  - 用途: Borders, darker accents
  
- **WarmTerracottaLight** `#E4C09A`
  - 明るさ: Light
  - 用途: Light accents, hover states

### Background Colors (背景色)
全体の雰囲気を作る背景色

- **WarmBackground** `#FFFBF7`
  - 用途: Main background
  - 特徴: 非常に明るく、ほのかに暖かい
  
- **WarmSurface** `#FFF8F2`
  - 用途: Card surfaces, elevated elements
  - 特徴: 背景よりわずかに暖かい
  
- **WarmSurfaceVariant** `#FFF0E5`
  - 用途: Alternative surfaces, subtle emphasis
  - 特徴: より暖色が強い

### Text Colors (テキストカラー)
読みやすさを重視したテキスト色

- **WarmTextPrimary** `#3E2723`
  - 用途: Main text, headings
  - 特徴: 暖かみのある深いブラウン
  
- **WarmTextSecondary** `#5D4037`
  - 用途: Secondary text, descriptions
  - 特徴: やや明るいブラウン
  
- **WarmTextTertiary** `#795548`
  - 用途: Captions, disabled text
  - 特徴: より明るいブラウン

### Status Colors (ステータスカラー)
フィードバック用の色

- **WarmSuccess** `#81C784`
  - 用途: Success messages, confirmations
  - 特徴: 柔らかいグリーン
  
- **WarmWarning** `#FFB74D`
  - 用途: Warnings, cautions
  - 特徴: 暖かいオレンジ
  
- **WarmError** `#E57373`
  - 用途: Errors, destructive actions
  - 特徴: 柔らかいレッド

## Color Usage Guidelines (使用ガイドライン)

### Primary Actions
- ボタン: WarmOrange
- ホバー: WarmOrangeDark
- 無効状態: WarmSurfaceVariant

### Secondary Actions
- ボタン: WarmBeige (outline)
- テキスト: WarmOrange

### Surfaces
- カード: WarmSurface
- ダイアログ: WarmSurface
- 強調カード: WarmSurfaceVariant

### Text Hierarchy
1. タイトル: WarmTextPrimary
2. 本文: WarmTextPrimary
3. キャプション: WarmTextSecondary
4. 無効テキスト: WarmTextTertiary

### Feedback
- 成功: WarmSuccess
- 警告: WarmWarning
- エラー: WarmError

## Accessibility Considerations (アクセシビリティ)

### Contrast Ratios
すべてのテキストカラーと背景色の組み合わせは、WCAG 2.1 AA基準を満たすようにコントラスト比を確保しています。

- WarmTextPrimary on WarmBackground: 高いコントラスト
- WarmTextSecondary on WarmSurface: 十分なコントラスト
- Primary buttons: 明確な視認性

### Color Blind Friendly
暖色系を基調としていますが、以下の点に配慮：
- 状態表示にアイコンも併用
- テキストラベルを常に表示
- コントラストで識別可能

## Dark Theme (ダークテーマ)

ダークテーマでは以下のように色が調整されます：

- Background: `#2C1810` (非常に暗いブラウン)
- Surface: `#3E2723` (暗いブラウン)
- SurfaceVariant: `#4E342E` (やや明るいブラウン)
- OnSurface: WarmBeigeLight
- Primary: WarmOrangeLight (明るめに調整)

## Implementation Example

```kotlin
// テーマの適用
MaterialTheme(
    colorScheme = WarmLightColorScheme
) {
    // UI components
}

// 個別の色を使用
Box(
    modifier = Modifier.background(AppColors.WarmOrange)
)

// MaterialThemeから取得
Text(
    text = "Hello",
    color = MaterialTheme.colorScheme.primary
)
```

## Design Philosophy (デザイン哲学)

この暖色系パレットは以下のコンセプトに基づいています：

1. **温かみ**: オレンジとベージュで親しみやすい雰囲気
2. **落ち着き**: 彩度を抑えて目に優しい
3. **自然**: 大地や木を連想させる色合い
4. **調和**: すべての色が調和して統一感を生む
5. **柔軟性**: 様々な用途に適応可能

この配色により、ユーザーに安心感と快適さを提供し、長時間の使用でも疲れにくいUIを実現します。
