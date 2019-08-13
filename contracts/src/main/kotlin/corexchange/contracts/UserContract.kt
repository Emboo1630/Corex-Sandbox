package corexchange.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

class UserContract : Contract
{
    companion object
    {
        const val CONTRACT_ID = "corexchange.contracts.UserContract"
    }

    override fun verify(tx: LedgerTransaction)
    {

    }

    interface Commands : CommandData
    {
        class Register : TypeOnlyCommandData(), Commands
        class Transfer : TypeOnlyCommandData(), Commands
        class Move : TypeOnlyCommandData(), Commands
    }
}