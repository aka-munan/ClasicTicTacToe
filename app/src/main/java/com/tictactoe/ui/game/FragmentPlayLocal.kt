package com.tictactoe.ui.game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tictactoe.R
import com.tictactoe.databinding.FragmentPlayLocalBinding
import com.tictactoe.databinding.PlaygroundItemBinding
import com.tictactoe.domain.models.LocalPlayingGameState
import com.tictactoe.ui.home.divider

class FragmentPlayLocal : Fragment() {
    private lateinit var binding: FragmentPlayLocalBinding
    private val viewModel: PlayLocalViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayLocalBinding.inflate(inflater, container, false)
        binding.touchBlocker.setOnTouchListener { v, _ -> true }
        binding.playground.apply {
            viewModel.gridSize = resources.getInteger(R.integer.grid_size)
            viewModel.gameState.observe(viewLifecycleOwner) {
                when (it) {
                    is LocalPlayingGameState.Finished -> {
                        binding.infoText.text = it.winner
                    }

                    LocalPlayingGameState.OTurn -> {
                        binding.infoText.text = context.getString(R.string.o_turn)
                    }

                    LocalPlayingGameState.XTurn -> {
                        binding.infoText.text = context.getString(R.string.x_turn)
                    }
                }
                binding.touchBlocker.visibility =if (it is LocalPlayingGameState.Finished) View.VISIBLE else View.GONE
            }
            addPlayground(this, inflater, viewModel.gridSize) { x, y ->
                viewModel.toggleTurn(x, y)
            }
        }
        binding.newGameBtn.setOnClickListener {
            viewModel.newGame()
            binding.playground.apply {
                removeAllViews()
                addPlayground(this, inflater, viewModel.gridSize) { x, y ->
                    viewModel.toggleTurn(x, y)
                }
            }
        }
        return binding.root
    }

    private fun addPlayground(
        parent: ViewGroup,
        inflater: LayoutInflater,
        gridSize: Int,
        onItemClicked: ((Int, Int) -> Int)? = null
    ) {
        for (i in 0 until gridSize) {
            val ll = LinearLayout(context)
            ll.orientation = LinearLayout.HORIZONTAL
            ll.layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            val dividerSize = resources.getDimensionPixelSize(R.dimen.divider_size_grid)
            for (j in 0 until gridSize) {
                val itemBinding = PlaygroundItemBinding.inflate(inflater)
                itemBinding.root.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f
                    )
                    setOnClickListener {
                        val action = onItemClicked?.invoke(i, j)
                        if (action == 0) {
                            itemBinding.image.setImageResource(R.drawable.circle)
                        } else if (action == 1) {
                            itemBinding.image.setImageResource(R.drawable.cross)
                        } else {
                            itemBinding.image.setImageResource(0)
                        }
                        itemBinding.root.isEnabled = action == -1
                    }
                }
                ll.addView(itemBinding.root)
                if (j != gridSize - 1)
                    ll.addView(requireContext().divider(false, dividerSize))
            }
            parent.addView(ll)
            if (i != gridSize - 1)
                parent.addView(requireContext().divider(true, dividerSize))
        }
    }
}