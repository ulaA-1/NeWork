package ru.social.nework.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.social.nework.R
import ru.social.nework.databinding.CardUserChooseBinding
import ru.social.nework.dto.User

class ChooseUserAdapter(
    private val context: Context,
    private var checkedUsers: LongArray?,
    private val onCheckListener: OnInteractionListener
) : ListAdapter<User, ChooseUserAdapter.ChooseUserViewHolder>(ChooseUserDiffCallback()){

        override fun onBindViewHolder(holder: ChooseUserViewHolder, position: Int) {
            val user = getItem(position)
            holder.bind(user)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChooseUserViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return ChooseUserViewHolder(
                context,
                CardUserChooseBinding.inflate(layoutInflater, parent, false),
                onCheckListener
            )
        }

    inner class ChooseUserViewHolder(
        private val context: Context,
        private val binding: CardUserChooseBinding,
        private val onCheckListener: OnInteractionListener
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
                checkbox.isChecked = checkedUsers?.contains(user.id) == true

                checkbox.setOnClickListener{
                    if(checkedUsers != null) {
                        if (checkbox.isChecked) {
                            if (checkedUsers?.contains(user.id) == false) checkedUsers =
                                checkedUsers!!.plus(user.id)
                        } else{
                            if (checkedUsers?.contains(user.id) == true) checkedUsers =
                                checkedUsers!!.takeIf { !it.equals(user.id) }
                        }
                    }
                    onCheckListener.onCheck(user, checkbox.isChecked)
                }
            }
        }
    }

    }



    class ChooseUserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }