package dadm.grupo.dadmproyecto.ui.destinationmap

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dadm.grupo.dadmproyecto.databinding.FragmentDestinationMapBinding
import dadm.grupo.dadmproyecto.ui.AuthActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DestinationMapFragment : Fragment() {

    private var _binding: FragmentDestinationMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DestinationMapViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDestinationMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userData.collect { user ->
                        user?.let {
                            binding.tvFragmentDestinationMap.text = buildString {
                                appendLine("Información del usuario:")
                                appendLine("Email: ${viewModel.getUserEmail()}")
                                appendLine("Nombre: ${viewModel.getUserDisplayName()}")
                                appendLine("ID: ${viewModel.getUserId()}")
                            }
                        }
                    }
                }

                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            binding.tvFragmentDestinationMap.text = it
                        }
                    }
                }

                launch {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            is DestinationMapViewModel.NavigationEvent.NavigateToAuth -> {
                                navigateToAuth()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            viewModel.logout()
        }
    }

    private fun navigateToAuth() {
        Intent(requireActivity(), AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }.also { intent ->
            startActivity(intent)
            requireActivity().finish()
        }
    }
}
