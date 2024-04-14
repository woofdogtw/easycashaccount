package tw.idv.woofdog.easycashaccount.activities

import java.util.Vector

import android.os.Bundle
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.adapters.StGrandTotalAdapter
import tw.idv.woofdog.easycashaccount.db.DbTableSQLite
import tw.idv.woofdog.easycashaccount.db.DbTransaction
import tw.idv.woofdog.easycashaccount.utils.Utils

/**
 * The activity to display grand total analysis information.
 */
class StGrandTotalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.titleStGrandTotal)
        setContentView(R.layout.activity_st_grand_total)

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

        // Values for list view contents.
        var incomeSum = 0.0
        var expenseSum = 0.0
        var budgetSum = 0.0

        // Calculate income, expense, budget for all types.
        for (t in trans) {
            if (isRangeSet && (t.transDate < minDate || t.transDate > maxDate)) {
                continue
            }

            val money = t.transMoney
            when (t.transInExp) {
                DbTransaction.InExp.INCOME -> {
                    incomeSum += money
                }

                DbTransaction.InExp.EXPENSE -> {
                    expenseSum += money
                }

                DbTransaction.InExp.BUDGET -> {
                    budgetSum += money
                }
            }
        }

        // Insert table headers.
        val sum = incomeSum + budgetSum - expenseSum
        findViewById<TextView>(R.id.stSumTextView).text = Utils.formatMoney(sum)

        // Calculate remain for all types.
        val results = Vector<StGrandTotalAdapter.StItem>()
        stAdapter = StGrandTotalAdapter(this, results)

        results.add(StGrandTotalAdapter.StItem(getString(R.string.transIncome), incomeSum))
        results.add(StGrandTotalAdapter.StItem(getString(R.string.transExpense), expenseSum))
        results.add(StGrandTotalAdapter.StItem(getString(R.string.transBudget), budgetSum))
    }

    private fun setupViewComponent() {
        findViewById<ListView>(R.id.listView).adapter = stAdapter
    }

    private lateinit var stAdapter: StGrandTotalAdapter
    private var minDate = -1
    private var maxDate = -1
}
