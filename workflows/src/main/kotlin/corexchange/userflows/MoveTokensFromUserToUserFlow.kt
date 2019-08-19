package corexchange.userflows

import corexchange.*
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.transactions.*
import corexchange.contracts.UserContract
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary

@StartableByRPC
class MoveTokensFromUserToUserFlow (private val senderId: String,
                                    private val receiverId: String,
                                    private val amount: Long,
                                    private val currency: String): UserFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        val transaction = moveTokens()

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val verify = verifyAndSign(transaction)
        val transactionSignedByBothParties: SignedTransaction = verify

        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALIZING
        return subFlow((FinalityFlow(transactionSignedByBothParties, listOf())))
    }

    private fun moveTokens() = TransactionBuilder(notary = getPreferredNotary(serviceHub)).apply {
        val sender = inputUserRefUsingLinearID(stringToLinearID(senderId)).state.data
        val receiver = inputUserRefUsingLinearID(stringToLinearID(receiverId)).state.data
        val command = Command(UserContract.Commands.Register(), ourIdentity.owningKey)
        val newSenderWallet = updateSenderWallet(stringToLinearID(senderId), currency, amount)
                .sortedBy { it.token.tokenIdentifier }.toMutableList()
        val newReceiverWallet = updateReceiverWallet(stringToLinearID(receiverId), currency, amount)
                .sortedBy { it.token.tokenIdentifier }.toMutableList()
        addInputState(inputUserRefUsingLinearID(stringToLinearID(receiverId)))
        addInputState(inputUserRefUsingLinearID(stringToLinearID(senderId)))
        addOutputState(sender.copy(wallet = newSenderWallet), contract = UserContract.CONTRACT_ID)
        addOutputState(receiver.copy(wallet = newReceiverWallet), contract = UserContract.CONTRACT_ID)
        addCommand(command)
    }

    private fun updateSenderWallet(linearId: UniqueIdentifier, currency: String, amount: Long): MutableList<Amount<TokenType>>
    {
        val state = inputUserRefUsingLinearID(linearId).state.data
        val stateWallet = state.wallet.find { x -> x.token.tokenIdentifier == currency }
                ?: throw IllegalArgumentException("No currency found")
        val wallet = state.wallet.filter { stateWallet.token.tokenIdentifier == currency }
        val totalAmount = stateWallet.quantity - (amount * 100)
        val token = Amount(totalAmount, FiatCurrency.getInstance(stateWallet.token.tokenIdentifier))
        val minusWallet = wallet.minus(stateWallet).toMutableList()
        return minusWallet.plus(token).toMutableList()
    }

    private fun updateReceiverWallet(linearId: UniqueIdentifier,currency:String,amount: Long): MutableList<Amount<TokenType>>
    {
        val state = inputUserRefUsingLinearID(linearId).state.data
        val stateWallet = state.wallet.find { x -> x.token.tokenIdentifier == currency }
                ?: throw IllegalArgumentException("No currency found")
        val wallet = state.wallet.filter { stateWallet.token.tokenIdentifier == currency }
        val totalAmount = stateWallet.quantity + (amount * 100)
        val token = Amount(totalAmount, FiatCurrency.getInstance(stateWallet.token.tokenIdentifier))
        val minusWallet = wallet.minus(stateWallet)
        return minusWallet.plus(token).toMutableList()
    }
}