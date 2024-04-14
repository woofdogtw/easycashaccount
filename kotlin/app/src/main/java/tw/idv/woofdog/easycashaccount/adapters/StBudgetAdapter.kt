package tw.idv.woofdog.easycashaccount.adapters

import java.util.Vector

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.utils.Utils

/**
 * The adapter that contains budget statistics information.
 */
class StBudgetAdapter(private val parentContext: Activity, private val results: Vector<StItem>) :
    BaseAdapter() {
    data class StItem(
        val showRemainAvg: Boolean,
        val type: String,
        val income: Double,
        val expense: Double,
        val budget: Double,
        val remain: Double,
        val remainAvg: Double
    )

    private data class StItemHolder(
        val typeView: TextView,
        val incomeView: TextView,
        val expenseView: TextView,
        val budgetView: TextView,
        val remainView: TextView,
        val remainAvgView: TextView
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
            view = parentContext.layoutInflater.inflate(R.layout.item_st_budget, parent, false)
            holder = StItemHolder(
                view.findViewById(R.id.itemType),
                view.findViewById(R.id.itemIncome),
                view.findViewById(R.id.itemExpense),
                view.findViewById(R.id.itemBudget),
                view.findViewById(R.id.itemRemain),
                view.findViewById(R.id.itemRemainAvg)
            )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as StItemHolder
        }

        val item = results[position]
        holder.typeView.text = item.type
        holder.incomeView.text = Utils.formatMoney(item.income)
        holder.expenseView.text = Utils.formatMoney(item.expense)
        holder.budgetView.text = Utils.formatMoney(item.budget)
        holder.remainView.text = Utils.formatMoney(item.remain)
        if (item.showRemainAvg) {
            holder.remainAvgView.text = Utils.formatMoney(item.remainAvg)
        }

        return view
    }
}
