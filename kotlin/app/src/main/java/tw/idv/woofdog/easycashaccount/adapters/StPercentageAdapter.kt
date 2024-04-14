package tw.idv.woofdog.easycashaccount.adapters

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.utils.Utils
import java.util.Vector

/**
 * The adapter that contains percentage statistics information.
 */
class StPercentageAdapter(
    private val parentContext: Activity,
    private val results: Vector<StItem>
) :
    BaseAdapter() {
    data class StItem(
        val type: String,
        val sum: Double,
        val percentage: Double
    )

    private data class StItemHolder(
        val typeView: TextView,
        val sumView: TextView,
        val percentageView: TextView
    )

    override fun getCount(): Int {
        return results.size
    }

    override fun getItem(position: Int): Any {
        return results[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: StItemHolder

        val view: View
        if (convertView == null) {
            view = parentContext.layoutInflater.inflate(R.layout.item_st_percentage, parent, false)
            holder = StItemHolder(
                view.findViewById(R.id.itemType),
                view.findViewById(R.id.itemSum),
                view.findViewById(R.id.itemPercentage)
            )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as StItemHolder
        }

        val item = results[position]
        holder.typeView.text = item.type
        holder.sumView.text = Utils.formatMoney(item.sum)
        holder.percentageView.text = Utils.formatMoney(item.percentage)

        return view
    }
}
