package tokyo.isseikuzumaki.puzzroom.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureCategory
import tokyo.isseikuzumaki.puzzroom.domain.FurnitureTemplate

/**
 * カテゴリ名を日本語で取得
 */
fun FurnitureCategory.displayName(): String = when (this) {
    FurnitureCategory.LIVING -> "リビング"
    FurnitureCategory.BEDROOM -> "寝室"
    FurnitureCategory.KITCHEN -> "キッチン"
    FurnitureCategory.DINING -> "ダイニング"
    FurnitureCategory.BATHROOM -> "バスルーム"
    FurnitureCategory.OFFICE -> "オフィス"
    FurnitureCategory.CUSTOM -> "カスタム"
}

/**
 * 家具ライブラリ選択パネル
 */
@Composable
fun FurnitureLibraryPanel(
    templates: List<FurnitureTemplate>,
    selectedTemplate: FurnitureTemplate?,
    onTemplateSelected: (FurnitureTemplate) -> Unit,
    onCreateCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<FurnitureCategory?>(null) }

    Column(modifier = modifier) {
        Text(
            "家具を選択",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        // カテゴリフィルター
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("すべて") }
            )
            FurnitureCategory.entries.forEach { category ->
                val count = templates.count { it.category == category }
                if (count > 0) {
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text("${category.displayName()} ($count)") }
                    )
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // 家具リスト
        val filteredTemplates = if (selectedCategory != null) {
            templates.filter { it.category == selectedCategory }
        } else {
            templates
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredTemplates) { template ->
                FurnitureTemplateCard(
                    template = template,
                    isSelected = template == selectedTemplate,
                    onClick = { onTemplateSelected(template) }
                )
            }
        }

        // カスタム家具作成ボタン
        Button(
            onClick = onCreateCustom,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("+ カスタム家具を作成")
        }
    }
}

/**
 * 家具テンプレートカード
 */
@Composable
fun FurnitureTemplateCard(
    template: FurnitureTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    template.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    template.category.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "サイズ: ${template.width.value}cm × ${template.depth.value}cm",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
