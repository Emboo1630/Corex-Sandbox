package corexchange.userflows

import corexchange.*
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.transactions.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import corexchange.contracts.ReserveOrderContract
import corexchange.states.ReserveOrderState

@InitiatingFlow
@StartableByRPC
class ReserveTokensFlow (private val amount: Long,
                         private val currency: String): UserFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(preOrder())

        progressTracker.currentStep = FINALIZING
        return subFlow(FinalityFlow(signedTransaction, listOf()))
    }

    private fun outState(): ReserveOrderState
    {
        return ReserveOrderState(
                amount = amount,
                currency = currency,
                linearId = UniqueIdentifier(),
                participants = listOf(ourIdentity)
        )
    }

    private fun preOrder() = TransactionBuilder(notary = getPreferredNotary(serviceHub)).apply {
        val cmd = Command(ReserveOrderContract.Commands.PreOrder(), listOf(ourIdentity.owningKey))
        addOutputState(outState(), ReserveOrderContract.PREORDER_ID)
        addCommand(cmd)
    }
}