package com.tictactoe.ui.name

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.tictactoe.R
import com.tictactoe.data.repository.AuthRepo
import com.tictactoe.databinding.FragmentSetNameBinding

class SetNameFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val viewModel: AuthViewModel by viewModels{
            AuthViewModelFactory(AuthRepo())
        }
        val binding = FragmentSetNameBinding.inflate(inflater, container, false)
        binding.apply {
            val chaneNameResultObserver = Observer<ChangeNameResult> {
               val text =  when (it) {
                    is ChangeNameResult.Error -> {
                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show();
                        null
                    }
                    ChangeNameResult.InProgress -> "Updating name"
                    ChangeNameResult.Success -> {
                        Toast.makeText(context, "logged in", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.nav_home)
                        null
                    }
                }
                submitBtn.text = text?: ContextCompat.getString(requireContext(), R.string.submit_name)
                //submitBtn.text = text?: getString(context, R.string.)
                submitBtn.isEnabled = it !is ChangeNameResult.InProgress
            }
            submitBtn.setOnClickListener {
                if (viewModel.isSignedIn()){
                    viewModel.changeName(usernameInputLayout.editText?.text.toString()).observe(viewLifecycleOwner,chaneNameResultObserver)
                }else{
                    viewModel.signInAnonymously().observe(viewLifecycleOwner){
                        submitBtn.isEnabled = false
                        viewModel.changeName(usernameInputLayout.editText?.text.toString()).observe(viewLifecycleOwner,chaneNameResultObserver)
                        submitBtn.isEnabled = true
                    }
                }
            }
        }
        return binding.root
    }
}