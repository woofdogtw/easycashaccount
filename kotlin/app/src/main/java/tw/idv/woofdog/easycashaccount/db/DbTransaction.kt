package tw.idv.woofdog.easycashaccount.db

import java.io.Serializable

import tw.idv.woofdog.easycashaccount.utils.Utils

/**
 * The class to manage all data and operations for transactions.
 *
 * Here are special fields for transactions:
 *  - Transaction No:
 *      This is the unique ID to identify transactions. We use the creation epoch time (from 1970
 *      Jan 1st 0:00:00 UTC in seconds) because no human can create two transactions in one second.
 *  - Date:
 *      This is the decimal number to indicate the date. One can divides this value by 10000 to get
 *      the year A.D., divides 100 and modulo 100 to get the month, modulo 100 to get the day.
 *
 * A valid transaction must have No,Date,Type,InExp,Money fields. The field `money` can be zero.
 */
open class DbTransaction: Cloneable, Serializable {
    enum class InExp(val v: Int) {
        INCOME(0), EXPENSE(1), BUDGET(2)
    }

    constructor(no: Long = Utils.getCurrentTimeEpoch()) {
        transNo = no
    }

    constructor(
        date: Int, type: String, inExp: InExp, money: Double, no: Long = Utils.getCurrentTimeEpoch()
    ) {
        transNo = no
        transDate = date
        transType = type
        transInExp = inExp
        transMoney = money
    }

    constructor(trans: DbTransaction) {
        transNo = trans.transNo
        copyFrom(trans)
    }

    /** Copy whole data except the transaction number. */
    fun copyFrom(rhs: DbTransaction) {
        this.transDate = rhs.transDate
        this.transType = rhs.transType
        this.transInExp = rhs.transInExp
        this.transMoney = rhs.transMoney
        this.transDescription = rhs.transDescription
        this.transLocation = rhs.transLocation
    }

    var transNo = Utils.getCurrentTimeEpoch()
    var transDate = 0
    var transType = ""
    var transInExp = InExp.EXPENSE
    var transMoney = 0.0
    var transDescription = ""
    var transLocation = ""
}
