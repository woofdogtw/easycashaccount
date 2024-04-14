package tw.idv.woofdog.easycashaccount.dialogs

import java.lang.Double
import java.util.Calendar
import java.util.Vector

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner

import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.activities.FindActivity
import tw.idv.woofdog.easycashaccount.db.DbTableBase
import tw.idv.woofdog.easycashaccount.db.DbTransaction

/**
 * Find transactions with conditions.
 */
class FindDialog(
    private val activity: Activity,
    private val dbTable: DbTableBase
) {
    private val dialog: AlertDialog
    private val view: View

    init {
        val inflater =
            activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflater.inflate(R.layout.dialog_find, null)
        dialog = AlertDialog.Builder(activity).setTitle(R.string.dFindTitle).setView(view)
            .setPositiveButton(R.string.bOk, null)
            .setNeutralButton(R.string.bCancel) { dialog, _ -> dialog.dismiss() }.show()

        setupViewComponent()
    }

    private fun setupViewComponent() {
        dateCheckBox = view.findViewById(R.id.dateCheckBox)
        typeCheckBox = view.findViewById(R.id.typeCheckBox)
        inExpCheckBox = view.findViewById(R.id.inExpCheckBox)
        moneyCheckBox = view.findViewById(R.id.moneyCheckBox)
        descriptCheckBox = view.findViewById(R.id.descriptCheckBox)
        locationCheckBox = view.findViewById(R.id.locationCheckBox)
        fromDatePicker = view.findViewById(R.id.fromDatePicker)
        toDatePicker = view.findViewById(R.id.toDatePicker)
        typeText = view.findViewById(R.id.typeEditText)
        inExpSpinner = view.findViewById(R.id.inExpSpinner)
        fromMoneyText = view.findViewById(R.id.fromMoneyEditText)
        toMoneyText = view.findViewById(R.id.toMoneyEditText)
        descriptText = view.findViewById(R.id.descriptEditText)
        locationText = view.findViewById(R.id.locationEditText)

        val editTextListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
        }

        view.findViewById<RadioButton>(R.id.allButton).isChecked = true
        dateCheckBox.setOnClickListener { validateInput() }
        typeCheckBox.setOnClickListener { validateInput() }
        inExpCheckBox.setOnClickListener { validateInput() }
        moneyCheckBox.setOnClickListener { validateInput() }
        descriptCheckBox.setOnClickListener { validateInput() }
        locationCheckBox.setOnClickListener { validateInput() }
        typeText.addTextChangedListener(editTextListener)
        fromMoneyText.addTextChangedListener(editTextListener)
        toMoneyText.addTextChangedListener(editTextListener)
        descriptText.addTextChangedListener(editTextListener)
        locationText.addTextChangedListener(editTextListener)

        val date = Calendar.getInstance()
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
        val str = arrayOf(
            activity.getString(R.string.transIncome),
            activity.getString(R.string.transExpense),
            activity.getString(R.string.transBudget)
        )
        val spinnerAdapter =
            ArrayAdapter(activity, android.R.layout.simple_spinner_item, str)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        inExpSpinner.adapter = spinnerAdapter
        inExpSpinner.setSelection(1)
        okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

        okButton.setOnClickListener {
            dialog.dismiss()
            doFind()
        }

        validateInput()
    }

    private fun doFind() {
        val cond = DbTableBase.QueryCond()

        if (dateCheckBox.isChecked) {
            cond.fromDate = DbTableBase.getDateFromYMD(
                fromDatePicker.year,
                fromDatePicker.month + 1,
                fromDatePicker.dayOfMonth
            )
            cond.toDate = DbTableBase.getDateFromYMD(
                toDatePicker.year,
                toDatePicker.month + 1,
                toDatePicker.dayOfMonth
            )
        }
        if (typeCheckBox.isChecked) {
            cond.type = typeText.text.toString()
        }
        if (inExpCheckBox.isChecked) {
            cond.inExp = when (inExpSpinner.selectedItemPosition) {
                DbTransaction.InExp.INCOME.v -> DbTransaction.InExp.INCOME
                DbTransaction.InExp.BUDGET.v -> DbTransaction.InExp.BUDGET
                else -> DbTransaction.InExp.EXPENSE
            }
        }
        if (moneyCheckBox.isChecked) {
            if (fromMoneyText.text.toString().isNotEmpty()) {
                cond.fromMoney = Double.parseDouble(fromMoneyText.text.toString())
            }
            if (toMoneyText.text.toString().isNotEmpty()) {
                cond.toMoney = Double.parseDouble(toMoneyText.text.toString())
            }
        }
        if (descriptCheckBox.isChecked) {
            cond.description = descriptText.text.toString()
        }
        if (locationCheckBox.isChecked) {
            cond.location = locationText.text.toString()
        }

        val results = if (view.findViewById<RadioButton>(R.id.allButton).isChecked) {
            dbTable.queryAND(cond)
        } else {
            dbTable.queryOR(cond)
        } ?: Vector<DbTransaction>()

        val intent = Intent(activity, FindActivity::class.java)
        intent.putExtra("results", results)
        activity.startActivity(intent)
    }

    private fun validateInput() {
        var valid = true

        typeText.error = null
        fromMoneyText.error = null
        toMoneyText.error = null
        descriptText.error = null
        locationText.error = null

        if (!dateCheckBox.isChecked &&
            !typeCheckBox.isChecked &&
            !inExpCheckBox.isChecked &&
            !moneyCheckBox.isChecked &&
            !descriptCheckBox.isChecked &&
            !locationCheckBox.isChecked
        ) {
            valid = false
        }

        if (typeCheckBox.isChecked) {
            if (typeText.text.toString().isEmpty()) {
                valid = false
                typeText.error = activity.getString(R.string.dErrEmpty)
            }
        }
        if (moneyCheckBox.isChecked) {
            if (fromMoneyText.text.toString().isEmpty()) {
                if (toMoneyText.text.toString().isEmpty()) {
                    valid = false
                    fromMoneyText.error = activity.getString(R.string.dErrEmpty)
                    toMoneyText.error = activity.getString(R.string.dErrEmpty)
                } else {
                    try {
                        val v = Double.parseDouble(toMoneyText.text.toString())
                        if (v < 0.0) {
                            valid = false
                            toMoneyText.error = activity.getString(R.string.dTransErrMoney)
                        }
                    } catch (e: NumberFormatException) {
                        valid = false
                        toMoneyText.error = activity.getString(R.string.dTransErrMoney)
                    }
                }
            } else {
                var fromMoney = -1.0
                var toMoney = -1.0
                try {
                    fromMoney = Double.parseDouble(fromMoneyText.text.toString())
                    if (fromMoney < 0.0) {
                        valid = false
                        fromMoneyText.error = activity.getString(R.string.dTransErrMoney)
                    }
                } catch (e: NumberFormatException) {
                    valid = false
                    fromMoneyText.error = activity.getString(R.string.dTransErrMoney)
                }
                if (toMoneyText.text.toString().isNotEmpty()) {
                    try {
                        toMoney = Double.parseDouble(toMoneyText.text.toString())
                        if (toMoney < 0.0) {
                            valid = false
                            toMoneyText.error = activity.getString(R.string.dTransErrMoney)
                        }
                    } catch (e: NumberFormatException) {
                        valid = false
                        toMoneyText.error = activity.getString(R.string.dTransErrMoney)
                    }
                    if (valid && toMoney < fromMoney) {
                        valid = false
                        fromMoneyText.error = activity.getString(R.string.dFindErrMoney)
                        toMoneyText.error = activity.getString(R.string.dFindErrMoney)
                    }
                }
            }
        }
        if (descriptCheckBox.isChecked) {
            if (descriptText.text.toString().isEmpty()) {
                valid = false
                descriptText.error = activity.getString(R.string.dErrEmpty)
            }
        }
        if (locationCheckBox.isChecked) {
            if (locationText.text.toString().isEmpty()) {
                valid = false
                locationText.error = activity.getString(R.string.dErrEmpty)
            }
        }

        okButton.isEnabled = valid
    }

    private lateinit var dateCheckBox: CheckBox
    private lateinit var typeCheckBox: CheckBox
    private lateinit var inExpCheckBox: CheckBox
    private lateinit var moneyCheckBox: CheckBox
    private lateinit var descriptCheckBox: CheckBox
    private lateinit var locationCheckBox: CheckBox
    private lateinit var fromDatePicker: DatePicker
    private lateinit var toDatePicker: DatePicker
    private lateinit var typeText: EditText
    private lateinit var inExpSpinner: Spinner
    private lateinit var fromMoneyText: EditText
    private lateinit var toMoneyText: EditText
    private lateinit var descriptText: EditText
    private lateinit var locationText: EditText
    private lateinit var okButton: Button
}
