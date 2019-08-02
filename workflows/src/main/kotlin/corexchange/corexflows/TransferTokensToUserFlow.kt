package corexchange.corexflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import corexchange.CREATING
import corexchange.FINALIZING
import corexchange.SIGNING
import corexchange.VERIFYING
import corexchange.contracts.UserContract
import corexchange.states.UserState
import corexchange.states.WalletState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateRef
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.stream.Collectors

@StartableByRPC
class TransferTokensToUserFlow (private val amount: Long,
                                private val walletRef: StateRef,
                                private val userId: String): CorexFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        val transferToken = transfer()

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(transferToken)
        val transactionSignedByParties = subFlow(CollectSignaturesFlow(signedTransaction, listOf()))

        progressTracker.currentStep = FINALIZING
        return subFlow(FinalityFlow(transactionSignedByParties, listOf()))
    }

    private fun newWallet(): MutableList<Amount<TokenType>>
    {
        val wallet = serviceHub.toStateAndRef<WalletState>(walletRef).state.data
        val user = inputUserRefUsingLinearID(stringToLinearID(userId)).state.data
        val filteredListOfWallet = user.wallet.filter { x -> x.token.tokenIdentifier == wallet.tokenType.tokenIdentifier }
        val newUserWallet= user.wallet.minus(filteredListOfWallet[0])
        val newQuantity = filteredListOfWallet[0].quantity + amount
        val newElement = Amount(newQuantity, TokenType(wallet.tokenType.tokenIdentifier, wallet.tokenType.fractionDigits))
        return newUserWallet.plus(newElement).toMutableList()
    }

    private fun newAmount(): Amount<IssuedTokenType>
    {
        val wallet = serviceHub.toStateAndRef<WalletState>(walletRef).state.data
        val walletTokenType = IssuedTokenType(ourIdentity, TokenType(wallet.tokenType.tokenIdentifier, wallet.tokenType.fractionDigits))
        val newQuantity = wallet.amount.quantity - amount
        return Amount(newQuantity, walletTokenType)
    }

    private fun transfer() = TransactionBuilder(notary = inputUserRefUsingLinearID(stringToLinearID(userId)).state.notary).apply {
        val userState = inputUserRefUsingLinearID(stringToLinearID(userId)).state.data
//        val walletInputState = serviceHub.toStateAndRef<WalletState>(walletRef)
//        val walletState = walletInputState.state.data
        val userCommand = Command(UserContract.Commands.Transfer(), ourIdentity.owningKey)
//        val walletCommand = Command()
        addInputState(inputUserRefUsingLinearID(stringToLinearID(userId)))
//        addInputState(walletInputState)
        addOutputState(userState.copy(wallet = newWallet()), UserContract.CONTRACT_ID)
//        addOutputState(walletState.copy(amount = newAmount()), FungibleTokenContract.contractId)
        addCommand(userCommand)
    }
}