package tw.idv.woofdog.easycashaccount.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

import tw.idv.woofdog.easycashaccount.R
import tw.idv.woofdog.easycashaccount.adapters.DbContentAdapter
import tw.idv.woofdog.easycashaccount.adapters.TransListAdapter
import tw.idv.woofdog.easycashaccount.adapters.TypeListAdapter
import tw.idv.woofdog.easycashaccount.db.DbTableBase
import tw.idv.woofdog.easycashaccount.db.DbTableMemory
import tw.idv.woofdog.easycashaccount.db.DbTableSQLite
import tw.idv.woofdog.easycashaccount.dialogs.AboutDialog
import tw.idv.woofdog.easycashaccount.dialogs.DbTypeDialog
import tw.idv.woofdog.easycashaccount.dialogs.FindDialog
import tw.idv.woofdog.easycashaccount.dialogs.OptionsSyncDialog
import tw.idv.woofdog.easycashaccount.dialogs.StRangeDialog
import tw.idv.woofdog.easycashaccount.dialogs.SyncFtpDialog
import tw.idv.woofdog.easycashaccount.dialogs.SyncMsDialog
import tw.idv.woofdog.easycashaccount.dialogs.SyncType
import tw.idv.woofdog.easycashaccount.dialogs.TransactionDialog
import tw.idv.woofdog.easycashaccount.dialogs.WarningDialog
import tw.idv.woofdog.easycashaccount.dialogs.readOptions
import tw.idv.woofdog.easycashaccount.utils.Utils

class DbContentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_db_content)
        supportActionBar?.hide()

        dbDir = "${applicationInfo.dataDir}/databases"
        dbName = intent.getStringExtra("dbName") ?: ""
        if (dbName == "") {
            dbTable = DbTableMemory()
            dbTable.isReadOnly = true
        } else {
            dbTable = DbTableSQLite()
        }
        dbTable.setFileName("$dbDir/$dbName")
        transListAdapter = TransListAdapter(this, dbTable)
        dbTypeListAdapter = TypeListAdapter(this, dbTable)
        drawerLayout = findViewById(R.id.drawerLayout)
        progressBar = findViewById(R.id.progressBar)

        initActivityNavigation()

        setupViewComponent()

        progressBar.visibility = View.INVISIBLE
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val adapter = viewPager.adapter as DbContentAdapter
        when (tabLayout.selectedTabPosition) {
            1 -> {
                adapter.typeFragment.doContextItemSelected(item)
            }

            else -> {
                adapter.transFragment.doContextItemSelected(item)
            }
        }
        return super.onContextItemSelected(item)
    }

    /**
     * To update adapters because database file is synchronized from network.
     */
    fun syncUpdate() {
        progressBar.visibility = View.VISIBLE
        transListAdapter.update()
        dbTypeListAdapter.update()
        updateTitle()
        (viewPager.adapter as DbContentAdapter).transFragment.scrollToLatest()
        progressBar.visibility = View.INVISIBLE
        dbFileSync = true
    }

    private fun setupViewComponent() {
        initTab()
        initTopAppBar()
        initBottomAppBar()
        initLeftDrawer()
    }

    private fun initActivityNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                dbTable.setFileName("")
                val bundle = Bundle()
                bundle.putBoolean("dbFileSync", dbFileSync)
                val data = Intent()
                data.putExtras(bundle)
                setResult(0, data)
                finish()
            }
        } else {
            onBackPressedDispatcher.addCallback(this) {
                dbTable.setFileName("")
                val bundle = Bundle()
                bundle.putBoolean("dbFileSync", dbFileSync)
                val data = Intent()
                data.putExtras(bundle)
                setResult(0, data)
                finish()
            }
        }

        syncLoginLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val type = result.data?.extras?.getString("type")
                if (type == "ms") {
                    val code = result.data?.extras?.getString("code")
                    SyncMsDialog(this, dbTable, code!!, syncLoginLauncher, dbDir)
                } else {
                    dbTable.setFileName(dbName)
                }
            }
    }

    private fun initTab() {
        tabLayout = findViewById(R.id.tabLayout)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(p0: TabLayout.Tab?) {
                p0?.let {
                    when (it.position) {
                        1 -> {
                            findViewById<MaterialToolbar>(R.id.transAppBar).visibility =
                                View.INVISIBLE
                            findViewById<MaterialToolbar>(R.id.typeAppBar).visibility = View.VISIBLE
                        }

                        else -> {
                            findViewById<MaterialToolbar>(R.id.transAppBar).visibility =
                                View.VISIBLE
                            findViewById<MaterialToolbar>(R.id.typeAppBar).visibility =
                                View.INVISIBLE
                        }
                    }
                }
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {}

            override fun onTabReselected(p0: TabLayout.Tab?) {}
        })
        findViewById<MaterialToolbar>(R.id.transAppBar).visibility = View.VISIBLE
        findViewById<MaterialToolbar>(R.id.typeAppBar).visibility = View.INVISIBLE

        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = DbContentAdapter(
            supportFragmentManager,
            lifecycle,
            this,
            dbTypeListAdapter,
            transListAdapter
        )
        TabLayoutMediator(tabLayout, viewPager, true) { tab, position ->
            tab.text = when (position) {
                1 -> getString(R.string.tabTypeTitle)
                else -> getString(R.string.tabTransTitle)
            }
            viewPager.currentItem = tab.position
        }.attach()
    }

    private fun initTopAppBar() {
        topAppBar = findViewById(R.id.topAppBar)
        updateTitle()
        topAppBar.setNavigationOnClickListener {
            if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
    }

    private fun initBottomAppBar() {
        val transAppBar: MaterialToolbar = findViewById(R.id.transAppBar)
        val typeAppBar: MaterialToolbar = findViewById(R.id.typeAppBar)

        transAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.transAddButton -> {
                    if (dbTable.getTransTypeNumber() == 0) {
                        WarningDialog(this, 0, getString(R.string.dTransWarnNoType)).show()
                        return@setOnMenuItemClickListener true
                    }
                    TransactionDialog(
                        this,
                        TransactionDialog.Type.CREATE,
                        transListAdapter,
                        0
                    )
                    true
                }

                R.id.transFindButton -> {
                    FindDialog(this, dbTable)
                    true
                }

                R.id.transSyncButton -> {
                    doSync()
                    true
                }

                R.id.transStBudgetButton -> {
                    StRangeDialog(this, StRangeDialog.Type.BUDGET, dbTable)
                    true
                }

                R.id.transStGrandTotalButton -> {
                    StRangeDialog(this, StRangeDialog.Type.GRANT_TOTAL, dbTable)
                    true
                }

                R.id.transStPercentButton -> {
                    StRangeDialog(this, StRangeDialog.Type.PERCENTAGE, dbTable)
                    true
                }

                else -> true
            }
        }

        typeAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.typeAddButton -> {
                    DbTypeDialog(
                        this,
                        DbTypeDialog.Type.CREATE,
                        transListAdapter,
                        dbTypeListAdapter,
                        ""
                    )
                    true
                }

                else -> true
            }
        }
    }

    private fun initLeftDrawer() {
        val navigationView: NavigationView = findViewById(R.id.leftDrawer)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.mOptSync -> OptionsSyncDialog(this)
                R.id.mAbout -> AboutDialog(this)
            }
            true
        }
    }

    private fun updateTitle() {
        topAppBar.title =
            (dbTable.getDescription() ?: "").ifEmpty { getString(R.string.titleUntitled) }
    }

    private fun doSync() {
        if (dbName.isEmpty()) {
            return
        }

        // Set file name to empty to force commit DB operations.
        val dbTableFileName = dbTable.getFileName()
        dbTable.setFileName("")
        dbTable.setFileName(dbTableFileName)

        val sp = Utils.getSharedPreferences(this)
        val options = readOptions(sp)
        when (options.syncType) {
            SyncType.FTP, SyncType.FTPS -> {
                SyncFtpDialog(this, dbTable, dbDir)
                return
            }

            SyncType.MS -> {
                SyncMsDialog(this, dbTable, "", syncLoginLauncher, dbDir)
                return
            }

            else -> {
                WarningDialog(this, 0, getString(R.string.dSyncWarnOpt)) { dialog, _ ->
                    dialog.dismiss()
                }.show()
            }
        }
    }

    private lateinit var dbDir: String
    private lateinit var transListAdapter: TransListAdapter
    private lateinit var dbTypeListAdapter: TypeListAdapter
    private lateinit var topAppBar: MaterialToolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var syncLoginLauncher: ActivityResultLauncher<Intent>
    private var dbName = ""
    private lateinit var dbTable: DbTableBase
    private var dbFileSync = false
}
