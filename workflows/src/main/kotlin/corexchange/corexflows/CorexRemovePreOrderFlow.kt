package corexchange.corexflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import corexchange.CREATING
import corexchange.FINALIZING
import corexchange.SIGNING
import corexchange.VERIFYING
import corexchange.contracts.PreOrderContract
import net.corda.core.contracts.Command
import net.corda.core.flows.FinalityFlow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

class CorexRemovePreOrderFlow (private val preOrderId: String): CorexFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(removePreOrder())

        progressTracker.currentStep = FINALIZING
        return subFlow(FinalityFlow(signedTransaction, listOf()))
    }

    private fun removePreOrder() = TransactionBuilder(notary = getPreferredNotary(serviceHub)).apply {
        val input = inputPreOrderRefUsingLinearID(stringToLinearID(preOrderId))
        val cmd = Command(PreOrderContract.Commands.RemovePreOrder(), listOf(ourIdentity.owningKey))
        addInputState(input)
        addCommand(cmd)
    }
}