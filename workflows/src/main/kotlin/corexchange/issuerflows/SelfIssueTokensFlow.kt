package corexchange.issuerflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction

@StartableByRPC
class SelfIssueTokensFlow (private val amount: Long,
                           private val currency: String): IssuerFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val tokens = FiatCurrency.getInstance(currency)
        return subFlow(IssueTokens(listOf(amount of tokens issuedBy ourIdentity heldBy ourIdentity)))
    }
}