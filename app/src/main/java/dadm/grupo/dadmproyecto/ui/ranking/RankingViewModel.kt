package dadm.grupo.dadmproyecto.ui.ranking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dadm.grupo.dadmproyecto.data.db.LocationsVisitedRepository
import dadm.grupo.dadmproyecto.domain.model.RankedUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RankingViewModel @Inject constructor(
    private val locationsVisitedRepository: LocationsVisitedRepository
) : ViewModel() {

    private val _rankingUsers = MutableStateFlow<List<RankedUser>>(emptyList())
    val rankingUsers: StateFlow<List<RankedUser>> = _rankingUsers.asStateFlow()

    // Add loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadRanking()
    }

    fun loadRanking() {
        viewModelScope.launch {
            _isLoading.value = true // Start loading
            try {
                val userVisitCounts = locationsVisitedRepository.getUsersOrderedByLocationsVisited()
                val rankedUsers = userVisitCounts.mapIndexed { index, user ->
                    RankedUser(
                        userId = user.userId,
                        displayName = user.displayName,
                        bio = "",
                        position = index + 1,
                        visitCount = user.visitCount
                    )
                }
                _rankingUsers.value = rankedUsers
            } finally {
                _isLoading.value = false // Stop loading regardless of success/failure
            }
        }
    }

}
