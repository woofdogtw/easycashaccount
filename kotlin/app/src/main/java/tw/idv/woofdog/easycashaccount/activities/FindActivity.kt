package tw.idv.woofdog.easycashaccount.activities

import android.os.Build
import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.adapters.TransListAdapter
import tw.idv.woofdog.easycashaccount.db.DbTableMemory
import tw.idv.woofdog.easycashaccount.db.DbTransaction

class FindActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.titleFind)
        setContentView(R.layout.activity_find)

        setupViewComponent()
    }

    private fun setupViewComponent() {
        // Get find results from the intent object.
        val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("results", ArrayList::class.java) as ArrayList<DbTransaction>
        } else {
            intent.getSerializableExtra("results") as ArrayList<DbTransaction>
        }
        dbTable.setDescription("find")
        for (trans in results) {
            dbTable.addTransType(trans.transType)
            dbTable.addTransaction(trans)
        }

        // Set data to the adapter.
        val adapter = TransListAdapter(this, dbTable)
        findViewById<ListView>(R.id.listView).adapter = adapter
        adapter.update()
    }

    private val dbTable = DbTableMemory()
}
