package corexchange.userflows

import co.paralleluniverse.fibers.Suspendable
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
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
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

@StartableByRPC
class ExchangeFlow (private val amount: Long,
                    private val currency: String,
                    private val userId: String): UserFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
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

        // External Data
        val httpclient = HttpClientBuilder.create().build()
        val request = HttpGet("https://api.exchangeratesapi.io/latest?base=USD&symbols=PHP,USD")
        val response = httpclient.execute(request)
        val inputStreamReader = InputStreamReader(response.entity.content)

        BufferedReader(inputStreamReader).use {
            val stringBuff = StringBuffer()
            var inputLine = it.readLine()
            while (inputLine != null) {
                stringBuff.append(inputLine)
                inputLine = it.readLine()
            }

            val gson = GsonBuilder().create()
            val jsonWholeObject = gson.fromJson(stringBuff.toString(), JsonObject::class.java)
            val rates = jsonWholeObject.get("rates").asJsonObject
            val usd = rates.get("USD").asLong
            val php = rates.get("PHP").asLong

            // Currency to be filtered
            val filteredListOfWallet = user.wallet.filter { x -> x.token.tokenIdentifier == currency }
            val newUserWallet = user.wallet.minus(filteredListOfWallet[0])
            val newQuantity = filteredListOfWallet[0].quantity - (amount * 100)
            val newElement = Amount(newQuantity, TokenType(currency, filteredListOfWallet[0].token.fractionDigits))
            val updatedUserWallet = newUserWallet.plus(newElement).toMutableList()

            // Currency to be exchanged
            val exchangeFilteredWallet = updatedUserWallet.filter { x -> x.token.tokenIdentifier != currency }
            val exchangeUserWallet = updatedUserWallet.minus(exchangeFilteredWallet[0])
            return if (currency == "USD")
            {
                val exchangeQuantity = exchangeFilteredWallet[0].quantity + ((amount * php) * 100)
                val exchangeElement = Amount(exchangeQuantity, TokenType(exchangeFilteredWallet[0].token.tokenIdentifier, exchangeFilteredWallet[0].token.fractionDigits))
                exchangeUserWallet.plus(exchangeElement).toMutableList()
            } else {
                val exchangeQuantity = exchangeFilteredWallet[0].quantity + ((amount / php) * 100)
                val exchangeElement = Amount(exchangeQuantity, TokenType(exchangeFilteredWallet[0].token.tokenIdentifier, exchangeFilteredWallet[0].token.fractionDigits))
                exchangeUserWallet.plus(exchangeElement).toMutableList()
            }
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