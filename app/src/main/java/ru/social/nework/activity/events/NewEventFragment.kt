package ru.social.nework.activity.events

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.MediaController
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import dagger.hilt.android.AndroidEntryPoint
import ru.social.nework.R
import ru.social.nework.activity.posts.ChooseUsersFragment.Companion.longArrayArg
import ru.social.nework.databinding.FragmentNewEventBinding
import ru.social.nework.dto.Attachment
import ru.social.nework.dto.Coords
import ru.social.nework.enumeration.AttachmentType
import ru.social.nework.enumeration.EventType
import ru.social.nework.util.AndroidUtils
import ru.social.nework.util.AndroidUtils.calendarToUTCDate
import ru.social.nework.util.AndroidUtils.dateUTCToCalendar
import ru.social.nework.util.AndroidUtils.getFile
import ru.social.nework.util.MediaLifecycleObserver
import ru.social.nework.viewmodel.EventViewModel
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
@AndroidEntryPoint
@Suppress("DEPRECATION")
class NewEventFragment : Fragment(), UserLocationObjectListener, CameraListener {

    companion object {
        const val MAX_SIZE = 15728640
    }
    private val viewModel: EventViewModel by viewModels(ownerProducer = ::requireActivity)
    private var fragmentBinding: FragmentNewEventBinding? = null
    private val mediaObserver = MediaLifecycleObserver()
    private lateinit var checkedUsers: LongArray
    private lateinit var mapView: MapView
    private lateinit var userLocationLayer: UserLocationLayer
    private var followUserLocation = false
    private var permissionLocation = false
    private var myLocation = Point(0.0, 0.0)
    private lateinit var checkLocationPermission: ActivityResultLauncher<Array<String>>
    private val mapInputListener: InputListener = object : InputListener {
        override fun onMapTap(p0: Map, p1: Point) {
            viewModel.changeCoords(Coords(p1.latitude, p1.longitude))
        }

        override fun onMapLongTap(p0: Map, p1: Point) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setHasOptionsMenu(true)
    }

    @Deprecated("Deprecated in Java", ReplaceWith(
        "inflater.inflate(R.menu.menu_new_post, menu)",
        "ru.netology.nework.R"
    )
    )
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_new_post, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.save -> {
                fragmentBinding?.let {
                    viewModel.changeContent(it.edit.text.toString())
                    viewModel.changeLink(it.link.text.toString())
                    viewModel.save()
                    AndroidUtils.hideKeyboard(requireView())
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewEventBinding.inflate(
            inflater,
            container,
            false
        )

        fragmentBinding = binding
        mapView = binding.mapView
        lifecycle.addObserver(mediaObserver)
        checkLocationPermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                onMapReady()
            }
        }

