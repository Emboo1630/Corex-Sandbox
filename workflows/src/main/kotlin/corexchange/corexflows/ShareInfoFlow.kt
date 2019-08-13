package corexchange.corexflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import corexchange.contracts.UserContract
import corexchange.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class ShareInfoFlow (private val recipient: String): CorexFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.ALL)
        val x = serviceHub.vaultService.queryBy<UserState>(criteria).states
        val y = serviceHub.vaultService.queryBy<UserState>(criteria).states.map {
            it.state.data
        }

        val builder = TransactionBuilder(notary = getPreferredNotary(serviceHub)).apply {
            for (itx in x)
            {
                addInputState(itx)
            }

            for (ity in y)
            {
                addOutputState(ity.copy(participants = listOf(ourIdentity, stringToParty(recipient))), UserContract.CONTRACT_ID)
            }
            addCommand(Command(UserContract.Commands.Move(), listOf(ourIdentity.owningKey, stringToParty(recipient).owningKey)))
        }
        val signedTransaction = verifyAndSign(builder)
        val session = initiateFlow(stringToParty(recipient))
        val transactionSignedByParties = subFlow(CollectSignaturesFlow(signedTransaction, listOf(session)))
        return subFlow(FinalityFlow(transactionSignedByParties, listOf(session)))
    }
}

@InitiatedBy(ShareInfoFlow::class)
class ShareInfoFlowResponder(private val flowSession: FlowSession): FlowLogic<SignedTransaction>()
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