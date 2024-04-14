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
import tw.idv.woofdog.easycashaccount.adapters.TypeListAdapter
import tw.idv.woofdog.easycashaccount.dialogs.DbTypeDialog

class DbTypeFragment(
    private val parentActivity: Activity,
    private val typeListAdapter: TypeListAdapter,
    private val transListAdapter: TransListAdapter
) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_db_type, container, false)

        setupViewComponent(view)

        return view
    }

    fun doContextItemSelected(item: MenuItem) {
        val menuInfo = item.menuInfo as AdapterView.AdapterContextMenuInfo
        val typeItem = typeListAdapter.getTypeListItems()[menuInfo.position] ?: return
        when (item.itemId) {
            R.id.typeDeleteButton -> DbTypeDialog(
                parentActivity,
                DbTypeDialog.Type.DELETE,
                transListAdapter,
                typeListAdapter,
                typeItem
            )

            R.id.typeModifyButton -> DbTypeDialog(
                parentActivity,
                DbTypeDialog.Type.MODIFY,
                transListAdapter,
                typeListAdapter,
                typeItem
            )
        }
    }

    private fun setupViewComponent(view: View) {
        val listView = view.findViewById<ListView>(R.id.typeListView)
        listView.adapter = typeListAdapter
        listView.setOnCreateContextMenuListener { menu, _, _ ->
            parentActivity.menuInflater.inflate(R.menu.db_type_context_menu, menu)
        }
        typeListAdapter.update()
    }
}
