package corexchange.userflows

import corexchange.*
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.transactions.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import corexchange.contracts.UserContract
import corexchange.states.UserState
import java.util.*


@StartableByRPC
class UserRegisterFlow (private val name: String,
                        private val amount: MutableList<Long>,
                        private val currency: MutableList<String>): UserFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        val registration = register()

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(registration)
        val transactionSignedByParties = subFlow(CollectSignaturesFlow(signedTransaction, listOf()))

        progressTracker.currentStep = FINALIZING
        return subFlow(FinalityFlow(transactionSignedByParties, listOf()))
    }

    private fun outState(): UserState
    {
        return UserState(
                name = name,
                wallet = amountAndCurrencyToTokenType(),
                linearId = UniqueIdentifier(),
                participants = listOf(ourIdentity)
        )
    }

    private fun amountAndCurrencyToTokenType(): MutableList<Amount<TokenType>>
    {
        val wallet = mutableListOf<Amount<TokenType>>()
        for ((index, value) in amount.withIndex())
        {
            val token = FiatCurrency.getInstance(currency[index])
            wallet.add(index,value of token)
            // TODO - if currency != usd and php, give user-define token identifier with matching fraction digits
        }
        return wallet
    }
    private fun register() = TransactionBuilder(notary = getPreferredNotary(serviceHub)).apply {
        val command = Command(UserContract.Commands.Register(), ourIdentity.owningKey)
        addOutputState(outState(), UserContract.CONTRACT_ID)
        addCommand(command)
    }
}