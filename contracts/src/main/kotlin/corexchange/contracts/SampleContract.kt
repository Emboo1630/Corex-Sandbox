package corexchange.contracts

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.transactions.LedgerTransaction

class SampleContract : Contract
{
    companion object
    {
        const val sample_id= "corexchange.contracts.SampleContract"
    }

    override fun verify(tx: LedgerTransaction)
    {

    }

    interface Commands : CommandData
    {
        class SampleCommand : TypeOnlyCommandData(), Commands
    }
}