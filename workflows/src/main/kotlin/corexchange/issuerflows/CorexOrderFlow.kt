package corexchange.issuerflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import corexchange.*
import corexchange.contracts.OrderContract
import corexchange.states.OrderState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class CorexOrderFlow (private val amount: Long,
                      private val currency: String): IssuerFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        val orderToken = order()

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(orderToken)

        // Issue PHP to issuer php node if currency = PHP
        if (currency == "PHP")
        {
            val session = initiateFlow(stringToParty("IssuerPHP"))
            val transactionSignedByParties = subFlow(CollectSignaturesFlow(signedTransaction, listOf(session)))

            progressTracker.currentStep = NOTARIZING
            progressTracker.currentStep = FINALIZING
            return subFlow(FinalityFlow(transactionSignedByParties, listOf(session)))
        }

        // Else, issue usd to issuer usd node
        else
        {
            val session = initiateFlow(stringToParty("IssuerUSD"))
            val transactionSignedByParties = subFlow(CollectSignaturesFlow(signedTransaction, listOf(session)))

            progressTracker.currentStep = NOTARIZING
            progressTracker.currentStep = FINALIZING
            return subFlow(FinalityFlow(transactionSignedByParties, listOf(session)))
        }
    }

    private fun outState(): OrderState
    {
        if (currency == "PHP")
        {
            return OrderState(
                    amount = amount,
                    currency = currency,
                    issuer = stringToParty("IssuerPHP"),
                    linearId = UniqueIdentifier(),
                    participants = listOf(ourIdentity, stringToParty("IssuerPHP"))
            )
        }
        else
        {
            return OrderState(
                    amount = amount,
                    currency = currency,
                    issuer = stringToParty("IssuerUSD"),
                    linearId = UniqueIdentifier(),
                    participants = listOf(ourIdentity, stringToParty("IssuerUSD"))
            )
        }
    }

    private fun order() = TransactionBuilder(notary = getPreferredNotary(serviceHub)).apply {
        val cmd = if (currency == "PHP")
        {
            Command(OrderContract.Commands.Order(),
                    listOf(ourIdentity.owningKey, stringToParty("IssuerPHP").owningKey))
        }
        else
        {
            Command(OrderContract.Commands.Order(),
                    listOf(ourIdentity.owningKey, stringToParty("IssuerUSD").owningKey))
        }
        addOutputState(outState(), OrderContract.ORDER_ID)
        addCommand(cmd)
    }
}

@InitiatedBy(CorexOrderFlow::class)
class CorexOrderFlowResponder(private val flowSession: FlowSession): FlowLogic<SignedTransaction>()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        subFlow(object : SignTransactionFlow(flowSession)
        {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an order transaction" using (output is OrderState)
            }
        })
        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
    }
}