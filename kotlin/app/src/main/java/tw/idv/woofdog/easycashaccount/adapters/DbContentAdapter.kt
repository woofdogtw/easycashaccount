package tw.idv.woofdog.easycashaccount.adapters

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

import tw.idv.woofdog.easycashaccount.activities.DbTypeFragment
import tw.idv.woofdog.easycashaccount.activities.TransactionFragment

class DbContentAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val parentActivity: Activity,
    private val typeListAdapter: TypeListAdapter,
    private val transListAdapter: TransListAdapter
) :
    FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> {
                typeFragment = DbTypeFragment(parentActivity, typeListAdapter, transListAdapter)
                typeFragment
            }

            else -> {
                transFragment = TransactionFragment(parentActivity, transListAdapter)
                transFragment
            }
        }
    }

    lateinit var typeFragment: DbTypeFragment
    lateinit var transFragment: TransactionFragment
}
