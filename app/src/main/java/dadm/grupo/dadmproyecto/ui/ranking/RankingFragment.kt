package dadm.grupo.dadmproyecto.ui.ranking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.data.auth.AuthRepository
import dadm.grupo.dadmproyecto.databinding.FragmentRankingBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RankingFragment : Fragment(R.layout.fragment_ranking) {
    private var _binding: FragmentRankingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RankingViewModel by viewModels()

    @Inject
    lateinit var authRepository: AuthRepository

    private lateinit var rankingAdapter: RankingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.rankingUsers.collect { rankedUsers ->
                rankingAdapter.submitList(rankedUsers)

                val currentUserId = authRepository.getCurrentUser()?.id
                val userPosition = rankedUsers.indexOfFirst { it.userId == currentUserId } + 1

                if (userPosition > 0) {
                    binding.tvUserPosition.text =
                        getString(R.string.current_user_position, userPosition)
                } else {
                    binding.tvUserPosition.text = getString(R.string.user_not_ranked)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRankingBinding.inflate(inflater, container, false)

        setupRecyclerView()

        return binding.root
    }

    private fun setupRecyclerView() {
        rankingAdapter = RankingAdapter()
        binding.rvRanking.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rankingAdapter
            itemAnimator = DefaultItemAnimator()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
