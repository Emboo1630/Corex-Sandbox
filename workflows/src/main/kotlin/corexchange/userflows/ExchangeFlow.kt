package corexchange.userflows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import corexchange.*
import corexchange.contracts.UserContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
class ExchangeFlow (private val amount: Long,
                    private val currency: String,
                    private val userId: String,
                    private val walletRefIncreased: String,
                    private val walletRefReduced: String): UserFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        // Update Corex Wallet
        val walletRefIncreased = serviceHub.toStateAndRef<FungibleToken>(stringToStateRef(walletRefIncreased)).state.data
        if (walletRefIncreased.tokenType.tokenIdentifier == "PHP" || walletRefIncreased.tokenType.tokenIdentifier == "USD")
        {
            val tokens = FiatCurrency.getInstance(currency)
            subFlow(IssueTokens(listOf(amount of tokens issuedBy ourIdentity heldBy ourIdentity)))
            //subFlow merge after issuing tokens

            if (currency == "USD")
            {
                val walletReduced = serviceHub.toStateAndRef<FungibleToken>(stringToStateRef(walletRefReduced)).state.data
                val amountWithCurrency = (amount * returnExternalPhp()).toBigDecimal()
                val tokenType = FiatCurrency.getInstance(walletReduced.tokenType.tokenIdentifier)
                subFlow(RedeemFungibleTokens(Amount.fromDecimal(amountWithCurrency, token = tokenType), walletReduced.issuer))
            }
            else if (currency == "PHP")
            {
                val walletReduced = serviceHub.toStateAndRef<FungibleToken>(stringToStateRef(walletRefReduced)).state.data
                val amountWithCurrency = (amount / returnExternalPhp()).toBigDecimal()
                val tokenType = FiatCurrency.getInstance(walletReduced.tokenType.tokenIdentifier)
                subFlow(RedeemFungibleTokens(Amount.fromDecimal(amountWithCurrency, token = tokenType), walletReduced.issuer))
            }
        }

        /**
         * Exchange currency on the user wallet
         */
        progressTracker.currentStep = CREATING
        progressTracker.currentStep = VERIFYING
        progressTracker.currentStep = SIGNING
        val signedTransaction = verifyAndSign(exchange())

        val transactionSignedByPartiesUser = subFlow(CollectSignaturesFlow(signedTransaction, listOf()))
        progressTracker.currentStep = NOTARIZING
        progressTracker.currentStep = FINALIZING
        return subFlow(FinalityFlow(transactionSignedByPartiesUser, listOf()))
    }

    // Wallet of user
    private fun newExchangeWallet(): MutableList<Amount<TokenType>>
    {
        val user = inputUserRefUsingLinearID(stringToLinearID(userId)).state.data

        // Currency to be filtered
        val filteredListOfWallet = user.wallet.filter { x -> x.token.tokenIdentifier == currency }
        val newUserWallet = user.wallet.minus(filteredListOfWallet[0])
        val newQuantity = filteredListOfWallet[0].quantity - (amount * 100)
        val tokenType = FiatCurrency.getInstance(currency)
        val newElement = Amount(newQuantity, tokenType)
        val updatedUserWallet = newUserWallet.plus(newElement).toMutableList()

        // Currency to be exchanged
        val exchangeFilteredWallet = updatedUserWallet.filter { x -> x.token.tokenIdentifier != currency }
        val exchangeUserWallet = updatedUserWallet.minus(exchangeFilteredWallet[0])
        return if (currency == "USD")
        {
            val exchangeQuantity = (exchangeFilteredWallet[0].quantity / 100) + (amount * returnExternalPhp())
            val exchangeTokenType = FiatCurrency.getInstance(exchangeFilteredWallet[0].token.tokenIdentifier)
            val exchangeElement = Amount.fromDecimal(exchangeQuantity.toBigDecimal(), exchangeTokenType)
            exchangeUserWallet.plus(exchangeElement).toMutableList()
        } else {
            val exchangeQuantity = (exchangeFilteredWallet[0].quantity / 100) + (amount / returnExternalPhp())
            val exchangeTokenType = FiatCurrency.getInstance(exchangeFilteredWallet[0].token.tokenIdentifier)
            val exchangeElement = Amount.fromDecimal(exchangeQuantity.toBigDecimal(), exchangeTokenType)
            exchangeUserWallet.plus(exchangeElement).toMutableList()
        }
    }

    private fun exchange() = TransactionBuilder(notary = getPreferredNotary(serviceHub)).apply {
        val userRef = inputUserRefUsingLinearID(stringToLinearID(userId))
        val user = inputUserRefUsingLinearID(stringToLinearID(userId)).state.data
        val cmd = Command(UserContract.Commands.Exchange(), listOf(ourIdentity.owningKey))
        addInputState(userRef)
        addOutputState(user.copy(wallet = newExchangeWallet()), UserContract.CONTRACT_ID)
        addCommand(cmd)
    }
}