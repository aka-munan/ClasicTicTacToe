package com.tictactoe.ui.game

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tictactoe.R
import com.tictactoe.databinding.FragmentPlayOnlineBinding
import com.tictactoe.databinding.PlaygroundItemBinding
import com.tictactoe.domain.models.GameState
import com.tictactoe.ui.home.divider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FragmentPlayOnline : Fragment() {
    private val viewmodel: PlayOnlineViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentPlayOnlineBinding.inflate(inflater, container, false)
        binding.touchBlocker.setOnTouchListener { v, event -> true }
        showDialog()
        binding.apply {
            newGameBtn.setOnClickListener {
                viewmodel.newGame()
            }
            viewmodel.gameState.observe(viewLifecycleOwner) {
                newGameBtn.visibility = if(it is GameState.Finished || it is GameState.Error) View.VISIBLE else View.GONE
                when (it) {
                    is GameState.Error -> {
                        touchBlocker.visibility = View.VISIBLE
                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                    }

                    is GameState.Finished -> {
                        infoText.text = it.winner
                        touchBlocker.visibility = View.VISIBLE
                        viewmodel.removeListener()
                    }

                    is GameState.Matched -> {
                        player1.text =
                            if (viewmodel.gameInfo!!.currentPlayer == 0) viewmodel.gameInfo!!.opponent.name else "You"
                        player2.text =
                            if (viewmodel.gameInfo!!.currentPlayer == 1) viewmodel.gameInfo!!.opponent.name else "You"
                    }

                    is GameState.XTurn -> {
                        infoText.text = getString(R.string.x_turn)
                        touchBlocker.visibility =
                            if (viewmodel.gameInfo!!.currentPlayer == 1) View.GONE else View.VISIBLE
                    }

                    is GameState.Searching -> {
                        touchBlocker.visibility = View.VISIBLE
                        infoText.text = getString(R.string.searching_for_opponent)
                    }

                    is GameState.OTurn -> {
                        infoText.text = getString(R.string.o_turn)
                        touchBlocker.visibility =
                            if (viewmodel.gameInfo!!.currentPlayer == 1) View.VISIBLE else View.GONE
                    }
                }
            }
        }
        binding.playground.apply {
            val itemBindings = addPlayground(this, inflater, viewmodel.gridSize) { x, y ->
                viewmodel.toggleTurn(x, y)
            }
            viewmodel.matrix.observe(viewLifecycleOwner) {
                it.forEachIndexed { x, ints ->
                    ints.forEachIndexed { y, i ->
                        itemBindings[x][y].apply {
                            image.setImageResource(
                                when (i) {
                                    0 -> R.drawable.circle
                                    1 -> R.drawable.cross
                                    else -> 0
                                }
                            )
                            root.isEnabled = i==-1
                        }
                    }
                }
            }
        }
        return binding.root
    }

    private fun addPlayground(
        parent: ViewGroup,
        inflater: LayoutInflater,
        gridSize: Int,
        onItemClicked: ((Int, Int) -> Unit)? = null
    ): Array<Array<PlaygroundItemBinding>> {
        val bindings =
            Array(gridSize) { Array(gridSize) { PlaygroundItemBinding.inflate(inflater) } }
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
                val itemBinding = bindings[i][j]
                itemBinding.root.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f
                    )
                    setOnClickListener {
                         onItemClicked?.invoke(i, j)
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
        return bindings
    }

    private fun showDialog(){
       val dialog=  MaterialAlertDialogBuilder(requireContext()).apply {
           setTitle("Finding Match...")
//           setIcon(R.drawable.play_arrow)
           setMessage("Please wait for your opponent to join the game")
           setCancelable(false)
        }.show()
        viewmodel.viewModelScope.launch {
            while (viewmodel.gameState.value is GameState.Searching){
                val diff=System.currentTimeMillis()- (viewmodel.gameState.value as GameState.Searching).waitTime
                val waitTimeSec = diff/1000
                Log.i("","showDialog: $waitTimeSec")
                val timeDiff = if (waitTimeSec<60) "$waitTimeSec s" else "${waitTimeSec/60} min"
                dialog.setTitle("Finding Match... $timeDiff")
                delay(1000)
            }
            dialog.dismiss()
        }
    }
}