package dadm.grupo.dadmproyecto.ui.ranking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dadm.grupo.dadmproyecto.R
import dadm.grupo.dadmproyecto.domain.model.RankedUser

class RankingAdapter : ListAdapter<RankedUser, RankingAdapter.RankingViewHolder>(RankedUserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rank_item, parent, false)
        return RankingViewHolder(view)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        val rankedUser = getItem(position)
        holder.bind(rankedUser)
    }

    class RankingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val positionTextView: TextView = itemView.findViewById(R.id.positionTextView)
        private val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        private val locationsCountTextView: TextView = itemView.findViewById(R.id.locationsCountTextView)
        private val cardView: CardView = itemView as CardView

        fun bind(user: RankedUser) {
            val context = itemView.context
            val position = user.position - 1

            positionTextView.text = when (position) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> user.position.toString()
            }

            userNameTextView.text = user.displayName
            locationsCountTextView.text = user.visitCount.toString()

            val backgroundColor = when (position) {
                0 -> ContextCompat.getColor(context, R.color.gold)
                1 -> ContextCompat.getColor(context, R.color.silver)
                2 -> ContextCompat.getColor(context, R.color.bronze)
                else -> ContextCompat.getColor(context, R.color.light_grey)
            }

            cardView.setCardBackgroundColor(backgroundColor)
        }
    }

    class RankedUserDiffCallback : DiffUtil.ItemCallback<RankedUser>() {
        override fun areItemsTheSame(oldItem: RankedUser, newItem: RankedUser): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: RankedUser, newItem: RankedUser): Boolean {
            return oldItem == newItem
        }
    }
}
