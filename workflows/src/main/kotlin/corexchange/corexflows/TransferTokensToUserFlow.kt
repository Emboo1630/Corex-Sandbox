package corexchange.corexflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens
import corexchange.CREATING
import corexchange.FINALIZING
import corexchange.SIGNING
import corexchange.VERIFYING
import corexchange.contracts.UserContract
import corexchange.states.OrderState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateRef
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
class TransferTokensToUserFlow (private val amount: Long,
                                private val walletRef: StateRef,
                                private val userId: String): CorexFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        progressTracker.currentStep = CREATING
        val transferToken = transferToUser()

        // Update Corex Wallet
        val wallet = serviceHub.toStateAndRef<FungibleToken>(walletRef).state.data
        subFlow(RedeemFungibleTokens(Amount(amount, TokenType(wallet.tokenType.tokenIdentifier, wallet.tokenType.fractionDigits)), wallet.issuer))

        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransactionUser = verifyAndSign(transferToken)

        val transactionSignedByPartiesUser = subFlow(CollectSignaturesFlow(signedTransactionUser, listOf()))
        progressTracker.currentStep = FINALIZING
        return subFlow(FinalityFlow(transactionSignedByPartiesUser, listOf()))
    }

    // Wallet of user
    private fun newWallet(): MutableList<Amount<TokenType>>
    {
        val wallet = serviceHub.toStateAndRef<FungibleToken>(walletRef).state.data
        val user = inputUserRefUsingLinearID(stringToLinearID(userId)).state.data
        val filteredListOfWallet = user.wallet.filter { x -> x.token.tokenIdentifier == wallet.tokenType.tokenIdentifier }
        val newUserWallet= user.wallet.minus(filteredListOfWallet[0])
        val newQuantity = filteredListOfWallet[0].quantity + amount
        val newElement = Amount(newQuantity, TokenType(wallet.tokenType.tokenIdentifier, wallet.tokenType.fractionDigits))
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

