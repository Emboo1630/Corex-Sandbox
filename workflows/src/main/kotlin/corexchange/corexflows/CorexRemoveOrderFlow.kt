package corexchange.corexflows

import co.paralleluniverse.fibers.Suspendable
import corexchange.*
import corexchange.contracts.OrderContract
import corexchange.issuerflows.IssuerFunctions
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.transactions.*

@InitiatingFlow
class CorexRemoveOrderFlow (private val recipient: String,
                            private val orderId: String): IssuerFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(removeOrder())
        val session = initiateFlow(stringToParty(recipient))
        val transactionSignedByParties = subFlow(CollectSignaturesFlow(signedTransaction, listOf(session)))

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALIZING
        return subFlow(FinalityFlow(transactionSignedByParties, listOf(session)))
    }

    private fun removeOrder() = TransactionBuilder(notary = inputOrderRefUsingLinearID(stringToLinearID(orderId)).state.notary).apply {
        val cmd = Command(
                OrderContract.Commands.Remove(), listOf(ourIdentity.owningKey, stringToParty(recipient).owningKey)
        )
        addInputState(inputOrderRefUsingLinearID(stringToLinearID(orderId)))
        addCommand(cmd)
    }
}

@InitiatedBy(CorexRemoveOrderFlow::class)
class CorexRemoveOrderFlowResponder(private val flowSession: FlowSession): FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        subFlow(object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction)
            {
            }
        })
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}