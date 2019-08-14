package corexchange.userflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.DigitalCurrency
import com.r3.corda.lib.tokens.money.FiatCurrency
import corexchange.*
import corexchange.contracts.UserContract
import corexchange.states.UserState
import jdk.nashorn.internal.parser.Token
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.apache.commons.lang.mutable.Mutable
import org.hibernate.mapping.IndexedCollection
import java.util.ArrayList

@StartableByRPC
class UserRegisterFlow (private val name: String,
                        private val amount: MutableList<Long>,
                        private val currency: MutableList<String>,
                        private val fractionDigits: MutableList<Int>): UserFunctions()
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
        var index = 0
        val listOfAmountCurrencyAndFractionDigits = mutableListOf(Amount(amount[index], TokenType(tokenIdentifier = currency[index], fractionDigits = fractionDigits[index])))
        val iterate = amount.map { it }.size
        listOfAmountCurrencyAndFractionDigits.removeAt(index)
        while(index != iterate)
        {
            listOfAmountCurrencyAndFractionDigits.add(Amount(amount[index],TokenType(tokenIdentifier = currency[index], fractionDigits = fractionDigits[index])))
            index++
        }
        return listOfAmountCurrencyAndFractionDigits
    }
    private fun register() = TransactionBuilder(notary = getPreferredNotary(serviceHub)).apply {
        val command = Command(UserContract.Commands.Register(), ourIdentity.owningKey)
        addOutputState(outState(), UserContract.CONTRACT_ID)
        addCommand(command)
    }
}