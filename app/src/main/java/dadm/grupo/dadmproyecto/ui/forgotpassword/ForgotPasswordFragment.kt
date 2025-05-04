package dadm.grupo.dadmproyecto.ui.forgotpassword

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.databinding.FragmentForgotpasswordBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment(R.layout.fragment_forgotpassword) {

    private var _binding: FragmentForgotpasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotpasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnSendResetEmail.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            viewModel.sendPasswordResetEmail(email)
        }

        binding.btnBackToLogin.setOnClickListener {
            findNavController().navigate(R.id.actionForgotPasswordFragmentToLoginFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.resetStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                true -> {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.reset_email_sent_success),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.actionForgotPasswordFragmentToLoginFragment)
                }

                false -> {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.reset_email_sent_failure),
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                null -> {
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
