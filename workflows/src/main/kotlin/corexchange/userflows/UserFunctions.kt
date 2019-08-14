package corexchange.userflows

import corexchange.*
import net.corda.core.contracts.*
import net.corda.core.transactions.*
import corexchange.states.UserState
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

import net.corda.core.utilities.ProgressTracker

abstract class UserFunctions : FlowLogic<SignedTransaction>()
{
    override val progressTracker = ProgressTracker(
            CREATING, VERIFYING, SIGNING, NOTARIZING, FINALIZING
    )
    fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    fun inputUserRefUsingLinearID(id: UniqueIdentifier): StateAndRef<UserState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id))
        return serviceHub.vaultService.queryBy<UserState>(criteria = criteria).states.single()
    }

    fun stringToParty(name: String): Party {
        return serviceHub.identityService.partiesFromName(name, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for $name")
    }

    fun stringToLinearID(id: String): UniqueIdentifier {
        return UniqueIdentifier.fromString(id)
    }
}