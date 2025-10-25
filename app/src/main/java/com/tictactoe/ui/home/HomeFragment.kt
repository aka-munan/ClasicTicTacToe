package com.tictactoe.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.tictactoe.R
import com.tictactoe.databinding.FragmentHomeBinding
import com.tictactoe.databinding.RecyclerItemBinding
import com.tictactoe.domain.models.AuthState
import com.tictactoe.domain.models.MatchInstance

class HomeFragment : Fragment() {
    val viewModel: HomeViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentHomeBinding.inflate(inflater, container, false)
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                AuthState.Unauthenticated ->
                    findNavController().navigate(R.id.nav_set_name)

                is AuthState.Authenticated -> {
                    viewModel.getPlayerHistory()
                    viewModel.matchHistory.observe(viewLifecycleOwner) {
                        if (it.isEmpty()) {
                            binding.historyRecycler.visibility = View.GONE
                            binding.noHistoryText.visibility = View.VISIBLE
                        } else {
                            binding.noHistoryText.visibility = View.GONE
                            binding.historyRecycler.apply {
                                visibility = View.VISIBLE
                                if (adapter == null){
                                    adapter = HistoryAdapter(it, state.user.id )
                                }else{
                                    (adapter as HistoryAdapter).updateDateSet(it)

                                }
                            }

                        }
                    }
                }

                else -> {}
            }
        }
        binding.fabPlay.setOnClickListener {
            if (viewModel.authState.value is AuthState.Authenticated) {
                showMenu(it, R.menu.play_menu)
            }
        }
        return binding.root
    }

    private fun showMenu(view: View, playMenu: Int) {
        val popupMenu = androidx.appcompat.widget.PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(playMenu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.local -> {
                    findNavController().navigate(R.id.nav_play_local)
//                    mainViewModel.navigatePlayLocal()
                }

                else -> {
                    findNavController().navigate(R.id.nav_play_online)
//                    mainViewModel.navigatePlayOnline()
                }
            }
            true
        }
        popupMenu.show()
    }
}

class HistoryAdapter(
    private var history: List<MatchInstance>,
    private val currentPlayerId: String
) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            RecyclerItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: HistoryAdapter.ViewHolder,
        position: Int
    ) {
        val matchInstance = history[position]
        holder.binding.apply {
            title.text =
                if (matchInstance.opponentName == null) "You vs ${root.context.getString(R.string.unknown_user)}" else "You vs ${matchInstance.opponentName}"
            winner.text =
                if (matchInstance.winner == "draw") "Game Draw" else if (matchInstance.winner == currentPlayerId) "You Won" else "You lose"
            val relativeTime = DateUtils.getRelativeDateTimeString(
                root.context,
                matchInstance.createdAt,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0
            )
            time.text = relativeTime
        }
    }

    override fun getItemCount(): Int {
        return history.size
    }
    fun updateDateSet(history: List<MatchInstance>){
        this.history = history
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: RecyclerItemBinding) : RecyclerView.ViewHolder(binding.root) {
    }
}

@SuppressLint("ResourceType")
fun Context.divider(
    horizontal: Boolean = true,
    dividerSize: Int = resources.getDimensionPixelSize(R.dimen.divider_size)
): View {
    val divider = View(this)

    val layoutParams = if (horizontal) {
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dividerSize
        )
    } else {
        LinearLayout.LayoutParams(
            dividerSize,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
    divider.layoutParams = layoutParams
    divider.setBackgroundColor(
        ContextCompat.getColor(
            this,
            com.google.android.material.R.color.material_on_surface_stroke
        )
    )
    return divider
}