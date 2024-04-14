package tw.idv.woofdog.easycashaccount.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import tw.idv.woofdog.easycashaccount.activities.StPercentageActivity

class StPercentageTabAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val incomeAdapter: StPercentageAdapter,
    private val expenseAdapter: StPercentageAdapter,
    private val budgetAdapter: StPercentageAdapter
) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> {
                StPercentageActivity.StFragment(incomeAdapter)
            }
            2 -> {
                StPercentageActivity.StFragment(budgetAdapter)
            }
            else -> {
                StPercentageActivity.StFragment(expenseAdapter)
            }
        }
    }
}
