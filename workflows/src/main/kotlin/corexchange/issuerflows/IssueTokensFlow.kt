package corexchange.issuerflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class IssueTokensFlow (private val recipient: String,
                       private val orderId: String): IssuerFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        // Issue tokens from an Issuer Node -> Exchange Platform
        val input = inputOrderRefUsingLinearID(stringToLinearID(orderId)).state.data
        val tokens = FiatCurrency.getInstance(input.currency)
        return subFlow(IssueTokens(listOf(input.amount of tokens issuedBy ourIdentity heldBy stringToParty(recipient))))
    }
}