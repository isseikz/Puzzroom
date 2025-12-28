package tokyo.isseikuzumaki.vibeterminal.platform

/**
 * Platform-specific launcher for secondary display functionality.
 *
 * This is an expect/actual pattern:
 * - commonMain: expect function declaration
 * - androidMain: actual implementation using TerminalService
 * - Other platforms: no-op implementation
 *
 * @param context Platform-specific context (Android: Context, others: Unit)
 */
expect fun launchSecondaryDisplay(context: Any?)
