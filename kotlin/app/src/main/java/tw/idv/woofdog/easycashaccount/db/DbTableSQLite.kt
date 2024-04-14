package tw.idv.woofdog.easycashaccount.db

import java.util.Vector

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.util.Log

/**
 * The SQLite implementation of DbTableBase.
 *
 * This class implements complete database operations for this program, and uses SQLite for updating
 * and querying data.
 */
class DbTableSQLite private constructor() : DbTableBase {
    enum class AvailableType {
        /** Invalid database format. */
        INVALID,

        /** Old formats. */
        READ_ONLY,

        /** The current database format. */
        READ_WRITE
    }

    constructor(dbVer: Int = DB_TOP_VERSION) : this() {
        isReadOnly = dbVer < DB_TOP_VERSION
        dbVersion = dbVer
    }

    companion object {
        private const val DB_NAME = "Easy Cash Account"
        private const val DB_VERSION_V1 = 1
        private const val DB_VERSION_V2 = 2
        private const val DB_TOP_VERSION = DB_VERSION_V2

        private const val COL_IDX_NAME = 0
        private const val COL_IDX_VERSION = 1
        private const val COL_IDX_DESCRIPTION = 2
        private const val COL_IDX_LAST_MODIFY = 3

        private const val COL_IDX_TRANS_NO = 0
        private const val COL_IDX_TRANS_DATE = 1
        private const val COL_IDX_TRANS_TYPE = 2
        private const val COL_IDX_TRANS_INEXP = 3
        private const val COL_IDX_TRANS_MONEY = 4
        private const val COL_IDX_TRANS_DESCRIPTION = 5
        private const val COL_IDX_TRANS_LOCATION = 6

        /**
         * To check the database format.
         */
        fun isAvailableDatabase(dbPath: String, dbVer: Int = DB_TOP_VERSION): AvailableType {
            val logTag = "DbTableSQLite.isAvailableDatabase()"

            val db: SQLiteDatabase
            try {
                db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "")
                return AvailableType.INVALID
            }

            val validType = innerIsAvailableDatabase(db, dbVer)
            try {
                db.close()
            } catch (e: Exception) {
                Log.e(logTag, e.message ?: "close error")
            }
            return validType
        }

