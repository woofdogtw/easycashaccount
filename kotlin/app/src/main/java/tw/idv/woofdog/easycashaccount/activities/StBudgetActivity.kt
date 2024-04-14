package tw.idv.woofdog.easycashaccount.activities

import java.util.Calendar
import java.util.Vector

import android.os.Bundle
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.adapters.StBudgetAdapter
import tw.idv.woofdog.easycashaccount.db.DbTableBase
import tw.idv.woofdog.easycashaccount.db.DbTableSQLite
import tw.idv.woofdog.easycashaccount.db.DbTransaction
import tw.idv.woofdog.easycashaccount.utils.Utils

/**
 * The activity to display budget analysis information.
 */
class StBudgetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.titleStBudget)
        setContentView(R.layout.activity_st_budget)

        val dbName = intent.getStringExtra("dbName") ?: ""
        minDate = intent.getIntExtra("minDate", -1)
        maxDate = intent.getIntExtra("maxDate", -1)

        val dbTable = DbTableSQLite()
        dbTable.setFileName(dbName)

        setupAdapter(dbTable.getTransactions() ?: Vector<DbTransaction>())
        setupViewComponent()

        dbTable.setFileName("")
    }

    private fun setupAdapter(trans: Vector<DbTransaction>) {
        val isRangeSet = minDate > -1
        var dateDiff = -1

        // Values for list view contents.
        val typeList = mutableListOf<String>()
        val incomeSum = mutableMapOf<String, Double>()
        val expenseSum = mutableMapOf<String, Double>()
        val budgetSum = mutableMapOf<String, Double>()
        var incomeTotalSum = 0.0
        var expenseTotalSum = 0.0
        var budgetTotalSum = 0.0

        // Calculate income, expense, budget for all types.
        for (t in trans) {
            if (isRangeSet && (t.transDate < minDate || t.transDate > maxDate)) {
                continue
            }

            // Values for list view contents.
            val type = t.transType
            if (!typeList.contains(type)) {
                typeList.add(type)
                incomeSum[type] = 0.0
                expenseSum[type] = 0.0
                budgetSum[type] = 0.0
            }

            // Calculate income, expense, budget for all types.
            val money = t.transMoney
            when (t.transInExp) {
                DbTransaction.InExp.INCOME -> {
                    incomeSum[type]?.let { incomeSum[type] = it + money }
                    incomeTotalSum += money
                }

                DbTransaction.InExp.EXPENSE -> {
                    expenseSum[type]?.let { expenseSum[type] = it + money }
                    expenseTotalSum += money
                }

                DbTransaction.InExp.BUDGET -> {
                    budgetSum[type]?.let { budgetSum[type] = it + money }
                    budgetTotalSum += money
                }
            }
        }
        typeList.sort()

        // Adjust date range.
        if (isRangeSet) {
            val todayDate = DbTableBase.getDateFromCalendar(Calendar.getInstance())
            if (todayDate <= maxDate) {
                dateDiff = maxDate - todayDate + 1
            }
        }

        // Hide the average remaining.
        if (dateDiff <= 0) {
            findViewById<TextView>(R.id.stRemainAvgTextViewTitle).text = ""
            findViewById<TextView>(R.id.stRemainAvgTextView).text = ""
        }

        // Insert table headers.
        findViewById<TextView>(R.id.stIncomeTextView).text = Utils.formatMoney(incomeTotalSum)
        findViewById<TextView>(R.id.stExpenseTextView).text = Utils.formatMoney(expenseTotalSum)
        findViewById<TextView>(R.id.stBudgetTextView).text = Utils.formatMoney(budgetTotalSum)
        val remainTotal = incomeTotalSum + budgetTotalSum - expenseTotalSum
        findViewById<TextView>(R.id.stRemainTextView).text = Utils.formatMoney(remainTotal)
        if (dateDiff > 0) {
            findViewById<TextView>(R.id.stRemainAvgTextView).text =
                Utils.formatMoney(remainTotal / dateDiff)
        }

        // Calculate remain for all types.
        val results = Vector<StBudgetAdapter.StItem>()
        stAdapter = StBudgetAdapter(this, results)
        for (type in typeList) {
            val income = incomeSum[type] ?: 0.0
            val expense = expenseSum[type] ?: 0.0
            val budget = budgetSum[type] ?: 0.0
            val remain = income + budget - expense

            val remainAvg = if (dateDiff > 0) remain / dateDiff else 0.0
            val stItem = StBudgetAdapter.StItem(
                dateDiff > 0,
                type,
                income,
                expense,
                budget,
                remain,
                remainAvg
            )
            results.add(stItem)
        }
    }

    private fun setupViewComponent() {
        findViewById<ListView>(R.id.listView).adapter = stAdapter
    }

    private lateinit var stAdapter: StBudgetAdapter
    private var minDate = -1
    private var maxDate = -1
}
