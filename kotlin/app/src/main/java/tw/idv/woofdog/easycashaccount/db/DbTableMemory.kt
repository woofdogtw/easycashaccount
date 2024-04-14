package tw.idv.woofdog.easycashaccount.db

import java.util.Vector

/**
 * The memory implementation of DbTableBase.
 *
 * This class implements complete database operations for this program, but all data are in memory.
 *
 * This class is used for handling volatile database, such as find result and importing other
 * database format data.
 */
class DbTableMemory : DbTableBase {
    override fun getDescription(): String {
        return dbDescription
    }

    override fun getLastModified(): Long {
        return dbLastModified
    }

    override fun setFileName(name: String): Boolean {
        return false
    }

    override fun setDescription(description: String): Boolean {
        if (isReadOnly) {
            return false
        }

        dbDescription = description
        return true
    }

    override fun setLastModified(lastTime: Long): Boolean {
        if (isReadOnly) {
            return false
        }

        dbLastModified = lastTime
        return true
    }

    override fun getTransTypeNumber(): Int {
        return dbTransTypes.count()
    }

    override fun getTransTypes(): Vector<String> {
        val list = Vector<String>(dbTransTypes.count())
        for (t in dbTransTypes) {
            list.add(t)
        }
        return list
    }

    override fun addTransType(type: String): Boolean {
        if (isReadOnly || type == "") {
            return false
        } else if (dbTransTypes.contains(type)) {
            return false
        }

        dbTransTypes.add(type)
        setLastModified()
        return true
    }

    override fun deleteTransType(type: String): Boolean {
        if (isReadOnly || type == "") {
            return false
        }

        for (t in dbTransactions) {
            if (t.transType == type) {
                return false
            }
        }
        dbTransTypes.remove(type)
        setLastModified()
        return true
    }

    override fun modifyTransType(fromType: String, toType: String, merge: Boolean): Boolean {
        if (isReadOnly || fromType == "" || toType == "") {
            return false
        } else if (fromType == toType || !dbTransTypes.contains(fromType)) {
            return true
        }

        if (dbTransTypes.contains(toType)) {
            if (!merge) {
                return false
            }
            dbTransTypes.remove(fromType)
        } else {
            dbTransTypes[dbTransTypes.indexOf(fromType)] = toType
        }

        for (t in dbTransactions) {
            if (t.transType == fromType) {
                t.transType = toType
            }
        }
        setLastModified()
        return true
    }

    override fun getTransactionNumber(): Int {
        return dbTransactions.count()
    }

    override fun getTransactions(): Vector<DbTransaction> {
        val list = Vector<DbTransaction>(dbTransactions.count())
        for (t in dbTransactions) {
            list.add(DbTransaction(t))
        }
        return list
    }

    override fun getTransaction(transNo: Long): DbTransaction? {
        for (t in dbTransactions) {
            if (t.transNo == transNo) {
                return DbTransaction(t)
            }
        }
        return null
    }

    override fun addTransaction(trans: DbTransaction): Boolean {
        if (isReadOnly) {
            return false
        }

        for (t in dbTransactions) {
            if (t.transNo == trans.transNo) {
                return false
            }
        }
        dbTransactions.add(DbTransaction(trans))
        setLastModified()
        return true
    }

    override fun deleteTransaction(transNo: Long): Boolean {
        if (isReadOnly) {
            return false
        }

        for (t in dbTransactions) {
            if (t.transNo == transNo) {
                dbTransactions.remove(t)
                return true
            }
        }
        return true
    }

    override fun modifyTransaction(transNo: Long, trans: DbTransaction): Boolean {
        if (isReadOnly) {
            return false
        }

        for (t in dbTransactions) {
            if (t.transNo == transNo) {
                t.copyFrom(trans)
                return true
            }
        }
        return false
    }

    override fun queryAND(cond: DbTableBase.QueryCond): Vector<DbTransaction> {
        val queryResult = Vector<DbTransaction>()

        val (
            fromNo, toNo, fromDate, toDate, type, inExp, fromMoney, toMoney, description, location
        ) = cond

        for (t in dbTransactions) {
            if (
                fromNo != null && t.transNo < fromNo ||
                toNo != null && t.transNo > toNo ||
                fromDate != null && t.transDate < fromDate ||
                toDate != null && t.transDate > toDate ||
                type != null && t.transType != type ||
                inExp != null && t.transInExp != inExp ||
                fromMoney != null && t.transMoney < fromMoney ||
                toMoney != null && t.transMoney > toMoney ||
                description != null && t.transDescription.contains(description, true) ||
                location != null && t.transLocation.contains(location)
            ) {
                continue
            }
            queryResult.add(DbTransaction(t))
        }

        return queryResult
    }

    override fun queryOR(cond: DbTableBase.QueryCond): Vector<DbTransaction> {
        val queryResult = Vector<DbTransaction>()

        val (
            fromNo, toNo, fromDate, toDate, type, inExp, fromMoney, toMoney, description, location
        ) = cond

        for (t in dbTransactions) {
            if (
                type != null && t.transType != type ||
                inExp != null && t.transInExp != inExp ||
                description != null && t.transDescription.contains(description, true) ||
                location != null && t.transLocation.contains(location)
            ) {
                queryResult.add(DbTransaction(t))
                continue
            }
            if (fromNo != null) {
                if (
                    toNo != null && t.transNo >= fromNo && t.transNo <= toNo ||
                    toNo == null && t.transNo >= fromNo
                ) {
                    queryResult.add(DbTransaction(t))
                    continue
                }
            } else if (toNo != null && t.transNo <= toNo) {
                queryResult.add(DbTransaction(t))
                continue
            }
            if (fromDate != null) {
                if (
                    toDate != null && t.transDate >= fromDate && t.transDate <= toDate ||
                    toDate == null && t.transDate >= fromDate
                ) {
                    queryResult.add(DbTransaction(t))
                    continue
                }
            } else if (toDate != null && t.transDate <= toDate) {
                queryResult.add(DbTransaction(t))
                continue
            }
            if (fromMoney != null) {
                if (
                    toMoney != null && t.transMoney >= fromMoney && t.transMoney <= toMoney ||
                    toMoney == null && t.transMoney >= fromMoney
                ) {
                    queryResult.add(DbTransaction(t))
                    continue
                }
            } else if (toMoney != null && t.transMoney <= toMoney) {
                queryResult.add(DbTransaction(t))
                continue
            }
        }

        return queryResult
    }

    override var isReadOnly = false
    override var dbFileName = ""
    private var dbDescription = ""
    private var dbLastModified = 0L
    private var dbTransTypes = Vector<String>()
    private var dbTransactions = Vector<DbTransaction>()
}
