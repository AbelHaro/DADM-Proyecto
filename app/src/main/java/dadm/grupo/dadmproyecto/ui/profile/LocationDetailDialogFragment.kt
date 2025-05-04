package dadm.grupo.dadmproyecto.ui.profile

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import dadm.grupo.dadmproyecto.databinding.DialogLocationDetailBinding
import dadm.grupo.dadmproyecto.domain.model.Location

class LocationDetailDialogFragment : DialogFragment() {

    private var _binding: DialogLocationDetailBinding? = null
    private val binding get() = _binding!!

    private var location: Location? = null
    private var isVisited: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            location = Location(
                id = it.getLong(ARG_LOCATION_ID),
                createdAt = "",
                name = it.getString(ARG_LOCATION_NAME) ?: "",
                description = it.getString(ARG_LOCATION_DESC) ?: "",
                latitude = 0.0,
                longitude = 0.0,
                radius = 0.0,
                imageUrl = it.getString(ARG_LOCATION_IMAGE_URL) ?: ""
            )
            isVisited = it.getBoolean(ARG_IS_VISITED)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLocationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageSize = 400 // Adjust this value as needed

        location?.let { loc ->
            binding.txtLocationNameDetail.text = loc.name
            binding.txtLocationDescriptionDetail.text =
                if (isVisited) loc.description else "Lugar por descubrir"

            Glide.with(this)
                .load(loc.imageUrl)
                .override(imageSize, imageSize)
                .centerCrop()
                .into(binding.imgLocationDetail)

            if (!isVisited) {
                val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
                binding.imgLocationDetail.colorFilter = ColorMatrixColorFilter(colorMatrix)
            } else {
                binding.imgLocationDetail.clearColorFilter()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_LOCATION_ID = "location_id"
        private const val ARG_LOCATION_NAME = "location_name"
        private const val ARG_LOCATION_DESC = "location_desc"
        private const val ARG_LOCATION_IMAGE_URL = "location_image_url"
        private const val ARG_IS_VISITED = "is_visited"

        fun newInstance(location: Location, isVisited: Boolean): LocationDetailDialogFragment {
            val fragment = LocationDetailDialogFragment()
            val args = Bundle().apply {
                putLong(ARG_LOCATION_ID, location.id)
                putString(ARG_LOCATION_NAME, location.name)
                putString(ARG_LOCATION_DESC, location.description)
                putString(ARG_LOCATION_IMAGE_URL, location.imageUrl)
                putBoolean(ARG_IS_VISITED, isVisited)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
