package ru.social.nework.activity.posts

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.MediaController
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import dagger.hilt.android.AndroidEntryPoint
import ru.social.nework.R
import ru.social.nework.auth.AppAuth
import ru.social.nework.databinding.FragmentPostDetailsBinding
import ru.social.nework.dto.User
import ru.social.nework.enumeration.AttachmentType
import ru.social.nework.util.AndroidUtils
import ru.social.nework.util.AndroidUtils.addMarkerOnMap
import ru.social.nework.util.AndroidUtils.moveCamera
import ru.social.nework.util.AndroidUtils.share
import ru.social.nework.util.LongArg
import ru.social.nework.util.MediaLifecycleObserver
import ru.social.nework.viewmodel.PostViewModel
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("DEPRECATION")
class PostDetailsFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth

    companion object {
        var Bundle.longArg: Long? by LongArg
    }
    private val postViewModel: PostViewModel by viewModels(ownerProducer = ::requireActivity)
    private var mapLikers = HashMap<Int, ImageView>()
    private var mapMentioned = HashMap<Int, ImageView>()
    private lateinit var binding: FragmentPostDetailsBinding
    private val mediaObserver = MediaLifecycleObserver()

    private var likerNumber: Int = -1 //текущий подгружаемый юзер
    private var mentionedNumber: Int = -1
    private var needLoadLikersAvatars = false
    private var needLoadMentionedAvatars = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @Deprecated("Deprecated in Java", ReplaceWith(
        "inflater.inflate(R.menu.menu_details, menu)",
        "ru.netology.nework.R"
    )
    )
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_details, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share -> {
                share(requireContext(), binding.content.text.toString())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentPostDetailsBinding.inflate(inflater, container, false)
        clearLikersAvatars()
        clearMentionAvatars()
        fillMaps()
        MapKitFactory.initialize(requireContext())
        lifecycle.addObserver(mediaObserver)
        val postId = arguments?.longArg ?: -1
        postViewModel.getPostById(postId)
        postViewModel.currentPost.observe(viewLifecycleOwner) { post ->
            if(post != null) {
                postViewModel.getLastJob(post.authorId)
                if (post.likeOwnerIds.isNotEmpty()) {
                    if (needLoadLikersAvatars) {
                        needLoadLikersAvatars = false
                        postViewModel.getLikers(post)
                        binding.likersAvatarsNested.avatarMore.isVisible =
                            post.likeOwnerIds.size > 5
                    }
                }
                if (post.mentionIds.isNotEmpty()) {
                    if (needLoadMentionedAvatars) {
                        postViewModel.getMentioned(post)
                        needLoadMentionedAvatars = false
                        binding.mentionAvatarsNested.avatarMore.isVisible = post.mentionIds.size > 5
                    }
                }
                binding.apply {
                    author.text = post.author
                    Glide.with(avatar)
                        .load(post.authorAvatar)
                        .placeholder(R.drawable.ic_loading_100dp)
                        .error(R.drawable.ic_error_100dp)
                        .timeout(10_000)
                        .circleCrop()
                        .into(binding.avatar)
                    val date = ZonedDateTime.parse(post.published)
                        .withZoneSameInstant(ZoneId.systemDefault())
                    published.text =
                        date.format(DateTimeFormatter.ofPattern(context?.getString(R.string.date_pattern)))
                    content.text = post.content
                    like.isChecked = post.likedByMe
                    like.text = post.likes.toString()
                    mention.isChecked = post.mentionedMe
                    mention.text = post.mentionIds.size.toString()
                    if (post.link != null) {
                        link.isVisible = true
                        link.text = post.link
                    } else {
                        link.isVisible = false
                    }
                    if (post.coords != null) {
                        val point = Point(post.coords.lat, post.coords.long)
                        mapView.isVisible = true
                        moveToMarker(point)// Перемещаем камеру в определенную область на карте
                        setMarker(point)// Устанавливаем маркер на карте
                    } else {
                        mapView.isVisible = false
                    }

                    //вложение
                    if (post.attachment?.url != null) {
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
                                    .into(binding.videoAttachment.videoThumb)
                            }
                            //аудио
                            AttachmentType.AUDIO -> {
                                audioAttachment.audioAttachmentNested.isVisible = true
                                imageAttachment.isVisible = false
                                videoAttachment.videoAttachmentNested.isVisible = false
                            }
                        }
                    } else {
                        imageAttachment.isVisible = false
                        Glide.with(imageAttachment).clear(binding.imageAttachment)
                        audioAttachment.audioAttachmentNested.isVisible = false
                        videoAttachment.videoAttachmentNested.isVisible = false
                    }

                    videoAttachment.playVideo.setOnClickListener {
                        videoAttachment.videoView.isVisible = true
                        videoAttachment.videoView.apply {
                            setMediaController(MediaController(context))
                            setVideoURI(Uri.parse(post.attachment?.url))
                            setOnPreparedListener {
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
                        mediaObserver.playAudio(
                            post.attachment!!,
                            binding.audioAttachment.seekBar,
                            binding.audioAttachment.playAudio
                        )
                    }

                    like.setOnClickListener {
                        if (auth.authenticated()) {
                            clearLikersAvatars()
                            postViewModel.likeById(post)
                        } else {
                            like.isChecked = !like.isChecked
                            AndroidUtils.showSignInDialog(this@PostDetailsFragment)
                        }

                    }
                    likersAvatarsNested.avatarMore.setOnClickListener {
                        findNavController().navigate(R.id.action_postDetailsFragment_to_likersFragment)
                    }
                    mentionAvatarsNested.avatarMore.setOnClickListener {
                        findNavController().navigate(R.id.action_postDetailsFragment_to_mentionedFragment)
                    }
                }
            }

        }
        postViewModel.lastJob.observe(viewLifecycleOwner){
            if(postViewModel.lastJob.value?.position != null){
                binding.job.text = postViewModel.lastJob.value?.position.toString()
            } else binding.job.text = getString(R.string.looking_for_a_job)

        }
        postViewModel.likersLoaded.observe(viewLifecycleOwner){
            postViewModel.likers.value?.forEach { user ->
                likerNumber++
                mapLikers[likerNumber]?.let { imageView ->
                    loadAvatar(imageView, user)
                    imageView.isVisible = true
                }
            }
        }

        postViewModel.mentionedLoaded.observe(viewLifecycleOwner){
            postViewModel.mentioned.value?.forEach { user ->
                mentionedNumber++
                mapMentioned[mentionedNumber]?.let { imageView ->
                    loadAvatar(imageView, user)
                    imageView.isVisible = true
                }
            }
        }

        postViewModel.dataState.observe(viewLifecycleOwner){state ->
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .show()
                postViewModel.resetError()
            }
        }

        return binding.root
    }

    // Отображаем карты перед тем моментом, когда активити с картой станет видимой пользователю:
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapView.onStart()
    }

    // Останавливаем обработку карты, когда активити с картой становится невидимым для пользователя:
    override fun onStop() {
        binding.mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    private fun setMarker(point: Point) {
        addMarkerOnMap(requireContext(), binding.mapView, point)
    }

    private fun moveToMarker(point: Point) {
        moveCamera(binding.mapView, point)
    }
    private fun clearLikersAvatars(){
        likerNumber = -1
        needLoadLikersAvatars = true
        binding.likersAvatarsNested.avatar1.isVisible = false
        binding.likersAvatarsNested.avatar2.isVisible = false
        binding.likersAvatarsNested.avatar3.isVisible = false
        binding.likersAvatarsNested.avatar4.isVisible = false
        binding.likersAvatarsNested.avatar5.isVisible = false
    }

    private fun clearMentionAvatars(){
        mentionedNumber = -1
        needLoadMentionedAvatars = true
        binding.mentionAvatarsNested.avatar1.isVisible = false
        binding.mentionAvatarsNested.avatar2.isVisible = false
        binding.mentionAvatarsNested.avatar3.isVisible = false
        binding.mentionAvatarsNested.avatar4.isVisible = false
        binding.mentionAvatarsNested.avatar5.isVisible = false
    }

    private fun fillMaps(){
        mapLikers[0] = binding.likersAvatarsNested.avatar1
        mapLikers[1] = binding.likersAvatarsNested.avatar2
        mapLikers[2] = binding.likersAvatarsNested.avatar3
        mapLikers[3] = binding.likersAvatarsNested.avatar4
        mapLikers[4] = binding.likersAvatarsNested.avatar5

        mapMentioned[0] = binding.mentionAvatarsNested.avatar1
        mapMentioned[1] = binding.mentionAvatarsNested.avatar2
        mapMentioned[2] = binding.mentionAvatarsNested.avatar3
        mapMentioned[3] = binding.mentionAvatarsNested.avatar4
        mapMentioned[4] = binding.mentionAvatarsNested.avatar5
    }
    private fun loadAvatar(imageView: ImageView, user: User){
        Glide.with(imageView)
            .load(user.avatar)
            .placeholder(R.drawable.ic_loading_100dp)
            .error(user.userIcon(requireContext()))
            .timeout(10_000)
            .circleCrop()
            .into(imageView)

    }



}
