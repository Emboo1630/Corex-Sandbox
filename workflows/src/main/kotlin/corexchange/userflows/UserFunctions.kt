package corexchange.userflows

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.redeem.RedeemFungibleTokensFlow
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens
import corexchange.*
import corexchange.states.UserState
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

abstract class UserFunctions : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker(
            CREATING, VERIFYING, SIGNING, NOTARIZING, FINALIZING
    )

    fun fungibleTokenState(): List<StateAndRef<FungibleToken>> {
        val query = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
        return serviceHub.vaultService.queryBy<FungibleToken>(query).states
    }

    fun mergeFungibleTokens(currency:String):SignedTransaction {
        val input = fungibleTokenState().filter { it.state.data.tokenType.tokenIdentifier == currency }
        var totalAmount: Long = 0
        for (states in input) {
            val fungibleStates = states.state.data
            val quantity = fungibleStates.amount.quantity
            totalAmount += quantity
        }
        var finalAmount = totalAmount.div(100)
        val tokens = FiatCurrency.getInstance(currency)

        for(state in input)
        {
            val fungibleStates = state.state.data
            val quantity = fungibleStates.amount.quantity
//            subFlow(RedeemFungibleTokens(Amount(quantity, TokenType(currency,
//                    fungibleStates.tokenType.fractionDigits)),fungibleStates.issuer))

        }
        return subFlow(IssueTokens(listOf(finalAmount of tokens issuedBy ourIdentity heldBy ourIdentity)))

    }
    fun verifyAndSign(transaction: TransactionBuilder): SignedTransaction
    {
        transaction.verify(serviceHub)
        return serviceHub.signInitialTransaction(transaction)
    }

    fun inputUserRefUsingLinearID(id: UniqueIdentifier): StateAndRef<UserState>
    {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id))
        return serviceHub.vaultService.queryBy<UserState>(criteria = criteria).states.single()
    }

    fun stringToLinearID(id: String): UniqueIdentifier
    {
        return UniqueIdentifier.fromString(id)
    }

    fun stringToStateRef(stateRef: String): StateRef
    {
        return StateRef(txhash = SecureHash.parse(stateRef), index = 0)
    }

    fun returnExternalPhp(): Long {
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
            return rates.get("PHP").asLong
        }
    }
}