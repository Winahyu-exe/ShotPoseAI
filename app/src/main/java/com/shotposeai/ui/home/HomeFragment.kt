package com.shotposeai.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shotposeai.R
import com.shotposeai.data.SessionStore
import com.shotposeai.data.DatabaseProvider
import com.shotposeai.analysis.AchievementRules
import com.shotposeai.ui.common.AvatarStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.shotposeai.databinding.FragmentHomeBinding
import com.shotposeai.ui.analysis.AnalysisActivity
import com.shotposeai.ui.profile.ProfileActivity
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.btnCamera.setOnClickListener {
            openStudentPicker(AnalysisActivity.MODE_CAMERA)
        }

        binding.btnVideo.setOnClickListener {
            openStudentPicker(AnalysisActivity.MODE_VIDEO)
        }

        binding.btnClasses.setOnClickListener { requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_classes }
        binding.btnStudents.setOnClickListener { requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.nav_students }
        binding.btnProfile.setOnClickListener { startActivity(Intent(requireContext(), ProfileActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            val session = SessionStore(requireContext()).current()
            val data = withContext(Dispatchers.IO) {
                val dao = DatabaseProvider.get(requireContext()).appDao()
                HomeSnapshot(dao.classCount(), dao.studentCount(),
                    dao.completedStudentCount("Dribbling", AchievementRules.REQUIRED_SCORE),
                    session?.let { dao.getUser(it.userId)?.profilePhotoPath })
            }
            val b = _binding ?: return@launch
            b.tvClassCount.text = "${data.classes} Kelas"
            b.tvStudentCount.text = "${data.students} Siswa"
            b.tvProgress.text = "${data.completed}/${data.students} Siswa • target skor 8,5/10"
            b.progressBadge.progress = AchievementRules.percentage(data.completed, data.students)
            b.progressBadge.contentDescription = "${data.completed} dari ${data.students} siswa mencapai achievement Dribbling"
            AvatarStore.bind(b.btnProfile, data.photo, viewLifecycleOwner.lifecycleScope)
        }
    }
    private data class HomeSnapshot(val classes: Int, val students: Int, val completed: Int, val photo: String?)

    private fun openStudentPicker(mode: String) {
        startActivity(Intent(requireContext(), StudentPickerActivity::class.java).putExtra(StudentPickerActivity.EXTRA_MODE, mode))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
