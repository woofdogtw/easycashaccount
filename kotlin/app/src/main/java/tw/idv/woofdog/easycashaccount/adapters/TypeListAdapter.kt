package tw.idv.woofdog.easycashaccount.adapters

import java.util.Vector

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.db.DbTableBase

/**
 * The adapter that contains display transaction type information.
 */
class TypeListAdapter(private val parentContext: Activity, private val dbTable: DbTableBase) :
    BaseAdapter() {

    private data class TypeItemHolder(val typeView: TextView)

    override fun getCount(): Int {
        return typeListItems.size
    }

    override fun getItem(position: Int): Any {
        return typeListItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: TypeItemHolder

        val view: View
        if (convertView == null) {
            view = parentContext.layoutInflater.inflate(R.layout.item_type_list, parent, false)
            holder =
                TypeItemHolder(view.findViewById(R.id.typeTextView))
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as TypeItemHolder
        }

        val item = typeListItems[position]
        holder.typeView.text = item

        return view
    }

    fun update() {
        typeListItems = dbTable.getTransTypes() ?: return
        notifyDataSetChanged()
    }

    fun getDbTable(): DbTableBase {
        return dbTable
    }

    fun getTypeListItems(): Vector<String> {
        return typeListItems
    }

    private var typeListItems: Vector<String> = Vector<String>()
}
