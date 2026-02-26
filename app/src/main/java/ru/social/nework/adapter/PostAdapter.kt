package ru.social.nework.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.PopupMenu
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.social.nework.R
import ru.social.nework.databinding.CardPostBinding
import ru.social.nework.dto.Job
import ru.social.nework.dto.Post
import ru.social.nework.dto.User
import ru.social.nework.enumeration.AttachmentType
import ru.social.nework.util.AndroidUtils
import ru.social.nework.util.MediaLifecycleObserver


interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onShare(post: Post) {}
    fun onItemClick (post: Post) {}
    fun onPlayAudio (post: Post, seekBar: SeekBar, playAudio: ImageButton) {}
    fun onCheck(user: User, checked: Boolean){}
    fun onUserClick (user: User) {}
    fun onJobDelete(job: Job) {}
}


class PostAdapter(
    private val onInteractionListener: OnInteractionListener,
    context: Context,
    private val authenticated: Boolean,
    private val mediaLifecycleObserver: MediaLifecycleObserver
): PagingDataAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    private val context: Context
    var previousPosition = -1

    init {
        this.context = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return PostViewHolder(
            CardPostBinding.inflate(layoutInflater, parent, false),
            onInteractionListener
        )
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        if (post != null) {
            holder.bind(post, position)
        }
    }


    inner class PostViewHolder(
        private val binding: CardPostBinding,
        private val onInteractionListener: OnInteractionListener,
    ) : RecyclerView.ViewHolder(binding.root) {

        var seekBar = binding.audioAttachment.seekBar

        fun bind(post: Post, position: Int){
            binding.apply {
                author.text = post.author
                Glide.with(avatar)
                    .load(post.authorAvatar)
                    .placeholder(R.drawable.ic_loading_100dp)
                    .error(R.drawable.ic_error_100dp)
                    .timeout(10_000)
                    .circleCrop()
                    .into(binding.avatar)
                published.text = AndroidUtils.dateUTCToText(post.published, context)
                content.text = post.content
                // в адаптере
                like.isChecked = post.likedByMe
                like.text = post.likes.toString()//"${post.likes()}"
                if(post.link != null){
                    link.isVisible = true
                    link.text = post.link
                }
                else{
                    link.isVisible = false
                }
                imageAttachment.isVisible = false
                audioAttachment.audioAttachmentNested.isVisible = false
                videoAttachment.videoAttachmentNested.isVisible = false
                Glide.with(imageAttachment).clear(binding.imageAttachment)
                Glide.with(videoAttachment.videoThumb).clear(binding.videoAttachment.videoThumb)
                audioAttachment.playAudio.setBackgroundResource(R.drawable.play_48)
                videoAttachment.videoView.setVideoURI(null)
                videoAttachment.videoView.stopPlayback()
                audioAttachment.seekBar.progress = 0
                //вложение

                if(post.attachment != null) {
                    when (post.attachment.type) {
                        //изображение
                        AttachmentType.IMAGE -> {
                            imageAttachment.isVisible = true
                            audioAttachment.audioAttachmentNested.isVisible = false
                            videoAttachment.videoAttachmentNested.isVisible = false
                            Glide.with(imageAttachment)
                                .load(post.attachment.url)
                                .placeholder(R.drawable.ic_loading_100dp)
                                .error(R.drawable.ic_error_100dp)
                                .timeout(10_000)
                                .centerCrop()
                                .into(binding.imageAttachment)
                        }
                        //видео
                        AttachmentType.VIDEO -> {
                            audioAttachment.audioAttachmentNested.isVisible = false
                            imageAttachment.isVisible = false
                            videoAttachment.videoAttachmentNested.isVisible = true
                            Glide.with(videoAttachment.videoThumb)
                                .load(post.attachment.url)
                                .centerCrop()
                                .into(binding.videoAttachment.videoThumb)
                        }
                        //аудио
                        AttachmentType.AUDIO -> {
                            audioAttachment.audioAttachmentNested.isVisible = true
                            imageAttachment.isVisible = false
                            videoAttachment.videoAttachmentNested.isVisible = false
                            if(position != previousPosition){
                                audioAttachment.playAudio.setBackgroundResource(R.drawable.play_48)
                                audioAttachment.seekBar.progress = 0
                                audioAttachment.seekBar.removeCallbacks(mediaLifecycleObserver.runnable)
                                audioAttachment.playAudio.setBackgroundResource(R.drawable.play_48)
                            } else{
                                audioAttachment.seekBar.max = mediaLifecycleObserver.mediaPlayer!!.duration
                                audioAttachment.seekBar.progress = mediaLifecycleObserver.mediaPlayer!!.currentPosition
                                audioAttachment.seekBar.postDelayed(mediaLifecycleObserver.runnable, 1000)
                                audioAttachment.playAudio.setBackgroundResource(R.drawable.pause_48)
                                mediaLifecycleObserver.seekSet(audioAttachment.seekBar)
                            }
                        }
                    }
                }

                videoAttachment.playVideo.setOnClickListener {
                    videoAttachment.videoView.isVisible = true
                    videoAttachment.videoView.apply {
                        setMediaController(MediaController(context))
                        setVideoURI(Uri.parse(post.attachment?.url))
                        setOnPreparedListener{
                            videoAttachment.videoThumb.isVisible = false
                            videoAttachment.playVideo.isVisible = false
                            start()
                        }
                        setOnCompletionListener {
                            stopPlayback()
                            videoAttachment.videoView.isVisible = false
                            videoAttachment.playVideo.isVisible = true
                            videoAttachment.videoThumb.isVisible = true
                        }
                    }
                }

                audioAttachment.playAudio.setOnClickListener {
                    if(previousPosition == -1){
                        mediaLifecycleObserver.playAudio(post.attachment!!, audioAttachment.seekBar, audioAttachment.playAudio)
                    } else {
                        if (position == previousPosition) {
                            mediaLifecycleObserver.playAudio(
                                post.attachment!!,
                                audioAttachment.seekBar,
                                audioAttachment.playAudio
                            )
                        } else {
                            if (mediaLifecycleObserver.mediaPlayer?.isPlaying == true) {
                                mediaLifecycleObserver.stop()
                                notifyItemChanged(previousPosition)
                                mediaLifecycleObserver.playAudio(
                                    post.attachment!!,
                                    audioAttachment.seekBar,
                                    audioAttachment.playAudio
                                )
                            } else {
                                mediaLifecycleObserver.playAudio(
                                    post.attachment!!,
                                    audioAttachment.seekBar,
                                    audioAttachment.playAudio
                                )
                            }
                        }
                    }
                    previousPosition = position

                }
                menu.visibility = if (post.ownedByMe) View.VISIBLE else View.INVISIBLE
                menu.setOnClickListener {
                    PopupMenu(it.context, it).apply {
                        inflate(R.menu.options_post)
                        setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                R.id.remove -> {
                                    onInteractionListener.onRemove(post)
                                    true
                                }
                                R.id.edit -> {
                                    onInteractionListener.onEdit(post)
                                    true
                                }

                                else -> false
                            }
                        }
                    }.show()
                }
                like.setOnClickListener {
                    onInteractionListener.onLike(post)
                    if(!authenticated) {
                        notifyItemChanged(position)
                    }
                }

                share.setOnClickListener {
                    onInteractionListener.onShare(post)
                }

                itemView.setOnClickListener {
                    onInteractionListener.onItemClick(post)
                }
            }
        }

    }

}


class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
        return oldItem == newItem
    }

}