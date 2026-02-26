package ru.social.nework.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.social.nework.R
import ru.social.nework.databinding.CardUserBinding
import ru.social.nework.dto.User


class UserAdapter(private val onInteractionListener: OnInteractionListener, private val context: Context) : ListAdapter<User, UserViewHolder>(UserDiffCallback()){


    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return UserViewHolder(
            context,
            CardUserBinding.inflate(layoutInflater, parent, false),
            onInteractionListener
        )
    }

}

class UserViewHolder(
    private val context: Context,
    private val binding: CardUserBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(user: User) {
        binding.apply {
            Glide.with(avatar)
                .load(user.avatar)
                .placeholder(R.drawable.ic_loading_100dp)
                .error(user.userIcon(context))
                .timeout(10_000)
                .circleCrop()
                .into(binding.avatar)
            name.text = user.name
            login.text = user.login

            itemView.setOnClickListener {
                onInteractionListener.onUserClick(user)
            }
        }
    }
}

class UserDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}