        private fun innerIsAvailableDatabase(db: SQLiteDatabase, dbVer: Int): AvailableType {
            val logTag = "DbTableSQLite.innerIsAvailableDatabase()"

            if (!db.isOpen) {
                return AvailableType.INVALID
            }

            var availableType = AvailableType.INVALID
            var cursor: Cursor? = null
            do {
                try {
                    cursor = db.rawQuery("SELECT * FROM db_info", arrayOf())
                    if (cursor == null || !cursor.moveToNext()) {
                        break
                    }
                    val name = cursor.getString(COL_IDX_NAME)
                    val version = cursor.getInt(COL_IDX_VERSION)
                    availableType = if (
                        name != DB_NAME || dbVer < DB_VERSION_V1 || version > dbVer
                    ) {
                        break
                    } else if (version != dbVer) {
                        AvailableType.READ_ONLY
                    } else {
                        AvailableType.READ_WRITE
                    }
                } catch (e: SQLiteException) {
                    Log.e(logTag, e.message ?: "query error")
                }
            } while (false)

            cursor?.close()
            return availableType
        }
    }

    override fun getDescription(): String? {
        val logTag = "DbTableSQLite.getDescription()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var description: String? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor = dbConn?.rawQuery("SELECT descript FROM db_info", arrayOf()) ?: break
                description = if (cursor.moveToNext()) cursor.getString(0) else null
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return description
    }

    override fun getLastModified(): Long? {
        val logTag = "DbTableSQLite.getLastModified()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var lastModified: Long? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT last_modify FROM db_info", arrayOf()) ?: break
                lastModified = if (cursor.moveToNext()) cursor.getLong(0) else null
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return lastModified
    }

    override fun setFileName(name: String): Boolean {
        val logTag = "DbTableSQLite.setFileName()"

        dbFileName = name

        if (dbConn != null && dbConn?.isOpen!!) {
            dbConn?.close()
        }
        dbConn = null
        if (name == "") {
            return true
        }

        try {
            dbConn = SQLiteDatabase.openOrCreateDatabase(dbFileName, null)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "create error")
        }
        if (!createDatabase()) {
            return false
        }

        isReadOnly = true
        var success = false
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT version FROM db_info", arrayOf()) ?: break
                dbVersion = if (cursor.moveToNext()) cursor.getInt(0) else break
                isReadOnly = dbVersion != DB_TOP_VERSION
                success = true
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query version error")
            }
        } while (false)

        cursor?.close()
        return success
    }

    override fun setDescription(description: String): Boolean {
        val logTag = "DbTableSQLite.setDescription()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        try {
            dbConn?.execSQL("UPDATE db_info SET descript=?", arrayOf(description))
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "db_info error")
            return false
        }
        setLastModified()
        return true
    }

    override fun setLastModified(lastTime: Long): Boolean {
        val logTag = "DbTableSQLite.setLastModified()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        try {
            dbConn?.execSQL("UPDATE db_info SET last_modify=?", arrayOf("$lastTime"))
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "update error")
            return false
        }
        return true
    }

    override fun getTransTypeNumber(): Int? {
        val logTag = "DbTableSQLite.getTransTypeNumber()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var count: Int? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT COUNT(*) FROM db_trans_type", arrayOf()) ?: break
                count = if (cursor.moveToNext()) cursor.getInt(0) else break
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return count
    }

    override fun getTransTypes(): Vector<String>? {
        val logTag = "DbTableSQLite.getTransTypes()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var typeList: Vector<String>? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT * FROM db_trans_type ORDER BY type_name", arrayOf())
                        ?: break
                typeList = Vector<String>()
                while (cursor.moveToNext()) {
                    typeList.add(cursor.getString(0))
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return typeList
    }

    override fun addTransType(type: String): Boolean {
        val logTag = "DbTableSQLite.addTransType()"

        if (dbConn == null || !dbConn?.isOpen!! || type == "" || isReadOnly) {
            return false
        }

        var returnValue: Boolean? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor = dbConn?.rawQuery(QUERY_TRANS_TYPE, arrayOf(type)) ?: return false
                if (cursor.moveToNext()) {
                    returnValue = true
                    break
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
                returnValue = false
            }
        } while (false)
        cursor?.close()
        if (returnValue != null) {
            return returnValue
        }

        try {
            dbConn?.execSQL("INSERT INTO db_trans_type (type_name) VALUES (?)", arrayOf(type))
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "insert error")
            return false
        }
        setLastModified()
        return true
    }

    override fun deleteTransType(type: String): Boolean {
        val logTag = "DbTableSQLite.deleteTransType()"

        if (dbConn == null || !dbConn?.isOpen!! || type == "" || isReadOnly) {
            return false
        }

        var returnValue: Boolean? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor = dbConn?.rawQuery(QUERY_TRANS_TYPE, arrayOf(type)) ?: return false
                if (!cursor.moveToNext()) {
                    returnValue = true
                    break
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query type error")
                returnValue = false
            }
        } while (false)
        cursor?.close()
        if (returnValue != null) {
            return returnValue
        }

        returnValue = null
        cursor = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT * FROM db_transaction WHERE type=?", arrayOf(type))
                        ?: return false
                if (cursor.moveToNext()) {
                    returnValue = false
                    break
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query transaction error")
                returnValue = false
            }
        } while (false)
        cursor?.close()
        if (returnValue != null) {
            return returnValue
        }

        try {
            dbConn?.execSQL("DELETE FROM db_trans_type WHERE type_name=?", arrayOf(type))
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "insert error")
            return false
        }
        setLastModified()
        return true
    }

    override fun modifyTransType(fromType: String, toType: String, merge: Boolean): Boolean {
        val logTag = "DbTableSQLite.modifyTransType()"

        if (dbConn == null || !dbConn?.isOpen!! || fromType == "" || toType == "" || isReadOnly) {
            return false
        } else if (fromType == toType) {
            return true
        }

        var returnValue: Boolean? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor = dbConn?.rawQuery(QUERY_TRANS_TYPE, arrayOf(fromType)) ?: return false
                if (!cursor.moveToNext()) {
                    returnValue = true
                    break
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query from type error")
                returnValue = false
            }
        } while (false)
        cursor?.close()
        if (returnValue != null) {
            return returnValue
        }

        var deleteFromType = false
        returnValue = null
        cursor = null
        do {
            try {
                cursor = dbConn?.rawQuery(QUERY_TRANS_TYPE, arrayOf(toType)) ?: return false
                if (cursor.moveToNext()) {
                    if (!merge) {
                        returnValue = false
                        break
                    }
                    deleteFromType = true
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query to type then modify type error")
                returnValue = false
            }
        } while (false)
        cursor?.close()
        if (returnValue != null) {
            return returnValue
        }

        try {
            if (deleteFromType) {
                dbConn?.execSQL("DELETE FROM db_trans_type WHERE type_name=?", arrayOf(fromType))
            } else {
                dbConn?.execSQL(
                    "UPDATE db_trans_type SET type_name=? WHERE type_name=?",
                    arrayOf(toType, fromType)
                )
            }
            dbConn?.execSQL(
                "UPDATE db_transaction SET type=? WHERE type=?",
                arrayOf(toType, fromType)
            )
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "modify type error")
            return false
        }
        setLastModified()
        return true
    }

    override fun getTransactionNumber(): Int? {
        val logTag = "DbTableSQLite.getTransactionNumber()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var count: Int? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT COUNT(*) FROM db_transaction", arrayOf()) ?: break
                count = if (cursor.moveToNext()) cursor.getInt(0) else break
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return count
    }

    override fun getTransactions(): Vector<DbTransaction>? {
        val logTag = "DbTableSQLite.getTransactions()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var transList: Vector<DbTransaction>? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery("SELECT * FROM db_transaction ORDER BY date,no", arrayOf())
                        ?: break
                transList = Vector<DbTransaction>()
                while (cursor.moveToNext()) {
                    val t = DbTransaction(cursor.getLong(COL_IDX_TRANS_NO))
                    t.transDate = cursor.getInt(COL_IDX_TRANS_DATE)
                    t.transType = cursor.getString(COL_IDX_TRANS_TYPE)
                    t.transInExp = when (cursor.getInt(COL_IDX_TRANS_INEXP)) {
                        DbTransaction.InExp.INCOME.v -> DbTransaction.InExp.INCOME
                        DbTransaction.InExp.EXPENSE.v -> DbTransaction.InExp.EXPENSE
                        DbTransaction.InExp.BUDGET.v -> DbTransaction.InExp.BUDGET
                        else -> continue
                    }
                    t.transMoney = cursor.getDouble(COL_IDX_TRANS_MONEY)
                    t.transDescription = cursor.getString(COL_IDX_TRANS_DESCRIPTION)?:""
                    t.transLocation = cursor.getString(COL_IDX_TRANS_LOCATION)?:""

                    transList.add(t)
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return transList
    }

    override fun getTransaction(transNo: Long): DbTransaction? {
        val logTag = "DbTableSQLite.getTransaction()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var trans: DbTransaction? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor = dbConn?.rawQuery(QUERY_TRANSACTION, arrayOf("$transNo")) ?: break
                if (!cursor.moveToNext()) {
                    break
                }
                trans = DbTransaction(cursor.getLong(COL_IDX_TRANS_NO))
                trans.transDate = cursor.getInt(COL_IDX_TRANS_DATE)
                trans.transType = cursor.getString(COL_IDX_TRANS_TYPE)
                trans.transInExp = when (cursor.getInt(COL_IDX_TRANS_INEXP)) {
                    DbTransaction.InExp.INCOME.v -> DbTransaction.InExp.INCOME
                    DbTransaction.InExp.EXPENSE.v -> DbTransaction.InExp.EXPENSE
                    DbTransaction.InExp.BUDGET.v -> DbTransaction.InExp.BUDGET
                    else -> break
                }
                trans.transMoney = cursor.getDouble(COL_IDX_TRANS_MONEY)
                trans.transDescription = cursor.getString(COL_IDX_TRANS_DESCRIPTION)
                trans.transLocation = cursor.getString(COL_IDX_TRANS_LOCATION)
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return trans
    }

    override fun addTransaction(trans: DbTransaction): Boolean {
        val logTag = "DbTableSQLite.addTransaction()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        var returnValue: Boolean? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor =
                    dbConn?.rawQuery(QUERY_TRANSACTION, arrayOf("${trans.transNo}")) ?: return false
                if (cursor.moveToNext()) {
                    returnValue = false
                    break
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query transaction error")
                returnValue = false
            }
        } while (false)
        cursor?.close()
        if (returnValue != null) {
            return returnValue
        }

        try {
            val sql = "INSERT INTO db_transaction " +
                    "(no, date, type, in_out, money, descript, location) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)"
            val values = arrayOf(
                "${trans.transNo}",
                "${trans.transDate}",
                trans.transType,
                "${trans.transInExp.v}",
                "${trans.transMoney}",
                trans.transDescription,
                trans.transLocation
            )
            dbConn?.execSQL(sql, values)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "insert error")
            return false
        }
        setLastModified()
        return true
    }

    override fun deleteTransaction(transNo: Long): Boolean {
        val logTag = "DbTableSQLite.deleteTransaction()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        try {
            dbConn?.execSQL("DELETE FROM db_transaction WHERE no=?", arrayOf(transNo))
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "delete error")
            return false
        }
        setLastModified()
        return true
    }

    override fun modifyTransaction(transNo: Long, trans: DbTransaction): Boolean {
        val logTag = "DbTableSQLite.modifyTransaction()"

        if (dbConn == null || !dbConn?.isOpen!! || isReadOnly) {
            return false
        }

        var returnValue: Boolean? = null
        var cursor: Cursor? = null
        do {
            try {
                cursor = dbConn?.rawQuery(QUERY_TRANSACTION, arrayOf("$transNo")) ?: return false
                if (!cursor.moveToNext()) {
                    returnValue = false
                    break
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
                returnValue = false
            }
        } while (false)
        cursor?.close()
        if (returnValue != null) {
            return returnValue
        }

        try {
            val sql = "UPDATE db_transaction " +
                    "SET date=?, type=?, in_out=?, money=?, descript=?, location=?" +
                    "WHERE no=?"
            val values = arrayOf(
                "${trans.transDate}",
                trans.transType,
                "${trans.transInExp.v}",
                "${trans.transMoney}",
                trans.transDescription,
                trans.transLocation,
                "${trans.transNo}"
            )
            dbConn?.execSQL(sql, values)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "update error")
            return false
        }
        setLastModified()
        return true
    }

    override fun queryAND(cond: DbTableBase.QueryCond): Vector<DbTransaction>? {
        val logTag = "DbTableSQLite.queryAND()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var transList: Vector<DbTransaction>? = null
        var cursor: Cursor? = null
        do {
            try {
                var queryParams = queryString(cond, true)
                val queryStr = queryParams[0]
                queryParams = queryParams.sliceArray(1..<queryParams.size)
                cursor = dbConn?.rawQuery(
                    "SELECT * FROM db_transaction WHERE $queryStr ORDER BY date,no",
                    queryParams
                ) ?: break
                transList = Vector<DbTransaction>()
                while (cursor.moveToNext()) {
                    val t = DbTransaction(cursor.getLong(COL_IDX_TRANS_NO))
                    t.transDate = cursor.getInt(COL_IDX_TRANS_DATE)
                    t.transType = cursor.getString(COL_IDX_TRANS_TYPE)
                    t.transInExp = when (cursor.getInt(COL_IDX_TRANS_INEXP)) {
                        DbTransaction.InExp.INCOME.v -> DbTransaction.InExp.INCOME
                        DbTransaction.InExp.EXPENSE.v -> DbTransaction.InExp.EXPENSE
                        DbTransaction.InExp.BUDGET.v -> DbTransaction.InExp.BUDGET
                        else -> continue
                    }
                    t.transMoney = cursor.getDouble(COL_IDX_TRANS_MONEY)
                    t.transDescription = cursor.getString(COL_IDX_TRANS_DESCRIPTION)
                    t.transLocation = cursor.getString(COL_IDX_TRANS_LOCATION)

                    transList.add(t)
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return transList
    }

    override fun queryOR(cond: DbTableBase.QueryCond): Vector<DbTransaction>? {
        val logTag = "DbTableSQLite.queryAND()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return null
        }

        var transList: Vector<DbTransaction>? = null
        var cursor: Cursor? = null
        do {
            try {
                var queryParams = queryString(cond, false)
                val queryStr = queryParams[0]
                queryParams = queryParams.sliceArray(1..<queryParams.size)
                cursor = dbConn?.rawQuery(
                    "SELECT * FROM db_transaction WHERE $queryStr ORDER BY date,no",
                    queryParams
                ) ?: break
                transList = Vector<DbTransaction>()
                while (cursor.moveToNext()) {
                    val t = DbTransaction(cursor.getLong(COL_IDX_TRANS_NO))
                    t.transDate = cursor.getInt(COL_IDX_TRANS_DATE)
                    t.transType = cursor.getString(COL_IDX_TRANS_TYPE)
                    t.transInExp = when (cursor.getInt(COL_IDX_TRANS_INEXP)) {
                        DbTransaction.InExp.INCOME.v -> DbTransaction.InExp.INCOME
                        DbTransaction.InExp.EXPENSE.v -> DbTransaction.InExp.EXPENSE
                        DbTransaction.InExp.BUDGET.v -> DbTransaction.InExp.BUDGET
                        else -> continue
                    }
                    t.transMoney = cursor.getDouble(COL_IDX_TRANS_MONEY)
                    t.transDescription = cursor.getString(COL_IDX_TRANS_DESCRIPTION)
                    t.transLocation = cursor.getString(COL_IDX_TRANS_LOCATION)

                    transList.add(t)
                }
            } catch (e: SQLiteException) {
                Log.e(logTag, e.message ?: "query error")
            }
        } while (false)

        cursor?.close()
        return transList
    }

    private fun createDatabase(): Boolean {
        val logTag = "DbTableSQLite.createDatabase()"

        if (dbConn == null || !dbConn?.isOpen!!) {
            return false
        }

        try {
            dbConn?.execSQL(CREATE_DB_INFO)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "db_info error")
            return false
        }
        try {
            dbConn?.execSQL(CREATE_DB_INFO_IDX)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "db_info index error")
            return false
        }
        try {
            dbConn?.execSQL(
                "INSERT INTO db_info (name, version, last_modify) VALUES (?, ?, ?)",
                arrayOf(DB_NAME, "$DB_TOP_VERSION", "0")
            )
        } catch (e: SQLiteException) {
            Log.w(logTag, e.message ?: "insert db_info error")
        }
        try {
            dbConn?.execSQL(CREATE_DB_TRANS_TYPE)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "db_trans_type error")
            return false
        }
        try {
            dbConn?.execSQL(CREATE_DB_TRANSACTION)
        } catch (e: SQLiteException) {
            Log.e(logTag, e.message ?: "db_transaction error")
            return false
        }
        return true
    }

    private fun queryString(cond: DbTableBase.QueryCond, isAND: Boolean): Array<String> {
        var queryStr = ""
        val queryParams = ArrayList<String>()

        val (
            fromNo, toNo, fromDate, toDate, type, inExp, fromMoney, toMoney, description, location
        ) = cond
        var condCount = 0

        if (fromNo != null) {
            queryStr += if (toNo != null) {
                "(no >= $fromNo AND no <= $toNo)"
            } else {
                "(no >= $fromNo)"
            }
            condCount++
        }
        if (toNo != null) {
            if (condCount > 0) {
                queryStr += if (isAND) " AND " else " OR "
            }
            condCount++
            queryStr += "(no <= $toNo)"
        }
        if (fromDate != null) {
            if (condCount > 0) {
                queryStr += if (isAND) " AND " else " OR "
            }
            condCount++
            queryStr += if (toDate != null) {
                "(date >= $fromDate AND date <= $toDate)"
            } else {
                "(date >= $fromDate)"
            }
        }
        if (toDate != null) {
            if (condCount > 0) {
                queryStr += if (isAND) " AND " else " OR "
            }
            condCount++
            queryStr += "(date <= $toDate)"
        }
        if (type != null) {
            if (condCount > 0) {
                queryStr += if (isAND) " AND " else " OR "
            }
            condCount++
            queryStr += "(UPPER(type) LIKE UPPER(?))"
            queryParams.add("%" + type.replace("'", "\'") + "%")
        }
        if (inExp != null) {
            if (condCount > 0) {
                queryStr += if (isAND) " AND " else " OR "
            }
            condCount++
            queryStr += "(in_out=$inExp)"
        }
        if (fromMoney != null) {
            if (condCount > 0) {
                queryStr += if (isAND) " AND " else " OR "
            }
            condCount++
            queryStr += if (toMoney != null) {
                "(money >= $fromMoney AND money <= $toMoney)"
            } else {
                "(money >= $fromMoney)"
            }
        }
        if (toMoney != null) {
            if (condCount > 0) {
                queryStr += if (isAND) " AND " else " OR "
            }
            condCount++
            queryStr += "(money <= $toMoney)"
        }
        if (description != null) {
            if (condCount > 0) {
                queryStr += if (isAND) " AND " else " OR "
            }
            condCount++
            queryStr += "(UPPER(descript) LIKE UPPER(?))"
            queryParams.add("%" + description.replace("'", "\'") + "%")
        }
        if (location != null) {
            if (condCount > 0) {
                queryStr += if (isAND) " AND " else " OR "
            }
            condCount++
            queryStr += "(UPPER(location) LIKE UPPER(?))"
            queryParams.add("%" + location.replace("'", "\'") + "%")
        }

        if (condCount > 1) {
            queryStr = "($queryStr)"
        }
        queryParams.add(0, queryStr)
        return queryParams.toTypedArray()
    }

    override var isReadOnly = true
    override var dbFileName = ""
    private var dbVersion = 0
    private var dbConn: SQLiteDatabase? = null
}

