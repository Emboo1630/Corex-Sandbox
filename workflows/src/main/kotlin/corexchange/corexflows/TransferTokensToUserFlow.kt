package corexchange.corexflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.issue.addIssueTokens
import corexchange.CREATING
import corexchange.FINALIZING
import corexchange.SIGNING
import corexchange.VERIFYING
import corexchange.contracts.UserContract
import corexchange.contracts.WalletContract
import corexchange.states.WalletState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
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
        val wallet = serviceHub.toStateAndRef<WalletState>(walletRef).state.data
        val walletInputState = serviceHub.toStateAndRef<WalletState>(walletRef)
        val updateCorexWallet = TransactionBuilder(notary = serviceHub.toStateAndRef<WalletState>(walletRef).state.notary)
        val updatedWallet = wallet.copy(amount = newAmount())
        addUpdateTokens(updateCorexWallet, listOf(updatedWallet), listOf(walletInputState))

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
        val wallet = serviceHub.toStateAndRef<WalletState>(walletRef).state.data
        val user = inputUserRefUsingLinearID(stringToLinearID(userId)).state.data
        val filteredListOfWallet = user.wallet.filter { x -> x.token.tokenIdentifier == wallet.tokenType.tokenIdentifier }
        val newUserWallet= user.wallet.minus(filteredListOfWallet[0])
        val newQuantity = filteredListOfWallet[0].quantity + amount
        val newElement = Amount(newQuantity, TokenType(wallet.tokenType.tokenIdentifier, wallet.tokenType.fractionDigits))
        return newUserWallet.plus(newElement).toMutableList()
    }

    // Wallet of Corex
    private fun newAmount(): Amount<IssuedTokenType>
    {
        val wallet = serviceHub.toStateAndRef<WalletState>(walletRef).state.data
        val walletTokenType = IssuedTokenType(ourIdentity, TokenType(wallet.tokenType.tokenIdentifier, wallet.tokenType.fractionDigits))
        val newQuantity = wallet.amount.quantity - amount
        return Amount(newQuantity, walletTokenType)
    }

    private fun transferToUser() = TransactionBuilder(notary = inputUserRefUsingLinearID(stringToLinearID(userId)).state.notary).apply {
        val userState = inputUserRefUsingLinearID(stringToLinearID(userId)).state.data
        val userCommand = Command(UserContract.Commands.Transfer(), ourIdentity.owningKey)
        addInputState(inputUserRefUsingLinearID(stringToLinearID(userId)))
        addOutputState(userState.copy(wallet = newWallet()), UserContract.CONTRACT_ID)
        addCommand(userCommand)
    }

    @Suspendable
    fun addUpdateTokens(transactionBuilder: TransactionBuilder, outputs: List<AbstractToken>, inputs: List<StateAndRef<AbstractToken>>): TransactionBuilder {
        val outputGroups: Map<IssuedTokenType, List<AbstractToken>> = outputs.groupBy { it.issuedTokenType }
        val inputGroups: Map<IssuedTokenType, List<StateAndRef<AbstractToken>>> = inputs.groupBy { it.state.data.issuedTokenType }
        return transactionBuilder.apply {
            TODO()
//            outputGroups.forEach { (issuedTokenType: IssuedTokenType, states: List<AbstractToken>) ->
//                val inputGroup = inputGroups[issuedTokenType]
//                        ?: throw IllegalArgumentException("No corresponding inputs for the outputs issued token type: $issuedTokenType")
//                val issuers = states.map { it.issuer }.toSet()
//                require(issuers.size == 1) { "All tokensToIssue must have the same issuer." }
//                val issuer = issuers.single()
//
//                var inputStartingIdx = inputStates().size
//                var outputStartingIdx = outputStates().size
//                val inputIdx = inputGroup.map {
//                    addInputState(it)
//                    inputStartingIdx++
//                }
//                val outputIdx = states.map {
//                    addOutputState(it)
//                    outputStartingIdx++
//                }
//                addCommand(WalletContract.UpdateTokenCommand(issuedTokenType, inputIdx, outputIdx), issuer.owningKey)
//            }
        }
    }
}

