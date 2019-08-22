package corexchange.issuerflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.*
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import corexchange.corexflows.CorexRemoveOrderFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction

@InitiatingFlow
@StartableByRPC
class MoveTokensToPlatformFlow (private val recipient: String,
                                private val orderId: String): IssuerFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        // Move tokens from an Issuer Node -> Exchange Platform
        val input = inputOrderRefUsingLinearID(stringToLinearID(orderId)).state.data
        if (input.status != "verified")
        {
            throw FlowException("Status should be verified before moving tokens from issuer to platform")
        }
        val tokens = FiatCurrency.getInstance(input.currency)
        subFlow(MoveFungibleTokens(PartyAndAmount(stringToParty(recipient), input.amount of tokens)))

        // Remove Order from platform -> issuer
        return subFlow(CorexRemoveOrderFlow(recipient, orderId))
    }
}