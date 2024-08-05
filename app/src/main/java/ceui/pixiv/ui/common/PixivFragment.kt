package ceui.pixiv.ui.common

import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import ceui.lisa.R
import ceui.lisa.databinding.FragmentPixivListBinding
import ceui.lisa.databinding.LayoutToolbarBinding
import ceui.lisa.view.SpacesItemDecoration
import ceui.loxia.Illust
import ceui.loxia.RefreshHint
import ceui.loxia.RefreshState
import ceui.loxia.pushFragment
import ceui.pixiv.ui.list.PixivListViewModel
import ceui.pixiv.ui.works.IllustFragmentArgs
import ceui.refactor.ppppx
import ceui.refactor.setOnClick
import com.scwang.smart.refresh.header.FalsifyFooter
import com.scwang.smart.refresh.header.MaterialHeader

open class PixivFragment(layoutId: Int) : Fragment(layoutId), IllustCardActionReceiver {

    override fun onClickIllustCard(illust: Illust) {
        pushFragment(
            R.id.navigation_illust,
            IllustFragmentArgs(illust.id).toBundle()
        )
    }
}

interface ViewPagerFragment {

}

fun Fragment.setUpToolbar(binding: LayoutToolbarBinding) {
    if (parentFragment is ViewPagerFragment) {
        binding.toolbarLayout.isVisible = false
    } else {
        binding.toolbarLayout.isVisible = true
        binding.naviBack.setOnClick {
            findNavController().popBackStack()
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbarLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }
    }
}

fun Fragment.setUpRefreshState(binding: FragmentPixivListBinding, viewModel: PixivListViewModel<*, *>) {
    val ctx = requireContext()
    setUpToolbar(binding.toolbarLayout)
    binding.refreshLayout.setRefreshHeader(MaterialHeader(ctx))
    binding.refreshLayout.setOnRefreshListener {
        viewModel.refresh(RefreshHint.pullToRefresh())
    }
    viewModel.refreshState.observe(viewLifecycleOwner) { state ->
        if (state !is RefreshState.LOADING) {
            binding.refreshLayout.finishRefresh()
            binding.refreshLayout.finishLoadMore()
        }
        binding.emptyLayout.isVisible = state is RefreshState.LOADED && !state.hasContent
        if (state is RefreshState.LOADED) {
            if (state.hasNext) {
                binding.refreshLayout.setOnLoadMoreListener {
                    viewModel.loadMore()
                }
            } else {
                binding.refreshLayout.setRefreshFooter(FalsifyFooter(ctx))
            }
        }
        binding.loadingLayout.isVisible = state is RefreshState.LOADING && state.refreshHint == RefreshHint.initialLoad()
    }
}

fun Fragment.setUpStaggerLayout(binding: FragmentPixivListBinding, viewModel: PixivListViewModel<*, *>) {
    setUpRefreshState(binding, viewModel)
    binding.listView.addItemDecoration(SpacesItemDecoration(4.ppppx))
    binding.listView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
    val adapter = CommonAdapter(viewLifecycleOwner)
    binding.listView.adapter = adapter
    viewModel.holders.observe(viewLifecycleOwner) { holders ->
        adapter.submitList(holders)
    }
}

fun Fragment.setUpLinearLayout(binding: FragmentPixivListBinding, viewModel: PixivListViewModel<*, *>) {
    val ctx = requireContext()
    setUpRefreshState(binding, viewModel)
    val adapter = CommonAdapter(viewLifecycleOwner)
    binding.listView.layoutManager = LinearLayoutManager(ctx)
    binding.listView.adapter = adapter
    viewModel.holders.observe(viewLifecycleOwner) { holders ->
        adapter.submitList(holders)
    }
}