package corexchange.userflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import corexchange.CREATING
import corexchange.FINALIZING
import corexchange.SIGNING
import corexchange.VERIFYING
import corexchange.contracts.PreOrderContract
import corexchange.states.PreOrderState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class PreOrderFlow (private val amount: Long,
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

    private fun outState(): PreOrderState
    {
        return PreOrderState(
                amount = amount,
                currency = currency,
                linearId = UniqueIdentifier(),
                participants = listOf(ourIdentity)
        )
    }

    private fun preOrder() = TransactionBuilder(notary = getPreferredNotary(serviceHub)).apply {
        val cmd = Command(PreOrderContract.Commands.PreOrder(), listOf(ourIdentity.owningKey))
        addOutputState(outState(), PreOrderContract.PREORDER_ID)
        addCommand(cmd)
    }
}