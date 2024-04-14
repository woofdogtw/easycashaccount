package tw.idv.woofdog.easycashaccount.activities

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import androidx.fragment.app.Fragment

import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.adapters.TransListAdapter
import tw.idv.woofdog.easycashaccount.dialogs.TransactionDialog

class TransactionFragment(
    private val parentActivity: Activity,
    private val transListAdapter: TransListAdapter
) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_transaction, container, false)

        setupViewComponent(view)
        init = true

        return view
    }

    fun scrollToLatest() {
        if (init && transListAdapter.count > 0) {
            listView.setSelection(transListAdapter.count - 1)
        }
    }

    fun doContextItemSelected(item: MenuItem) {
        val menuInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val transItem = transListAdapter.getTransListItems()[menuInfo.position] ?: return
        when (item.itemId) {
            R.id.transCreateAsButton -> TransactionDialog(
                parentActivity,
                TransactionDialog.Type.CREATE_AS,
                transListAdapter,
                transItem.transNo
            )

            R.id.transDeleteButton -> TransactionDialog(
                parentActivity,
                TransactionDialog.Type.DELETE,
                transListAdapter,
                transItem.transNo
            )

            R.id.transModifyButton -> TransactionDialog(
                parentActivity,
                TransactionDialog.Type.MODIFY,
                transListAdapter,
                transItem.transNo
            )
        }
    }

    private fun setupViewComponent(view: View) {
        listView = view.findViewById(R.id.transListView)
        listView.adapter = transListAdapter
        listView.setOnCreateContextMenuListener { menu, _, _ ->
            parentActivity.menuInflater.inflate(R.menu.db_trans_context_menu, menu)
        }
        transListAdapter.update()
        listView.setSelection(transListAdapter.count - 1)
    }

    private lateinit var listView: ListView
    private var init = false
}
