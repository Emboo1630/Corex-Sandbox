package corexchange.corexflows

import corexchange.*
import corexchange.states.*
import net.corda.core.contracts.*
import net.corda.core.transactions.*
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.ProgressTracker

abstract class CorexFunctions : FlowLogic<SignedTransaction>()
{
    override val progressTracker = ProgressTracker(
            CREATING, VERIFYING, SIGNING, NOTARIZING, FINALIZING
    )

    fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    fun stringToParty(name: String): Party {
        return serviceHub.identityService.partiesFromName(name, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for $name")
    }

    fun stringToLinearID(id: String): UniqueIdentifier {
        return UniqueIdentifier.fromString(id)
    }

    fun inputUserRefUsingLinearID(id: UniqueIdentifier): StateAndRef<UserState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id))
        return serviceHub.vaultService.queryBy<UserState>(criteria = criteria).states.single()
    }

    fun inputReserveOrderRefUsingLinearID(id: UniqueIdentifier): StateAndRef<ReserveOrderState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id))
        return serviceHub.vaultService.queryBy<ReserveOrderState>(criteria = criteria).states.single()
    }

    fun stringToStateRef(stateRef: String): StateRef
    {
        return StateRef(txhash = SecureHash.parse(stateRef), index = 0)
    }
}