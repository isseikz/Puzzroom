package tokyo.isseikuzumaki.quickdeploy.ui.guide

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tokyo.isseikuzumaki.shared.ui.PreviewTemplate
import tokyo.isseikuzumaki.shared.ui.molecules.CodeBlockWithCopy
import tokyo.isseikuzumaki.shared.ui.molecules.SectionContent
import tokyo.isseikuzumaki.shared.ui.molecules.SectionTitle
import tokyo.isseikuzumaki.shared.ui.molecules.StepCard
import tokyo.isseikuzumaki.shared.ui.molecules.SubsectionTitle
import tokyo.isseikuzumaki.shared.ui.molecules.TipCard

/**
 * Usage guide screen (C-006)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    
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
                content = "発行されたアップロードURL取得エンドポイントをコピーします。このURLに対してPOSTリクエストを送ると、署名付きアップロードURLが取得できます。"
            )

            Spacer(modifier = Modifier.height(12.dp))

            StepCard(
                stepNumber = "3",
                title = "ビルドスクリプトに追加",
                content = "GitHub ActionsやビルドスクリプトにAPKアップロード処理を追加します。デバイストークンを使って署名付きURL取得→APKアップロード→通知の3ステップで完了します。"
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
            
            val githubActionsCode = """
                - name: Deploy APK to Quick Deploy
                  env:
                    SECRET_QUICK_DEPLOY_TOKEN: ${'$'}{{ secrets.SECRET_QUICK_DEPLOY_TOKEN }}
                  run: |
                    # Step 1: Build APK
                    ./gradlew assembleDebug
                    
                    # Step 2: Get signed upload URL
                    UPLOAD_URL_ENDPOINT="https://getuploadurl-o45ehp4r5q-uc.a.run.app/upload/${'$'}SECRET_QUICK_DEPLOY_TOKEN/url"
                    UPLOAD_RESPONSE=${'$'}(curl -s -w "\n%{http_code}" -X POST "${'$'}UPLOAD_URL_ENDPOINT")
                    SIGNED_URL=${'$'}(echo "${'$'}UPLOAD_RESPONSE" | sed '${'$'}d' | jq -r '.uploadUrl')
                    
                    # Step 3: Upload APK to Firebase Storage
                    curl -s -w "\n%{http_code}" -X PUT \
                      -H "Content-Type: application/vnd.android.package-archive" \
                      --data-binary "@app/build/outputs/apk/debug/app-debug.apk" \
                      "${'$'}SIGNED_URL"
                    
                    # Step 4: Notify device
                    NOTIFY_URL="https://notifyuploadcomplete-o45ehp4r5q-uc.a.run.app/upload/${'$'}SECRET_QUICK_DEPLOY_TOKEN/notify"
                    curl -s -X POST -H "Content-Type: application/json" "${'$'}NOTIFY_URL"
            """.trimIndent()
            
            CodeBlockWithCopy(
                code = githubActionsCode,
                onCopy = { copyToClipboard(context, githubActionsCode, "GitHub Actionsのコードをコピーしました") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SubsectionTitle("コマンドラインの場合")
            
            val commandLineCode = """
                #!/bin/bash
                # Quick Deploy - Manual deployment script
                
                # Set your device token
                DEVICE_TOKEN="your-device-token-here"
                APK_PATH="path/to/your/app.apk"
                
                # Step 1: Get signed upload URL
                UPLOAD_URL_ENDPOINT="https://getuploadurl-o45ehp4r5q-uc.a.run.app/upload/${'$'}DEVICE_TOKEN/url"
                UPLOAD_RESPONSE=${'$'}(curl -s -w "\n%{http_code}" -X POST "${'$'}UPLOAD_URL_ENDPOINT")
                SIGNED_URL=${'$'}(echo "${'$'}UPLOAD_RESPONSE" | sed '${'$'}d' | jq -r '.uploadUrl')
                
                # Step 2: Upload APK to Firebase Storage
                curl -s -w "\n%{http_code}" -X PUT \
                  -H "Content-Type: application/vnd.android.package-archive" \
                  --data-binary "@${'$'}APK_PATH" \
                  "${'$'}SIGNED_URL"
                
                # Step 3: Notify device
                NOTIFY_URL="https://notifyuploadcomplete-o45ehp4r5q-uc.a.run.app/upload/${'$'}DEVICE_TOKEN/notify"
                curl -s -X POST -H "Content-Type: application/json" "${'$'}NOTIFY_URL"
                
                echo "✓ Deployment completed!"
            """.trimIndent()
            
            CodeBlockWithCopy(
                code = commandLineCode,
                onCopy = { copyToClipboard(context, commandLineCode, "コマンドラインのコードをコピーしました") }
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

@Preview
@Composable
private fun GuideScreenPreview() {
    PreviewTemplate {
        GuideScreen(onNavigateBack = {})
    }
}

/**
 * Helper function to copy text to clipboard
 */
private fun copyToClipboard(context: Context, text: String, message: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Code", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
