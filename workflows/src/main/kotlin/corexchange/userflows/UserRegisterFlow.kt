package corexchange.userflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.FiatCurrency
import corexchange.*
import corexchange.contracts.UserContract
import corexchange.states.UserState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.ArrayList

@StartableByRPC
class UserRegisterFlow (private val name: String,
                        private val wallet: MutableList<Amount<TokenType>>): UserFunctions()
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
                wallet = wallet,
                linearId = UniqueIdentifier(),
                participants = listOf(ourIdentity)
        )
    }

    private fun register() = TransactionBuilder(notary = getPreferredNotary(serviceHub)).apply {
        val command = Command(UserContract.Commands.Register(), ourIdentity.owningKey)
        addOutputState(outState(), UserContract.CONTRACT_ID)
        addCommand(command)
    }
}