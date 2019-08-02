package corexchange.issuerflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import corexchange.states.WalletState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
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
        val tokenIssued = IssuedTokenType(ourIdentity, TokenType(input.currency, 2))
        val fungibleToken = WalletState(Amount(input.amount, tokenIssued), stringToParty(recipient))
        return subFlow(IssueTokens(listOf(fungibleToken)))
    }
}