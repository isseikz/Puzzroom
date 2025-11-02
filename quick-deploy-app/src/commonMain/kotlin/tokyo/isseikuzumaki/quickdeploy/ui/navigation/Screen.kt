package tokyo.isseikuzumaki.quickdeploy.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Screen destinations for navigation
 */
sealed interface Screen {
    @Serializable
    data object Registration : Screen

    @Serializable
    data object Guide : Screen
}
