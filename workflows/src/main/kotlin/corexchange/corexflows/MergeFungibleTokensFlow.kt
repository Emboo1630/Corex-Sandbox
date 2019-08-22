package corexchange.corexflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.contracts.Amount
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

class MergeFungibleTokensFlow (private val currency: String): CorexFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val tokens = serviceHub.vaultService.queryBy(FungibleToken::class.java).states.filter {
            it.state.data.tokenType.tokenIdentifier == currency
        }

        var newQuantity = 0.0
        for (x in tokens)
        {
             newQuantity += x.state.data.amount.quantity
        }

        val adjustedQuantity = newQuantity / 100
        val tokenType = FiatCurrency.getInstance(currency)
        return subFlow(MoveFungibleTokens(PartyAndAmount(stringToParty("CorexPlatform"), adjustedQuantity of tokenType)))
    }
}