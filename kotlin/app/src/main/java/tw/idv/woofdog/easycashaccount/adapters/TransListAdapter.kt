package tw.idv.woofdog.easycashaccount.adapters

import java.util.Locale
import java.util.Vector

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.db.DbTableBase
import tw.idv.woofdog.easycashaccount.db.DbTransaction
import tw.idv.woofdog.easycashaccount.utils.Utils

/**
 * The adapter that contains display transaction information.
 */
class TransListAdapter(private val parentContext: Activity, private val dbTable: DbTableBase) :
    BaseAdapter() {

    private data class TransItemHolder(
        val dateView: TextView,
        val typeView: TextView,
        val inExpView: TextView,
        val moneyView: TextView,
        val descriptView: TextView,
        val locationView: TextView
    )

    override fun getCount(): Int {
        return transListItems.size
    }

    override fun getItem(position: Int): Any {
        return transListItems[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val holder: TransItemHolder

        val view: View
        if (convertView == null) {
            view = parentContext.layoutInflater.inflate(R.layout.item_trans_list, parent, false)
            holder = TransItemHolder(
                view.findViewById(R.id.dateTextView),
                view.findViewById(R.id.typeTextView),
                view.findViewById(R.id.inExpTextView),
                view.findViewById(R.id.moneyTextView),
                view.findViewById(R.id.descriptTextView),
                view.findViewById(R.id.locationTextView)
            )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as TransItemHolder
        }

        val item = transListItems[position]
        val date = item.transDate
        val dateStr = String.format(
            Locale.getDefault(), "%d/%d/%d",
            DbTableBase.getYearFromDate(date),
            DbTableBase.getMonthFromDate(date),
            DbTableBase.getDayFromDate(date)
        )
        holder.dateView.text = dateStr
        holder.typeView.text = item.transType
        holder.inExpView.text = convertInExp(item.transInExp)
        holder.moneyView.text = Utils.formatMoney(item.transMoney)
        holder.descriptView.text = item.transDescription
        holder.locationView.text =
            if (item.transLocation.isEmpty()) "" else "[${item.transLocation}]"

        return view
    }

    fun update() {
        transListItems = dbTable.getTransactions() ?: return
        notifyDataSetChanged()
    }

    fun getDbTable(): DbTableBase {
        return dbTable
    }

    fun getTransListItems(): Vector<DbTransaction> {
        return transListItems
    }

    private fun convertInExp(inExp: DbTransaction.InExp): String {
        return when (inExp) {
            DbTransaction.InExp.INCOME -> parentContext.getString(R.string.transIncome)
            DbTransaction.InExp.EXPENSE -> parentContext.getString(R.string.transExpense)
            DbTransaction.InExp.BUDGET -> parentContext.getString(R.string.transBudget)
        }
    }

    private var transListItems: Vector<DbTransaction> = Vector<DbTransaction>()
}
