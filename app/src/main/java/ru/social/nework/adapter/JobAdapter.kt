package ru.social.nework.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.social.nework.databinding.CardJobBinding
import ru.social.nework.dto.Job
import ru.social.nework.util.AndroidUtils

class JobAdapter(private val onInteractionListener: OnInteractionListener) : ListAdapter<Job, JobViewHolder>(JobDiffCallback()){


    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = getItem(position)
        holder.bind(job)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return JobViewHolder(
            CardJobBinding.inflate(layoutInflater, parent, false),
            onInteractionListener
        )
    }

}

class JobViewHolder(
    private val binding: CardJobBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(job: Job) {
        binding.apply {
            name.text = job.name
            jobPosition.text = job.position
            startFinish.text = AndroidUtils.dateRangeToText(job.start, job.finish)
            link.text = job.link
            delete.visibility = if (job.ownedByMe) View.VISIBLE else View.INVISIBLE
            delete.setOnClickListener{
                onInteractionListener.onJobDelete(job)
            }
        }
    }
}

class JobDiffCallback : DiffUtil.ItemCallback<Job>() {
    override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean {
        return oldItem == newItem
    }
}
