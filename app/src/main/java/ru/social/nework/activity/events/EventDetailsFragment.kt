package ru.social.nework.activity.events

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
import ru.social.nework.databinding.FragmentEventDetailBinding
import ru.social.nework.dto.User
import ru.social.nework.enumeration.AttachmentType
import ru.social.nework.enumeration.EventType
import ru.social.nework.util.AndroidUtils
import ru.social.nework.util.AndroidUtils.dateUTCToText
import ru.social.nework.util.LongArg
import ru.social.nework.util.MediaLifecycleObserver
import ru.social.nework.viewmodel.EventViewModel
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("DEPRECATION")
class EventDetailsFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth

    companion object {
        var Bundle.longArg: Long? by LongArg
    }
    private val eventViewModel: EventViewModel by viewModels(ownerProducer = ::requireActivity)
    private var mapLikers = HashMap<Int, ImageView>()
    private var mapParticipants = HashMap<Int, ImageView>()
    private var mapSpeakers = HashMap<Int, ImageView>()
    private lateinit var binding: FragmentEventDetailBinding
    private val mediaObserver = MediaLifecycleObserver()
    private var likerNumber: Int = -1 //текущий подгружаемый юзер
    private var speakerNumber: Int = -1
    private var participantNumber: Int = -1
    private var needLoadLikersAvatars = false
    private var needLoadSpeakersAvatars = false
    private var needLoadParticipantsAvatars = false

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
                binding.let {
                    AndroidUtils.share(requireContext(), binding.content.text.toString())
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        clearSpeakersAvatars()
        clearLikersAvatars()
        clearParticipantsAvatars()
        fillMaps()
        MapKitFactory.initialize(requireContext())
        lifecycle.addObserver(mediaObserver)
        val eventId = arguments?.longArg ?: -1
        eventViewModel.getEventById(eventId)
        eventViewModel.currentEvent.observe(viewLifecycleOwner) { event ->
            if(event != null) {
                eventViewModel.getLastJob(event.authorId)
                if (event.likeOwnerIds.isNotEmpty()) {
                    if (needLoadLikersAvatars) {
                        needLoadLikersAvatars = false
                        eventViewModel.getLikers(event)
                        binding.likersAvatarsNested.avatarMore.isVisible =
                            event.likeOwnerIds.size > 5
                    }
                }
                if (event.speakerIds.isNotEmpty()) {
                    if (needLoadSpeakersAvatars) {
                        eventViewModel.getSpeakers(event)
                        needLoadSpeakersAvatars = false
                        binding.speakersAvatarsNested.avatarMore.isVisible =
                            event.speakerIds.size > 5
                    }
                }

                if (event.participantsIds.isNotEmpty()) {
                    if (needLoadParticipantsAvatars) {
                        eventViewModel.getParticipants(event)
                        needLoadParticipantsAvatars = false
                        binding.participantsAvatarsNested.avatarMore.isVisible =
                            event.participantsIds.size > 5
                    }
                }
                binding.apply {
                    author.text = event.author
                    Glide.with(avatar)
                        .load(event.authorAvatar)
                        .placeholder(R.drawable.ic_loading_100dp)
                        .error(R.drawable.ic_error_100dp)
                        .timeout(10_000)
                        .circleCrop()
                        .into(binding.avatar)
                    eventType.text =
                        when (event.type) {
                            EventType.OFFLINE -> getString(R.string.offline)
                            EventType.ONLINE -> getString(R.string.online)
                        }
                    published.text = dateUTCToText(event.datetime, requireContext())
                    content.text = event.content
                    like.isChecked = event.likedByMe
                    like.text = event.likes.toString()
                    participants.isChecked = event.participatedByMe
                    participants.text = event.participantsIds.size.toString()
                    if (event.link != null) {
                        link.isVisible = true
                        link.text = event.link
                    } else {
                        link.isVisible = false
                    }
                    if (event.coords != null) {
                        val point = Point(event.coords.lat, event.coords.long)
                        mapView.isVisible = true
                        moveToMarker(point)// Перемещаем камеру в определенную область на карте
                        setMarker(point)// Устанавливаем маркер на карте
                    } else {
                        mapView.isVisible = false
                    }

                    //вложение
                    if (event.attachment?.url != null) {
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
                            setVideoURI(Uri.parse(event.attachment?.url))
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
                            event.attachment!!,
                            binding.audioAttachment.seekBar,
                            binding.audioAttachment.playAudio
                        )
                    }

                    like.setOnClickListener {
                        if (auth.authenticated()) {
                            clearLikersAvatars()
                            eventViewModel.likeById(event)
                        } else {
                            like.isChecked = !like.isChecked
                            AndroidUtils.showSignInDialog(this@EventDetailsFragment)
                        }
                    }

                    participants.setOnClickListener {
                        if (auth.authenticated()) {
                            clearParticipantsAvatars()
                            eventViewModel.participateById(event)
                        } else {
                            participants.isChecked = !participants.isChecked
                            AndroidUtils.showSignInDialog(this@EventDetailsFragment)
                        }
                    }

                    likersAvatarsNested.avatarMore.setOnClickListener {
                        findNavController().navigate(R.id.action_eventDetailsFragment_to_likersFragment)
                    }
                    speakersAvatarsNested.avatarMore.setOnClickListener {
                        findNavController().navigate(R.id.action_eventDetailsFragment_to_speakersFragment)
                    }
                    participantsAvatarsNested.avatarMore.setOnClickListener {
                        findNavController().navigate(R.id.action_eventDetailsFragment_to_participantsFragment)
                    }
                }
            }

        }
        eventViewModel.lastJob.observe(viewLifecycleOwner){
            if(eventViewModel.lastJob.value?.position != null){
                binding.job.text = eventViewModel.lastJob.value?.position.toString()
            } else binding.job.text = getString(R.string.looking_for_a_job)

        }

        eventViewModel.likersLoaded.observe(viewLifecycleOwner){
            eventViewModel.likers.value?.forEach { user ->
                likerNumber++
                mapLikers[likerNumber]?.let { imageView ->
                    loadAvatar(imageView, user)
                    imageView.isVisible = true
                }
            }
        }

        eventViewModel.speakersLoaded.observe(viewLifecycleOwner){
            eventViewModel.speakers.value?.forEach { user ->
                speakerNumber++
                mapSpeakers[speakerNumber]?.let { imageView ->
                    loadAvatar(imageView, user)
                    imageView.isVisible = true
                }
            }
        }

        eventViewModel.participantsLoaded.observe(viewLifecycleOwner){
            eventViewModel.participants.value?.forEach { user ->
                participantNumber++
                mapParticipants[participantNumber]?.let { imageView ->
                    loadAvatar(imageView, user)
                    imageView.isVisible = true
                }
            }
        }

        eventViewModel.dataState.observe(viewLifecycleOwner){state ->
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .show()
                eventViewModel.resetError()
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
        AndroidUtils.addMarkerOnMap(requireContext(), binding.mapView, point)
    }

    private fun moveToMarker(point: Point) {
        AndroidUtils.moveCamera(binding.mapView, point)
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

    private fun clearSpeakersAvatars(){
        speakerNumber = -1
        needLoadSpeakersAvatars = true
        binding.speakersAvatarsNested.avatar1.isVisible = false
        binding.speakersAvatarsNested.avatar2.isVisible = false
        binding.speakersAvatarsNested.avatar3.isVisible = false
        binding.speakersAvatarsNested.avatar4.isVisible = false
        binding.speakersAvatarsNested.avatar5.isVisible = false
    }

    private fun clearParticipantsAvatars(){
        participantNumber = -1
        needLoadParticipantsAvatars = true
        binding.participantsAvatarsNested.avatar1.isVisible = false
        binding.participantsAvatarsNested.avatar2.isVisible = false
        binding.participantsAvatarsNested.avatar3.isVisible = false
        binding.participantsAvatarsNested.avatar4.isVisible = false
        binding.participantsAvatarsNested.avatar5.isVisible = false
    }

    private fun fillMaps(){
        mapLikers[0] = binding.likersAvatarsNested.avatar1
        mapLikers[1] = binding.likersAvatarsNested.avatar2
        mapLikers[2] = binding.likersAvatarsNested.avatar3
        mapLikers[3] = binding.likersAvatarsNested.avatar4
        mapLikers[4] = binding.likersAvatarsNested.avatar5

        mapSpeakers[0] = binding.speakersAvatarsNested.avatar1
        mapSpeakers[1] = binding.speakersAvatarsNested.avatar2
        mapSpeakers[2] = binding.speakersAvatarsNested.avatar3
        mapSpeakers[3] = binding.speakersAvatarsNested.avatar4
        mapSpeakers[4] = binding.speakersAvatarsNested.avatar5

        mapParticipants[0] = binding.participantsAvatarsNested.avatar1
        mapParticipants[1] = binding.participantsAvatarsNested.avatar2
        mapParticipants[2] = binding.participantsAvatarsNested.avatar3
        mapParticipants[3] = binding.participantsAvatarsNested.avatar4
        mapParticipants[4] = binding.participantsAvatarsNested.avatar5
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