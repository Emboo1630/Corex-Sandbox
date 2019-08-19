package corexchange.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

class ReserveOrderContract : Contract
{
    companion object
    {
        const val PREORDER_ID = "corexchange.contracts.ReserveOrderContract"
    }

    override fun verify(tx: LedgerTransaction)
    {

    }

    interface Commands : CommandData
    {
        class PreOrder : TypeOnlyCommandData(), Commands
        class RemovePreOrder : TypeOnlyCommandData(), Commands
    }
}