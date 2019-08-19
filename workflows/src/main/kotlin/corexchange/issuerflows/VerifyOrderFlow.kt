package corexchange.issuerflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import corexchange.*
import corexchange.contracts.OrderContract
import corexchange.states.OrderState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class VerifyOrderFlow (private val linearId: String): IssuerFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(verifyOrder())

        val orderState = inputOrderRefUsingLinearID(stringToLinearID(linearId)).state.data
        val session = (orderState.participants - ourIdentity).map { initiateFlow(it) }

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALIZING
        val transactionSignedByParties = subFlow(CollectSignaturesFlow(signedTransaction, session))
        return subFlow(FinalityFlow(transactionSignedByParties, session))
    }

    private fun outState(): OrderState
    {
        val input = inputOrderRefUsingLinearID(stringToLinearID(linearId)).state.data
        return input.verify()
    }

    private fun verifyOrder() = TransactionBuilder(notary = getPreferredNotary(serviceHub)).apply {
        val input = inputOrderRefUsingLinearID(stringToLinearID(linearId)).state.data
        val inputStateRef = inputOrderRefUsingLinearID(stringToLinearID(linearId))
        val cmd = Command(OrderContract.Commands.Verify(), input.participants.map { it.owningKey })
        addInputState(inputStateRef)
        addOutputState(outState(), OrderContract.ORDER_ID)
        addCommand(cmd)
    }
}

@InitiatedBy(VerifyOrderFlow::class)
class VerifyOrderFlowResponder(private val flowSession: FlowSession): FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        subFlow(object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be a verify order transaction" using (output is OrderState)
            }
        })
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}