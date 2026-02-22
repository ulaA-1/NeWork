package ru.social.nework.ui.feed

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.social.nework.R

class PostsAdapter(
    private val onMenu: (View, PostUi) -> Unit,
    private val onLike: (PostUi) -> Unit,
    private val onShare: (PostUi) -> Unit,
    private val onClick: (PostUi) -> Unit,
) : ListAdapter<PostUi, PostsAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<PostUi>() {
        override fun areItemsTheSame(oldItem: PostUi, newItem: PostUi) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: PostUi, newItem: PostUi) = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return VH(v, onMenu, onLike, onShare, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(
        itemView: View,
        private val onMenu: (View, PostUi) -> Unit,
        private val onLike: (PostUi) -> Unit,
        private val onShare: (PostUi) -> Unit,
        private val onClick: (PostUi) -> Unit,
    ) : RecyclerView.ViewHolder(itemView) {

        private val author: TextView = itemView.findViewById(R.id.author)
        private val published: TextView = itemView.findViewById(R.id.published)
        private val content: TextView = itemView.findViewById(R.id.content)
        private val likeCount: TextView = itemView.findViewById(R.id.likeCount)

        private val menu: ImageButton = itemView.findViewById(R.id.menu)
        private val like: ImageButton = itemView.findViewById(R.id.like)
        private val share: ImageButton = itemView.findViewById(R.id.share)

        private val avatar: ImageView = itemView.findViewById(R.id.avatar)

        private val attachmentContainer: View = itemView.findViewById(R.id.attachmentContainer)
        private val attachment: ImageView = itemView.findViewById(R.id.attachment)
        private val play: ImageView = itemView.findViewById(R.id.play)
        private val audio: ImageView = itemView.findViewById(R.id.audio)

        private val link: TextView = itemView.findViewById(R.id.link)

        private var current: PostUi? = null

        init {
            itemView.setOnClickListener { current?.let(onClick) }

            menu.setOnClickListener { v ->
                current?.let { onMenu(v, it) }
            }

            like.setOnClickListener {
                current?.let(onLike)
            }

            share.setOnClickListener {
                current?.let(onShare)
            }

            link.setOnClickListener {
                val raw = current?.linkUrl ?: return@setOnClickListener
                openUrl(raw)
            }
        }

        fun bind(post: PostUi) {
            current = post

            author.text = post.author
            published.text = post.published
            content.text = post.content
            likeCount.text = post.likes.toString()

            menu.isVisible = post.ownedByMe

            link.isVisible = !post.linkUrl.isNullOrBlank()
            link.text = post.linkUrl

            val hasAttachment = !post.attachmentUrl.isNullOrBlank() && post.attachmentType != null
            attachmentContainer.isVisible = hasAttachment

            play.isVisible = false
            audio.isVisible = false
            attachment.setImageDrawable(null)

            if (hasAttachment) {
                when (post.attachmentType) {
                    AttachmentType.IMAGE -> {
                        Glide.with(itemView.context)
                            .load(post.attachmentUrl)
                            .centerCrop()
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .into(attachment)
                    }

                    AttachmentType.VIDEO -> {
                        Glide.with(itemView.context)
                            .load(post.attachmentUrl)
                            .centerCrop()
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .into(attachment)

                        play.isVisible = true
                    }

                    AttachmentType.AUDIO -> {
                        attachment.setImageResource(android.R.drawable.ic_menu_gallery)
                        audio.setImageResource(R.drawable.ic_play_24)
                        audio.isVisible = true
                    }

                    null -> Unit
                }
            }

            if (!post.authorAvatar.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(post.authorAvatar)
                    .circleCrop()
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .into(avatar)
            } else {
                avatar.setImageResource(R.drawable.ic_default_avatar)
            }
        }

        private fun openUrl(raw: String) {
            val url = normalizeUrl(raw)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            try {
                itemView.context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
            }
        }

        private fun normalizeUrl(raw: String): String {
            val t = raw.trim()
            return if (t.startsWith("http://") || t.startsWith("https://")) t else "https://$t"
        }
    }
}