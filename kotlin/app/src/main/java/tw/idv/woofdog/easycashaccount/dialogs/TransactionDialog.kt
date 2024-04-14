package tw.idv.woofdog.easycashaccount.dialogs

import java.lang.Double.parseDouble
import java.lang.Exception
import java.util.Calendar
import java.util.Vector

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Spinner

import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.adapters.TransListAdapter
import tw.idv.woofdog.easycashaccount.db.DbTableBase
import tw.idv.woofdog.easycashaccount.db.DbTransaction
import tw.idv.woofdog.easycashaccount.utils.Utils

/**
 * Show the transaction editing dialog.
 */
class TransactionDialog(
    private val activity: Activity,
    private val type: Type,
    private val transListAdapter: TransListAdapter,
    private val transNo: Long
) {
    enum class Type {
        CREATE, CREATE_AS, DELETE, MODIFY
    }

    private val dialog: AlertDialog
    private val view: View

    init {
        val title = when (type) {
            Type.CREATE, Type.CREATE_AS -> R.string.dTransTitleCreate
            Type.DELETE -> R.string.dTransTitleDelete
            Type.MODIFY -> R.string.dTransTitleModify
        }

        val inflater =
            activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflater.inflate(R.layout.dialog_transaction, null)
        dialog = AlertDialog.Builder(activity).setTitle(title).setView(view)
            .setPositiveButton(R.string.bOk, null)
            .setNeutralButton(R.string.bCancel) { dialog, _ -> dialog.dismiss() }.show()

        setupViewComponent()
    }

    private fun setupViewComponent() {
        val datePicker = view.findViewById<DatePicker>(R.id.transDatePicker)
        val typeSpinner = view.findViewById<Spinner>(R.id.transTypeSpinner)
        val inExpSpinner = view.findViewById<Spinner>(R.id.transInExpSpinner)
        val moneyText = view.findViewById<EditText>(R.id.transMoneyEditText)
        val descriptText = view.findViewById<EditText>(R.id.transDescriptEditText)
        val locationText = view.findViewById<EditText>(R.id.transLocationEditText)
        okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

        setupTransComponent()

        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                validateInput()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }
        val editTextListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
        }
        typeSpinner.onItemSelectedListener = spinnerListener
        moneyText.addTextChangedListener(editTextListener)

        okButton.setOnClickListener {
            if (type == Type.DELETE) {
                transListAdapter.getDbTable().deleteTransaction(transNo)
                transListAdapter.update()
                dialog.dismiss()
                return@setOnClickListener
            }

            val trans = when (type) {
                Type.CREATE, Type.CREATE_AS -> DbTransaction()
                else -> DbTransaction(transNo)
            }
            trans.transDate = DbTableBase.getDateFromYMD(
                datePicker.year,
                datePicker.month + 1,
                datePicker.dayOfMonth
            )
            trans.transType = typeSpinner.selectedItem.toString()
            trans.transInExp = when (inExpSpinner.selectedItemPosition) {
                DbTransaction.InExp.INCOME.v -> DbTransaction.InExp.INCOME
                DbTransaction.InExp.BUDGET.v -> DbTransaction.InExp.BUDGET
                else -> DbTransaction.InExp.EXPENSE
            }
            trans.transMoney = try {
                parseDouble(moneyText.text.toString())
            } catch (e: Exception) {
                0.0
            }
            trans.transDescription = descriptText.text.toString()
            trans.transLocation = locationText.text.toString()

            when (type) {
                Type.CREATE -> {
                    transListAdapter.getDbTable().addTransaction(trans)
                    transListAdapter.update()

                    typeSpinner.setSelection(0)
                    inExpSpinner.setSelection(DbTransaction.InExp.EXPENSE.v)
                    moneyText.setText("")
                    descriptText.setText("")
                    locationText.setText("")
                }

                Type.CREATE_AS -> {
                    transListAdapter.getDbTable().addTransaction(trans)
                    transListAdapter.update()
                    dialog.dismiss()
                }

                else -> {
                    transListAdapter.getDbTable().modifyTransaction(transNo, trans)
                    transListAdapter.update()
                    dialog.dismiss()
                }
            }
        }

        validateInput()
    }

    private fun setupTransComponent() {
        val typeSpinner: Spinner = view.findViewById(R.id.transTypeSpinner)
        val inExpSpinner: Spinner = view.findViewById(R.id.transInExpSpinner)
        val types = transListAdapter.getDbTable().getTransTypes() ?: Vector<String>(0)
        val size = types.size
        var str = Array(size + 1) { "" }
        str[0] = activity.getString(R.string.dTransSelType)
        for (i in 0..<size) {
            str[i + 1] = types[i]
        }
        var spinnerAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, str)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        typeSpinner.adapter = spinnerAdapter
        typeSpinner.setSelection(0)
        str = arrayOf(
            activity.getString(R.string.transIncome),
            activity.getString(R.string.transExpense),
            activity.getString(R.string.transBudget)
        )
        spinnerAdapter =
            ArrayAdapter(activity, android.R.layout.simple_spinner_item, str)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        inExpSpinner.adapter = spinnerAdapter
        inExpSpinner.setSelection(if (type == Type.CREATE) 1 else 0)

        val datePicker = view.findViewById<DatePicker>(R.id.transDatePicker)
        if (type == Type.CREATE) {
            val date = Calendar.getInstance()
            datePicker.init(
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH),
                date.get(Calendar.DATE),
                null
            )
        } else {
            val trans = transListAdapter.getDbTable().getTransaction(transNo) ?: return

            if (type == Type.CREATE_AS) {
                val date = Calendar.getInstance()
                datePicker.init(
                    date.get(Calendar.YEAR),
                    date.get(Calendar.MONTH),
                    date.get(Calendar.DATE),
                    null
                )
            } else {
                val date = trans.transDate
                datePicker.init(
                    DbTableBase.getYearFromDate(date),
                    DbTableBase.getMonthFromDate(date) - 1,
                    DbTableBase.getDayFromDate(date),
                    null
                )
            }

            for (i in 0..<types.size) {
                if (types[i] == trans.transType) {
                    typeSpinner.setSelection(i + 1)
                    break
                }
            }
            inExpSpinner.setSelection(trans.transInExp.v)

            val moneyEditText = view.findViewById<EditText>(R.id.transMoneyEditText)
            moneyEditText.setText(Utils.formatMoney(trans.transMoney))
            val descEditText = view.findViewById<EditText>(R.id.transDescriptEditText)
            descEditText.setText(trans.transDescription)
            val locEditText = view.findViewById<EditText>(R.id.transLocationEditText)
            locEditText.setText(trans.transLocation)

            if (type == Type.DELETE) {
                datePicker.isEnabled = false
                typeSpinner.isEnabled = false
                inExpSpinner.isEnabled = false
                moneyEditText.isEnabled = false
                descEditText.isEnabled = false
                locEditText.isEnabled = false
            }
        }
    }

    private fun validateInput() {
        var valid = true
        val typeSpinner = view.findViewById<Spinner>(R.id.transTypeSpinner)
        val moneyText = view.findViewById<EditText>(R.id.transMoneyEditText)
        moneyText.error = null

        if (typeSpinner.selectedItemPosition == 0) {
            valid = false
        }
        try {
            val v = parseDouble(moneyText.text.toString())
            if (v < 0.0) {
                valid = false
                moneyText.error = activity.getString(R.string.dTransErrMoney)
            }
        } catch (e: NumberFormatException) {
            valid = false
            moneyText.error = activity.getString(R.string.dTransErrMoney)
        }

        okButton.isEnabled = valid
    }

    private lateinit var okButton: Button
}