private const val CREATE_DB_INFO: String = "CREATE TABLE IF NOT EXISTS db_info (" +
        "'name' TEXT NOT NULL," +
        "'version' INTEGER NOT NULL," +
        "'descript' TEXT," +
        "'last_modify' INTEGER NOT NULL)"
private const val CREATE_DB_INFO_IDX: String =
    "CREATE UNIQUE INDEX IF NOT EXISTS db_info_idx_name ON db_info (name)"
private const val CREATE_DB_TRANS_TYPE: String =
    "CREATE TABLE IF NOT EXISTS db_trans_type ('type_name' TEXT NOT NULL UNIQUE)"
private const val CREATE_DB_TRANSACTION: String = "CREATE TABLE IF NOT EXISTS db_transaction (" +
        "'no' INTEGER NOT NULL UNIQUE," +
        "'date' INTEGER NOT NULL," +
        "'type' TEXT NOT NULL," +
        "'in_out' INTEGER NOT NULL," +
        "'money' REAL NOT NULL," +
        "'descript' TEXT," +
        "'location' TEXT," +
        "PRIMARY KEY('no'))"
private const val QUERY_TRANS_TYPE: String = "SELECT * FROM db_trans_type WHERE type_name=?"
private const val QUERY_TRANSACTION: String = "SELECT * FROM db_transaction WHERE no=?"
