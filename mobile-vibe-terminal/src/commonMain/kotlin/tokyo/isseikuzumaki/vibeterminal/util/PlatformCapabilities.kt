package tokyo.isseikuzumaki.vibeterminal.util

/**
 * True if the current platform supports Magic Deploy (APK install from SSH SFTP).
 * Only Android supports this feature.
 */
expect val isMagicDeploySupported: Boolean
