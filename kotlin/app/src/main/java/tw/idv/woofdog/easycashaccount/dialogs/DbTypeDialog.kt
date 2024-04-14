package tw.idv.woofdog.easycashaccount.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText

import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.adapters.TransListAdapter
import tw.idv.woofdog.easycashaccount.adapters.TypeListAdapter

/**
 * Show the transaction type editing dialog.
 */
class DbTypeDialog(
    private val activity: Activity,
    private val type: Type,
    private val transListAdapter: TransListAdapter,
    private val typeListAdapter: TypeListAdapter,
    private val fromTypeName: String
) {
    enum class Type {
        CREATE, DELETE, MODIFY
    }

    private val dialog: AlertDialog
    private val view: View

    init {
        val title = when (type) {
            Type.CREATE -> R.string.dTypeTitleCreate
            Type.DELETE -> R.string.dTypeTitleDelete
            Type.MODIFY -> R.string.dTypeTitleModify
        }

        val inflater =
            activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        view = inflater.inflate(R.layout.dialog_db_type, null)
        dialog = AlertDialog.Builder(activity).setTitle(title).setView(view)
            .setPositiveButton(R.string.bOk, null)
            .setNeutralButton(R.string.bCancel) { dialog, _ -> dialog.dismiss() }.show()

        setupViewComponent()
    }

    private fun setupViewComponent() {
        typeEditText = view.findViewById(R.id.typeEditText)
        okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

        val editTextListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                validateInput()
            }
        }

        when (type) {
            Type.CREATE -> {}

            Type.DELETE -> {
                typeEditText.setText(fromTypeName)
                typeEditText.isEnabled = false
            }

            Type.MODIFY -> {
                typeEditText.setText(fromTypeName)
            }
        }
        typeEditText.addTextChangedListener(editTextListener)

        okButton.setOnClickListener {
            val typeName = typeEditText.text.toString().trim()
            when (type) {
                Type.CREATE -> {
                    typeListAdapter.getDbTable().addTransType(typeName)
                    typeListAdapter.update()
                    typeEditText.setText("")
                    validateInput()
                }

                Type.DELETE -> {
                    if (typeListAdapter.getDbTable().deleteTransType(typeName)) {
                        typeListAdapter.update()
                        dialog.dismiss()
                    } else {
                        dialog.dismiss()
                        WarningDialog(
                            activity,
                            0,
                            activity.getString(R.string.dTypeErrDelete)
                        ).show()
                    }
                }

                Type.MODIFY -> {
                    if (typeListAdapter.getDbTable()
                            .modifyTransType(fromTypeName, typeName, false)
                    ) {
                        typeListAdapter.update()
                        transListAdapter.update()
                        dialog.dismiss()
                    } else {
                        dialog.dismiss()
                        WarningDialog(
                            activity,
                            0,
                            activity.getString(R.string.dTypeWarnModifyMerge)
                        ) { wDialog, _ ->
                            typeListAdapter.getDbTable()
                                .modifyTransType(fromTypeName, typeName, true)
                            typeListAdapter.update()
                            transListAdapter.update()
                            wDialog.dismiss()
                        }.show()
                    }
                }
            }
        }

        validateInput()
    }

    private fun validateInput() {
        var valid = true
        typeEditText.error = null

        if (type == Type.CREATE || type == Type.MODIFY) {
            val text = typeEditText.text.toString().trim()
            if (text.isEmpty()) {
                typeEditText.error = activity.getString(R.string.dErrEmpty)
                valid = false
            } else if (text == fromTypeName) {
                valid = false
            } else if (type == Type.CREATE && typeListAdapter.getTypeListItems().contains(text)) {
                typeEditText.error = activity.getString(R.string.dTypeErrExistCreate)
                valid = false
            }
        }

        okButton.isEnabled = valid
    }

    private lateinit var typeEditText: EditText
    private lateinit var okButton: Button
}
