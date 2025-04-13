package dadm.grupo.dadmproyecto.ui.ranking

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.databinding.FragmentRankingBinding
import dadm.grupo.dadmproyecto.domain.model.Location
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RankingFragment : Fragment(R.layout.fragment_ranking) {
    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var supabaseClient: SupabaseClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingBinding.inflate(inflater, container, false)


// Add this above with other imports

        // In onCreateView:

// In RankingFragment.kt
        lifecycleScope.launch {
            try {

                // Then try decoding
                val locations = supabaseClient
                    .from("locations")
                    .select(Columns.ALL)
                    .decodeList<Location>()

                Log.d("RankingFragment", "Locations: $locations")
            } catch (e: Exception) {
                Log.e("RankingFragment", "Error fetching locations", e)
            }
        }


        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