        //редактирование
        viewModel.edited.observe(viewLifecycleOwner) {
            if(viewModel.changed.value != true) {
                val edited = viewModel.edited.value
                binding.edit.setText(edited?.content)
                binding.link.setText(edited?.link)
                edited?.attachment?.let {
                    viewModel.changeAttachment(it.url, null, null, it.type)
                }
                edited?.coords?.let {
                    viewModel.changeCoords(it)
                }
                edited?.speakerIds?.let {
                    viewModel.changeSpeakersNewEvent(it)
                }
                edited?.datetime?.let {
                    viewModel.changeDateTime(it)
                }
                edited?.type?.let {
                    viewModel.changeType(it)
                }
            }

        }
        viewModel.dataState.observe(viewLifecycleOwner){state ->
            if (state.error) {
                Snackbar.make(binding.root, state.errorText, Snackbar.LENGTH_LONG)
                    .show()
            }
        }
        viewModel.attachment.observe(viewLifecycleOwner) {
            if (it == null) {
                if (mediaObserver.mediaPlayer?.isPlaying == true) {
                    mediaObserver.stop()
                }
                binding.audioContainer.audioAttachmentNested.seekBar.progress = 0
                binding.photoContainer.photoContainerNested.visibility = View.GONE
                binding.audioContainer.audioContainerNested.visibility = View.GONE
                binding.videoContainer.videoContainerNested.visibility = View.GONE
            } else{
                when(it.attachmentType){
                    AttachmentType.IMAGE -> {
                        binding.photoContainer.photoContainerNested.visibility = View.VISIBLE
                        if (mediaObserver.mediaPlayer?.isPlaying == true) {
                            mediaObserver.stop()
                        }
                        binding.audioContainer.audioAttachmentNested.seekBar.progress = 0
                        binding.audioContainer.audioContainerNested.visibility = View.GONE
                        binding.videoContainer.videoContainerNested.visibility = View.GONE
                        if(it.url != null){
                            Glide.with(binding.photoContainer.photo)
                                .load("${it.url}")
                                .placeholder(R.drawable.ic_loading_100dp)
                                .error(R.drawable.ic_error_100dp)
                                .timeout(10_000)
                                .centerCrop()
                                .into(binding.photoContainer.photo)
                        } else{
                            binding.photoContainer.photo.setImageURI(it.uri)
                        }
                    }
                    AttachmentType.AUDIO -> {
                        binding.photoContainer.photoContainerNested.visibility = View.GONE
                        binding.videoContainer.videoContainerNested.visibility = View.GONE
                        binding.audioContainer.audioContainerNested.visibility = View.VISIBLE

                    }
                    AttachmentType.VIDEO -> {
                        if (mediaObserver.mediaPlayer?.isPlaying == true) {
                            mediaObserver.stop()
                        }
                        binding.audioContainer.audioAttachmentNested.seekBar.progress = 0
                        binding.photoContainer.photoContainerNested.visibility = View.GONE
                        binding.audioContainer.audioContainerNested.visibility = View.GONE
                        binding.videoContainer.videoContainerNested.visibility = View.VISIBLE
                        Glide.with(binding.videoContainer.videoAttachmentNested.videoThumb)
                            .load(it.url ?: it.uri)
                            .into(binding.videoContainer.videoAttachmentNested.videoThumb)
                    }

                    else -> Unit
                }
            }

        }
        viewModel.coords.observe(viewLifecycleOwner){
            binding.mapView.map.mapObjects.clear()
            if(it == null){
                binding.coordsContainer.visibility = View.GONE
            } else{
                binding.coordsContainer.visibility = View.VISIBLE
                AndroidUtils.moveCamera(binding.mapView, Point(it.lat, it.long))
                AndroidUtils.addMarkerOnMap(
                    requireContext(),
                    binding.mapView,
                    Point(it.lat, it.long)
                )
            }
        }

        viewModel.speakersNewEvent.observe(viewLifecycleOwner){
            checkedUsers = it.toLongArray()
        }

        binding.edit.requestFocus()

        val pickPhotoLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                when (it.resultCode) {
                    ImagePicker.RESULT_ERROR -> {
                        Snackbar.make(
                            binding.root,
                            ImagePicker.getError(it.data),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }

                    Activity.RESULT_OK -> {
                        val uri: Uri? = it.data?.data
                        viewModel.changeAttachment(null, uri, uri?.toFile(), AttachmentType.IMAGE)
                    }
                }
            }

