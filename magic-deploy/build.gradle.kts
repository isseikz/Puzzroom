plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "tokyo.isseikuzumaki"
version = "1.0.4"

gradlePlugin {
    plugins {
        create("magicDeploy") {
            id = "tokyo.isseikuzumaki.magic-deploy"
            implementationClass = "tokyo.isseikuzumaki.magicdeploy.MagicDeployPlugin"
            displayName = "Magic Deploy Plugin"
            description = "Automatically notifies the APK path via nc to support Magic Deploy."
        }
    }
}

dependencies {
    implementation(gradleApi())
}