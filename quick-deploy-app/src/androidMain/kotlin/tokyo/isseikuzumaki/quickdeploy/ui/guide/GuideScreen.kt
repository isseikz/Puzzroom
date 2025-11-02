package tokyo.isseikuzumaki.quickdeploy.ui.guide

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Usage guide screen (C-006)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("使い方ガイド") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Overview section
            SectionTitle("Quick Deployとは")
            SectionContent(
                """
                Quick Deployは、リモートビルド環境で作成したAPKを、即座にAndroidデバイスにインストールできるツールです。

                GitHub ActionsやSSHサーバーでビルドしたAPKを、数秒でスマホやタブレットに配信できます。
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Setup steps
            SectionTitle("セットアップ手順")

            StepCard(
                stepNumber = "1",
                title = "デバイスを登録",
                content = "アプリを起動して「デバイスを登録」ボタンをタップします。APKアップロード用のURLが発行されます。"
            )

            Spacer(modifier = Modifier.height(12.dp))

            StepCard(
                stepNumber = "2",
                title = "URLをコピー",
                content = "発行されたURLまたはcurlコマンドをコピーします。"
            )

            Spacer(modifier = Modifier.height(12.dp))

            StepCard(
                stepNumber = "3",
                title = "ビルドスクリプトに追加",
                content = "GitHub ActionsやビルドスクリプトにAPKアップロード処理を追加します。"
            )

            Spacer(modifier = Modifier.height(12.dp))

            StepCard(
                stepNumber = "4",
                title = "インストール権限を許可",
                content = "初回のみ、提供元不明のアプリのインストール権限を許可してください。"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Usage example
            SectionTitle("使用例")

            SubsectionTitle("GitHub Actionsの場合")
            CodeBlock(
                """
                - name: Upload to Quick Deploy
                  env:
                    QUICK_DEPLOY_URL: ${'$'}{{ secrets.QUICK_DEPLOY_URL }}
                  run: |
                    curl -X POST \
                      -F "file=@app/build/outputs/apk/debug/app-debug.apk" \
                      "${'$'}QUICK_DEPLOY_URL"
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(16.dp))

            SubsectionTitle("コマンドラインの場合")
            CodeBlock(
                """
                curl -X POST \
                  -F "file=@path/to/your/app.apk" \
                  "https://your-upload-url"
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // How it works
            SectionTitle("動作の流れ")
            SectionContent(
                """
                1. ビルド環境がAPKをアップロード
                2. サーバーがAPKを受信・保存
                3. プッシュ通知をデバイスに送信
                4. 通知をタップしてAPKをダウンロード
                5. インストール画面が自動で開く

                ※ APKは10分後に自動削除されます
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tips
            SectionTitle("ヒント")

            TipCard("🔐 URLは秘密情報として扱ってください。GitHub Secretsなどに保存することをおすすめします。")

            Spacer(modifier = Modifier.height(8.dp))

            TipCard("🔄 新しいURLを発行すると、古いURLは無効になります。")

            Spacer(modifier = Modifier.height(8.dp))

            TipCard("📱 通知が届かない場合、手動で「最新APKをダウンロード＆インストール」ボタンからインストールできます。")

            Spacer(modifier = Modifier.height(8.dp))

            TipCard("⏱️ APKは10分後に自動削除されるため、セキュリティ面でも安心です。")
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SubsectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun SectionContent(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StepCard(stepNumber: String, title: String, content: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stepNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = code,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TipCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
