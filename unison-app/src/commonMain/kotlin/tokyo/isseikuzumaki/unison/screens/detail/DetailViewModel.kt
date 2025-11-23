package tokyo.isseikuzumaki.unison.screens.detail

import androidx.lifecycle.ViewModel
import com.jetbrains.unison.data.MuseumObject
import com.jetbrains.unison.data.MuseumRepository
import kotlinx.coroutines.flow.Flow

class DetailViewModel(private val museumRepository: MuseumRepository) : ViewModel() {
    fun getObject(objectId: Int): Flow<MuseumObject?> =
        museumRepository.getObjectById(objectId)
}
