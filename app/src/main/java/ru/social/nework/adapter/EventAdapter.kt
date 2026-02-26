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
import ru.social.nework.databinding.CardEventBinding
import ru.social.nework.dto.Event
import ru.social.nework.enumeration.AttachmentType
import ru.social.nework.enumeration.EventType
import ru.social.nework.util.AndroidUtils.dateUTCToText
import ru.social.nework.util.MediaLifecycleObserver

interface EventOnInteractionListener {
    fun onLike(event: Event) {}
    fun onParticipate(event: Event) {}
    fun onEdit(event: Event) {}
    fun onRemove(event: Event) {}
    fun onShare(event: Event) {}
    fun onItemClick (event: Event) {}
    fun onPlayAudio (event: Event, seekBar: SeekBar, playAudio: ImageButton) {}
}

class EventAdapter {

    class EventAdapter(
        private val onInteractionListener: EventOnInteractionListener,
        context: Context,
        private val authenticated: Boolean,
        private val mediaLifecycleObserver: MediaLifecycleObserver
    ): PagingDataAdapter<Event, EventAdapter.EventViewHolder>(EventDiffCallback()) {


        private val context: Context
        var previousPosition = -1

        init {
            this.context = context
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return EventViewHolder(
                CardEventBinding.inflate(layoutInflater, parent, false),
                onInteractionListener
            )
        }

        override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
            val event = getItem(position)
            if (event != null) {
                holder.bind(event, position)
            }
        }


        inner class EventViewHolder(
            private val binding: CardEventBinding,
            private val onInteractionListener: EventOnInteractionListener,
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(event: Event, position: Int){
                binding.apply {
                    author.text = event.author
                    Glide.with(avatar)
                        .load(event.authorAvatar)
                        .placeholder(R.drawable.ic_loading_100dp)
                        .error(R.drawable.ic_error_100dp)
                        .timeout(10_000)
                        .circleCrop()
                        .into(binding.avatar)
                    published.text = dateUTCToText(event.published, context)
                    eventType.text =
                        when(event.type){
                            EventType.OFFLINE -> context.getString(R.string.offline)
                            EventType.ONLINE -> context.getString(R.string.online)
                        }
                    datetime.text = dateUTCToText(event.datetime, context)
                    content.text = event.content
                    // в адаптере
                    like.isChecked = event.likedByMe
                    like.text = event.likes.toString()//"${post.likes()}"
                    participants.isChecked = event.participatedByMe
                    participants.text = event.participants.toString()//"${post.likes()}"
                    if(event.link != null){
                        link.isVisible = true
                        link.text = event.link
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

                    if(event.attachment != null) {
                        when (event.attachment.type) {
                            //изображение
                            AttachmentType.IMAGE -> {
                                imageAttachment.isVisible = true
                                audioAttachment.audioAttachmentNested.isVisible = false
                                videoAttachment.videoAttachmentNested.isVisible = false
                                Glide.with(imageAttachment)
                                    .load(event.attachment.url)
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
                                    .load(event.attachment.url)
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
                            setVideoURI(Uri.parse(event.attachment?.url))
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
                            mediaLifecycleObserver.playAudio(event.attachment!!, audioAttachment.seekBar, audioAttachment.playAudio)
                        } else {
                            if (position == previousPosition) {
                                mediaLifecycleObserver.playAudio(
                                    event.attachment!!,
                                    audioAttachment.seekBar,
                                    audioAttachment.playAudio
                                )
                            } else {
                                if (mediaLifecycleObserver.mediaPlayer?.isPlaying == true) {
                                    mediaLifecycleObserver.stop()
                                    notifyItemChanged(previousPosition)
                                    mediaLifecycleObserver.playAudio(
                                        event.attachment!!,
                                        audioAttachment.seekBar,
                                        audioAttachment.playAudio
                                    )
                                } else {
                                    mediaLifecycleObserver.playAudio(
                                        event.attachment!!,
                                        audioAttachment.seekBar,
                                        audioAttachment.playAudio
                                    )
                                }
                            }
                        }
                        previousPosition = position

                    }
                    menu.visibility = if (event.ownedByMe) View.VISIBLE else View.INVISIBLE
                    menu.setOnClickListener {
                        PopupMenu(it.context, it).apply {
                            inflate(R.menu.options_post)
                            setOnMenuItemClickListener { item ->
                                when (item.itemId) {
                                    R.id.remove -> {
                                        onInteractionListener.onRemove(event)
                                        true
                                    }
                                    R.id.edit -> {
                                        onInteractionListener.onEdit(event)
                                        true
                                    }

                                    else -> false
                                }
                            }
                        }.show()
                    }

                    like.setOnClickListener {
                        onInteractionListener.onLike(event)
                        if(!authenticated) {
                            notifyItemChanged(position)
                        }
                    }

                    share.setOnClickListener {
                        onInteractionListener.onShare(event)
                    }

                    itemView.setOnClickListener {
                        onInteractionListener.onItemClick(event)
                    }

                    participants.setOnClickListener {
                        onInteractionListener.onParticipate(event)
                        if(!authenticated) {
                            notifyItemChanged(position)
                        }
                    }
                }
            }

        }
    }


    class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }

    }
}