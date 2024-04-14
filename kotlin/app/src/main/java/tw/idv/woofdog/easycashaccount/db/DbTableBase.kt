package tw.idv.woofdog.easycashaccount.db

import java.util.Vector

import tw.idv.woofdog.easycashaccount.utils.Utils
import java.util.Calendar

/**
 * The base class of all database.
 *
 * With this class, the program can manage databases without knowing detail implementation.
 *   - The getter function returns `null` means that the get operation has errors.
 *   - The setter function returns `false` means that the set operation has errors.
 *
 * The READ ONLY attribute is used for supporting old database formats.
 *
 * Please refer to `doc/schema.md` to get detail information of database tables.
 */
interface DbTableBase {
    data class QueryCond(
        var fromNo: Long? = null,
        var toNo: Long? = null,
        var fromDate: Int? = null,
        var toDate: Int? = null,
        var type: String? = null,
        var inExp: DbTransaction.InExp? = null,
        var fromMoney: Double? = null,
        var toMoney: Double? = null,
        var description: String? = null,
        var location: String? = null
    )

    companion object {
        fun getDateFromYMD(year: Int, month: Int, day: Int): Int {
            return year * 10000 + month * 100 + day
        }
        fun getYearFromDate(date: Int): Int {
            return date / 10000
        }
        fun getMonthFromDate(date: Int): Int {
            return date / 100 % 100
        }
        fun getDayFromDate(date: Int): Int {
            return date % 100
        }
        fun getCalendarFromDate(date: Int): Calendar {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, getYearFromDate(date))
            calendar.set(Calendar.MONTH, getMonthFromDate(date) - 1)
            calendar.set(Calendar.DATE, getDayFromDate(date))
            return calendar
        }
        fun getDateFromCalendar(calendar: Calendar): Int {
            return getDateFromYMD(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DATE))
        }
    }

    // Get functions.
    fun getFileName(): String { return dbFileName }
    fun getDescription(): String?
    fun getLastModified(): Long?

    // Set functions.
    fun setFileName(name: String): Boolean
    fun setDescription(description: String): Boolean
    fun setLastModified(lastTime: Long = Utils.getCurrentTimeEpoch()): Boolean

    // Transaction type functions.
    fun getTransTypeNumber(): Int?
    fun getTransTypes(): Vector<String>?
    fun addTransType(type: String): Boolean
    fun deleteTransType(type: String): Boolean
    fun modifyTransType(fromType: String, toType: String, merge: Boolean): Boolean

    // Transaction operation functions.
    fun getTransactionNumber(): Int?
    fun getTransactions(): Vector<DbTransaction>?
    fun getTransaction(transNo: Long): DbTransaction?
    fun addTransaction(trans: DbTransaction): Boolean
    fun deleteTransaction(transNo: Long): Boolean
    fun modifyTransaction(transNo: Long, trans: DbTransaction): Boolean

    // Query functions.
    fun queryAND(cond: QueryCond): Vector<DbTransaction>?
    fun queryOR(cond: QueryCond): Vector<DbTransaction>?

    var isReadOnly: Boolean
    var dbFileName: String
}
