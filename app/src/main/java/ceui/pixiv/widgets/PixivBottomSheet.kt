package ceui.pixiv.widgets

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import ceui.lisa.R
import ceui.lisa.utils.Common
import ceui.loxia.FRAGMENT_RESULT_REQUEST_ID
import ceui.pixiv.ui.common.FragmentResultRequestIdOwner
import ceui.pixiv.ui.common.NavFragmentViewModel
import ceui.refactor.screenHeight
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.math.roundToInt

open class PixivBottomSheet(layoutId: Int) : BottomSheetDialogFragment(layoutId),
    FragmentResultRequestIdOwner {

    private val fragmentResultStore by activityViewModels<FragmentResultStore>()
    private val fragmentViewModel: NavFragmentViewModel by viewModels()
    protected val viewModel by activityViewModels<DialogViewModel>()

    override fun onStart() {
        super.onStart()
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.skipCollapsed = true
        behavior.maxHeight = (screenHeight * 0.75F).roundToInt()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    open fun onViewFirstCreated(view: View) {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if (fragmentViewModel.viewCreatedTime.value == null) {
            onViewFirstCreated(view)
        }

        fragmentViewModel.viewCreatedTime.value = System.currentTimeMillis()
    }

    fun <T: Any> setFragmentResult(result: T) {
        resultRequestId?.let { requestId ->
            val existingLifecycle = fragmentResultStore.getExistingLifecycle(requestId)
            if (existingLifecycle != null && existingLifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {

                Common.showLog("dsaasdw STARTED getTypedResult ${result}")
//                    fragmentResultStore.getTypedTask(requestId)?.let { task ->
//                        task.complete(FragmentResultByFragment(result, fragment))
//                        fragmentResultStore.removeResult(requestId)
//                    }
            } else {
                fragmentResultStore.putResult(requestId, result)
            }
        }
    }

    override val resultRequestId: String?
        get() = arguments?.getString(FRAGMENT_RESULT_REQUEST_ID)
    override val fragmentUniqueId: String
        get() = fragmentViewModel.fragmentUniqueId
}