        binding.pickPhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(MAX_SIZE)
                .galleryMimeTypes(
                    arrayOf(
                        "image/png",
                        "image/jpeg",
                    )
                )
                .createIntent(pickPhotoLauncher::launch)
        }


        binding.photoContainer.removePhoto.setOnClickListener {
            removeAttachment()
        }

        binding.audioContainer.removeAudio.setOnClickListener {
            removeAttachment()
        }

        binding.videoContainer.removeVideo.setOnClickListener {
            removeAttachment()
        }

        binding.removeCoords.setOnClickListener {
            removeCoords()
        }

        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                try {
                    if (uri != null) {
                        if (mediaObserver.mediaPlayer?.isPlaying == true) {
                            mediaObserver.stop()
                        }
                        binding.audioContainer.audioAttachmentNested.seekBar.progress = 0
                        val fileDescriptor = requireContext().contentResolver.openAssetFileDescriptor(uri, "r")
                        val fileSize = fileDescriptor?.length ?: 0
                        if( fileSize > MAX_SIZE){
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.attachment_shouldn_t_excess_15mb),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@registerForActivityResult
                        }
                        fileDescriptor?.close()
                        val file = uri.getFile(requireContext())
                        if (requireContext().contentResolver.getType(uri)
                                ?.startsWith("audio/") == true
                        ) {
                            viewModel.changeAttachment(null, uri, file, AttachmentType.AUDIO)
                        } else {
                            if (requireContext().contentResolver.getType(uri)
                                    ?.startsWith("video/") == true
                            ) {
                                viewModel.changeAttachment(null, uri, file, AttachmentType.VIDEO)
                            }
                        }
                    }
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }


        binding.attach.setOnClickListener {
            val choose = arrayOf("audio/*", "video/*")
            resultLauncher.launch(choose)
        }

        binding.location.setOnClickListener{
            checkPermission()
            binding.coordsContainer.visibility = View.VISIBLE
        }

        binding.speakers.setOnClickListener{
            findNavController().navigate(R.id.action_newEventFragment_to_chooseSpeakersFragment,
                Bundle().apply{
                    longArrayArg = checkedUsers
                })
        }

        binding.fab.setOnClickListener{
            val bottomSheetDialog = BottomSheetDialog(requireContext())

            val view = layoutInflater.inflate(R.layout.bottom_sheet, null)
            val date = view.findViewById<TextInputEditText>(R.id.date_input)
            val datePicker = view.findViewById<ImageButton>(R.id.date_picker)
            val calendar = if(viewModel.datetime.value?.equals("") == true) {
                Calendar.getInstance()
            } else{
                dateUTCToCalendar(viewModel.datetime.value!!)
            }
            date.setText(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(calendar.time))
            datePicker.setOnClickListener {
                val datePickerDialog = DatePickerDialog(requireContext(),
                    R.style.dialog,
                    { _, year, month, day ->
                        calendar.set(year, month, day)
                        val timePicker = TimePickerDialog(requireContext(),
                            R.style.dialog,
                            { _, hour, minute ->
                                calendar.set(Calendar.HOUR_OF_DAY, hour)
                                calendar.set(Calendar.MINUTE, minute)
                                date.setText(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(calendar.time))
                                viewModel.changeDateTime(calendarToUTCDate(calendar))
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
                        timePicker.show()
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
                datePickerDialog.show()
            }
            val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group)
            when(viewModel.eventType.value){
                EventType.ONLINE -> {
                    val radioButton = view.findViewById<RadioButton>(R.id.online)
                    radioButton.isChecked = true
                }
                EventType.OFFLINE -> {
                    val radioButton = view.findViewById<RadioButton>(R.id.offline)
                    radioButton.isChecked = true
                }
                else -> Unit
            }
            radioGroup.setOnCheckedChangeListener { _, checkedId ->
                val radioButton = view.findViewById<RadioButton>(checkedId)
                val typeStr = radioButton.text
                viewModel.changeType(if(typeStr.equals(getString(R.string.online))) EventType.ONLINE else EventType.OFFLINE)
            }
            bottomSheetDialog.setContentView(view)
            bottomSheetDialog.show()
        }



        viewModel.eventCreated.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }


        binding.audioContainer.audioAttachmentNested.playAudio.setOnClickListener {

            if(viewModel.attachment.value?.url != null){
                mediaObserver.playAudio(
                    Attachment(url = viewModel.attachment.value!!.url.toString(), type = AttachmentType.AUDIO),
                    binding.audioContainer.audioAttachmentNested.seekBar,
                    binding.audioContainer.audioAttachmentNested.playAudio)
            } else{
                requireContext().contentResolver.openAssetFileDescriptor(viewModel.attachment.value!!.uri!!,"r")
                    ?.let { mediaObserver.playAudioFromDescriptor(it, binding.audioContainer.audioAttachmentNested.seekBar, binding.audioContainer.audioAttachmentNested.playAudio)}
            }
        }


        binding.videoContainer.videoAttachmentNested.playVideo.setOnClickListener {
            viewModel.attachment.value?.let{attachment ->
                binding.apply {
                    videoContainer.videoAttachmentNested.videoView.isVisible = true
                    videoContainer.videoAttachmentNested.videoView.apply {
                        setMediaController(MediaController(context))
                        if(attachment.url != null){
                            val uri = Uri.parse(attachment.url)
                            setVideoURI(uri)
                        } else{
                            setVideoURI(attachment.uri)
                        }
                        setOnPreparedListener{
                            videoContainer.videoAttachmentNested.videoThumb.isVisible = false
                            videoContainer.videoAttachmentNested.playVideo.isVisible = false
                            start()
                        }
                        setOnCompletionListener {
                            stopPlayback()
                            videoContainer.videoAttachmentNested.videoView.isVisible = false
                            videoContainer.videoAttachmentNested.playVideo.isVisible = true
                            videoContainer.videoAttachmentNested.videoThumb.isVisible = true
                        }
                    }
                }
            }

        }

        viewModel.dataState.observe(viewLifecycleOwner){state ->
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .show()
                viewModel.resetError()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentBinding?.mapView?.map?.addInputListener(mapInputListener)
    }

    private fun removeAttachment(){
        viewModel.changeAttachment(null, null, null, null)
    }
    private fun removeCoords(){
        viewModel.changeCoords(null)
    }

    override fun onDestroyView() {
        fragmentBinding = null
        super.onDestroyView()
    }

    // Отображаем карты перед тем моментом, когда активити с картой станет видимой пользователю:
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        fragmentBinding?.mapView?.onStart()
    }

    // Останавливаем обработку карты, когда активити с картой становится невидимым для пользователя:
    override fun onStop() {
        fragmentBinding?.mapView?.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    //проверка разрешения на запрос местоположения
    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onMapReady()
        } else {
            checkLocationPermission.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }


    override fun onObjectAdded(p0: UserLocationView) {
        userLocationLayer.setAnchor(
            PointF((mapView.width().times(0.5)).toFloat(), (mapView.height()* 0.5).toFloat()),
            PointF((mapView.width()* 0.5).toFloat(), (mapView.height()* 0.83).toFloat())
        )
        followUserLocation = false

    }

    override fun onObjectRemoved(p0: UserLocationView) {
    }

    override fun onObjectUpdated(p0: UserLocationView, p1: ObjectEvent) {
    }

    private fun onMapReady() {
        val mapKit = MapKitFactory.getInstance()
        userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
        userLocationLayer.isVisible = true
        userLocationLayer.isHeadingEnabled = true
        userLocationLayer.setObjectListener(this)

        mapView.map.addCameraListener(this)

        cameraUserPosition()

        permissionLocation = true
    }

    //фокус на местоположение пользователя
    private fun cameraUserPosition() {
        if (userLocationLayer.cameraPosition() != null) {
            myLocation = userLocationLayer.cameraPosition()!!.target
            AndroidUtils.moveCamera(mapView, myLocation)
        } else {
            AndroidUtils.moveCamera(mapView, Point(0.0, 0.0))
        }

    }

    override fun onCameraPositionChanged(
        p0: Map,
        p1: CameraPosition,
        p2: CameraUpdateReason,
        finish: Boolean
    ) {
        if (finish) {
            if (followUserLocation) {
                setAnchor()
            }
        } else {
            if (!followUserLocation) {
                noAnchor()
            }
        }
    }

    private fun setAnchor() {
        userLocationLayer.setAnchor(
            PointF(
                (mapView.width * 0.5).toFloat(),
                (mapView.height * 0.5).toFloat()
            ),
            PointF(
                (mapView.width * 0.5).toFloat(),
                (mapView.height * 0.83).toFloat()
            )
        )

        followUserLocation = false
    }

    private fun noAnchor() {
        userLocationLayer.resetAnchor()
    }

}
