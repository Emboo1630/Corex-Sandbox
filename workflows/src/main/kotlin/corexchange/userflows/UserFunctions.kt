package corexchange.userflows

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import corexchange.*
import net.corda.core.contracts.*
import net.corda.core.transactions.*
import corexchange.states.UserState
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria

import net.corda.core.utilities.ProgressTracker
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

abstract class UserFunctions : FlowLogic<SignedTransaction>()
{
    override val progressTracker = ProgressTracker(
            CREATING, VERIFYING, SIGNING, NOTARIZING, FINALIZING
    )

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