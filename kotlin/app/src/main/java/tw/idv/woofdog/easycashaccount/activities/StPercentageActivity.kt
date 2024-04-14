package tw.idv.woofdog.easycashaccount.activities

import java.util.Vector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.adapters.StPercentageAdapter
import tw.idv.woofdog.easycashaccount.adapters.StPercentageTabAdapter
import tw.idv.woofdog.easycashaccount.db.DbTableSQLite
import tw.idv.woofdog.easycashaccount.db.DbTransaction

/**
 * The activity to display percentage analysis information.
 */
class StPercentageActivity : AppCompatActivity() {
    class StFragment(private val listAdapter: StPercentageAdapter) : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.fragment_st_percentage, container, false)

            view.findViewById<ListView>(R.id.listView).adapter = listAdapter

            return view
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.titleStPercentage)
        setContentView(R.layout.activity_st_percentage)

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

            val type = t.transType
            if (!typeList.contains(type)) {
                typeList.add(type)
                incomeSum[type] = 0.0
                expenseSum[type] = 0.0
                budgetSum[type] = 0.0
            }

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

        // Calculate remain for all types.
        val incomeResults = Vector<StPercentageAdapter.StItem>()
        incomeAdapter = StPercentageAdapter(this, incomeResults)
        val expenseResults = Vector<StPercentageAdapter.StItem>()
        expenseAdapter = StPercentageAdapter(this, expenseResults)
        val budgetResults = Vector<StPercentageAdapter.StItem>()
        budgetAdapter = StPercentageAdapter(this, budgetResults)

        val incomeList = mutableListOf<StPercentageAdapter.StItem>()
        val expenseList = mutableListOf<StPercentageAdapter.StItem>()
        val budgetList = mutableListOf<StPercentageAdapter.StItem>()
        for (type in typeList) {
            val income = incomeSum[type] ?: 0.0
            val expense = expenseSum[type] ?: 0.0
            val budget = budgetSum[type] ?: 0.0

            val incomePercentage =
                if (incomeTotalSum > 0.0) (income * 100 / incomeTotalSum) else 0.0
            incomeList.add(StPercentageAdapter.StItem(type, income, incomePercentage))

            val expensePercentage =
                if (expenseTotalSum > 0.0) (expense * 100 / expenseTotalSum) else 0.0
            expenseList.add(StPercentageAdapter.StItem(type, expense, expensePercentage))

            val budgetPercentage =
                if (budgetTotalSum > 0.0) (budget * 100 / budgetTotalSum) else 0.0
            budgetList.add(StPercentageAdapter.StItem(type, budget, budgetPercentage))
        }

        // Sort lists.
        incomeList.sortByDescending { it.sum }
        expenseList.sortByDescending { it.sum }
        budgetList.sortByDescending { it.sum }

        for (item in incomeList) {
            incomeResults.add(item)
        }
        for (item in expenseList) {
            expenseResults.add(item)
        }
        for (item in budgetList) {
            budgetResults.add(item)
        }
    }

    private fun setupViewComponent() {
        initTab()
    }

    private fun initTab() {
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = StPercentageTabAdapter(
            supportFragmentManager,
            lifecycle,
            incomeAdapter,
            expenseAdapter,
            budgetAdapter
        )
        TabLayoutMediator(tabLayout, viewPager, true) { tab, position ->
            tab.text = when (position) {
                1 -> getString(R.string.transIncome)
                2 -> getString(R.string.transBudget)
                else -> getString(R.string.transExpense)
            }
            viewPager.currentItem = tab.position
        }.attach()
    }

    private lateinit var incomeAdapter: StPercentageAdapter
    private lateinit var expenseAdapter: StPercentageAdapter
    private lateinit var budgetAdapter: StPercentageAdapter
    private var minDate = -1
    private var maxDate = -1
}
