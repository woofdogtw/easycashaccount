package tw.idv.woofdog.easycashaccount.dialogs

import java.util.Calendar

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import android.widget.RadioButton

import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.activities.StBudgetActivity
import tw.idv.woofdog.easycashaccount.activities.StGrandTotalActivity
import tw.idv.woofdog.easycashaccount.activities.StPercentageActivity
import tw.idv.woofdog.easycashaccount.db.DbTableBase

/**
 * Show the range dialog for displaying range.
 */
class StRangeDialog(
    private val activity: Activity,
    private val type: Type,
    private val dbTable: DbTableBase
) {
    enum class Type {
        BUDGET, GRANT_TOTAL, PERCENTAGE
    }

    private val dialog: AlertDialog
    private val view: View

    init {
        val title = when (type) {
            Type.BUDGET -> R.string.titleStBudget
            Type.GRANT_TOTAL -> R.string.titleStGrandTotal
            Type.PERCENTAGE -> R.string.titleStPercentage
        }
        val inflater =
            activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflater.inflate(R.layout.dialog_st_range, null)
        dialog = AlertDialog.Builder(activity).setTitle(title).setView(view)
            .setPositiveButton(R.string.bOk, null)
            .setNeutralButton(R.string.bCancel) { dialog, _ -> dialog.dismiss() }.show()

        setupViewComponent()
    }

    private fun setupViewComponent() {
        val useButton = view.findViewById<RadioButton>(R.id.allButton)
        useButton.isChecked = true

        val date = Calendar.getInstance()
        fromDatePicker = view.findViewById(R.id.fromDatePicker)
        fromDatePicker.init(date.get(Calendar.YEAR), date.get(Calendar.MONTH), 1, null)
        fromDatePicker.setOnDateChangedListener { _, y, m, d ->
            val from = DbTableBase.getDateFromYMD(y, m + 1, d)
            val to = DbTableBase.getDateFromYMD(
                toDatePicker.year,
                toDatePicker.month + 1,
                toDatePicker.dayOfMonth
            )
            if (from > to) {
                toDatePicker.updateDate(y, m, d)
            }
        }
        toDatePicker = view.findViewById(R.id.toDatePicker)
        toDatePicker.init(
            date.get(Calendar.YEAR), date.get(Calendar.MONTH),
            date.getActualMaximum(Calendar.DAY_OF_MONTH), null
        )
        toDatePicker.setOnDateChangedListener { _, y, m, d ->
            val from = DbTableBase.getDateFromYMD(
                fromDatePicker.year,
                fromDatePicker.month + 1,
                fromDatePicker.dayOfMonth
            )
            val to = DbTableBase.getDateFromYMD(y, m + 1, d)
            if (to < from) {
                fromDatePicker.updateDate(y, m, d)
            }
        }
        okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

        okButton.setOnClickListener {
            dialog.dismiss()
            doStatistics()
        }
    }

    private fun doStatistics() {
        var minDate = -1
        var maxDate = -1

        if (!view.findViewById<RadioButton>(R.id.allButton).isChecked) {
            minDate = DbTableBase.getDateFromYMD(
                fromDatePicker.year,
                fromDatePicker.month + 1,
                fromDatePicker.dayOfMonth
            )
            maxDate = DbTableBase.getDateFromYMD(
                toDatePicker.year,
                toDatePicker.month + 1,
                toDatePicker.dayOfMonth
            )
        }

        val intent = when (type) {
            Type.BUDGET -> Intent(activity, StBudgetActivity::class.java)
            Type.GRANT_TOTAL -> Intent(activity, StGrandTotalActivity::class.java)
            Type.PERCENTAGE -> Intent(activity, StPercentageActivity::class.java)
        }
        intent.putExtra("dbName", dbTable.getFileName())
        intent.putExtra("minDate", minDate)
        intent.putExtra("maxDate", maxDate)
        activity.startActivity(intent)
    }

    private lateinit var fromDatePicker: DatePicker
    private lateinit var toDatePicker: DatePicker
    private lateinit var okButton: Button
}
