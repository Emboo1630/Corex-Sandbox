package corexchange.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

class OrderContract : Contract
{
    companion object
    {
        const val ORDER_ID = "corexchange.contracts.OrderContract"
    }

    override fun verify(tx: LedgerTransaction)
    {

    }

    interface Commands : CommandData
    {
        class Order : TypeOnlyCommandData(), Commands
        class Verify : TypeOnlyCommandData(), Commands
        class Remove : TypeOnlyCommandData(), Commands
    }
}