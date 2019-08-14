package corexchange.corexflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens
import corexchange.*
import corexchange.contracts.UserContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateRef
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
class TransferTokensToUserFlow (private val preOrderId: String,
                                private val walletRef: String,
                                private val userId: String): CorexFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransactionUser = verifyAndSign(transferToUser())

        val transactionSignedByPartiesUser = subFlow(CollectSignaturesFlow(signedTransactionUser, listOf()))
        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALIZING
        subFlow(FinalityFlow(transactionSignedByPartiesUser, listOf()))

        // Update Corex Wallet
        val preOrder = inputPreOrderRefUsingLinearID(stringToLinearID(preOrderId)).state.data
        val wallet = serviceHub.toStateAndRef<FungibleToken>(stringToStateRef(walletRef)).state.data
        subFlow(RedeemFungibleTokens(Amount(preOrder.amount, TokenType(wallet.tokenType.tokenIdentifier, wallet.tokenType.fractionDigits)), wallet.issuer))

        // Remove Order
        return subFlow(CorexRemovePreOrderFlow(preOrderId))
    }

    // Wallet of user
    private fun newWallet(): MutableList<Amount<TokenType>>
    {
        val preOrder = inputPreOrderRefUsingLinearID(stringToLinearID(preOrderId)).state.data
        val wallet = serviceHub.toStateAndRef<FungibleToken>(stringToStateRef(walletRef)).state.data
        val user = inputUserRefUsingLinearID(stringToLinearID(userId)).state.data
        val filteredListOfWallet = user.wallet.filter { x -> x.token.tokenIdentifier == preOrder.currency && x.token.tokenIdentifier == wallet.tokenType.tokenIdentifier}
        val newUserWallet= user.wallet.minus(filteredListOfWallet[0])
        val newQuantity = filteredListOfWallet[0].quantity + preOrder.amount
        val newElement= Amount(newQuantity, TokenType(wallet.tokenType.tokenIdentifier, wallet.tokenType.fractionDigits))
        return newUserWallet.plus(newElement).toMutableList()
    }

    private fun transferToUser() = TransactionBuilder(notary = inputUserRefUsingLinearID(stringToLinearID(userId)).state.notary).apply {
        val userState = inputUserRefUsingLinearID(stringToLinearID(userId)).state.data
        val userCommand = Command(UserContract.Commands.Transfer(), ourIdentity.owningKey)
        addInputState(inputUserRefUsingLinearID(stringToLinearID(userId)))
        addOutputState(userState.copy(wallet = newWallet()), UserContract.CONTRACT_ID)
        addCommand(userCommand)
    }
}

