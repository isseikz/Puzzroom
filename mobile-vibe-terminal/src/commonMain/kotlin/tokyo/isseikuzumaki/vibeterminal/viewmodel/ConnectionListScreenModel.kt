package tokyo.isseikuzumaki.vibeterminal.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch

class ConnectionListScreenModel : ScreenModel {
    init {
        screenModelScope.launch {
            // Initialize data
        }
    }
}
