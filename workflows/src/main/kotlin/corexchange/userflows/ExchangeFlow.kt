package corexchange.userflows

import co.paralleluniverse.fibers.Suspendable
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import corexchange.contracts.SampleContract
import corexchange.states.SampleState
import corexchange.states.UserState
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

@StartableByRPC
class ExchangeFlow : UserFunctions()
{
    @Suspendable
    override fun call(): SignedTransaction
    {
        val signedTransaction = verifyAndSign(builder())
        return subFlow(FinalityFlow(signedTransaction, listOf()))
    }

    // Wallet of User
    private fun sample(): SampleState
    {
        // External Data
        val httpclient = HttpClientBuilder.create().build()
        val request = HttpGet("https://api.exchangeratesapi.io/latest?base=USD&symbols=PHP,USD")
        val response = httpclient.execute(request)
        val inputStreamReader = InputStreamReader(response.entity.content)

        val bufferedReader = BufferedReader(inputStreamReader).use {
            val stringBuff = StringBuffer()
            var inputLine = it.readLine()
            while (inputLine != null) {
                stringBuff.append(inputLine)
                inputLine = it.readLine()
            }

            val gson = GsonBuilder().create()
            val jsonWholeObject = gson.fromJson(stringBuff.toString(), JsonObject::class.java)
            val rates = jsonWholeObject.get("rates").asJsonObject
            val x = rates.get("USD")
            val y = rates.get("PHP")
            return SampleState(
                    usd = x.asString,
                    php = y.asString,
                    linearId = UniqueIdentifier(),
                    participants = listOf(ourIdentity)
            )
        }
    }

    private fun builder() = TransactionBuilder(notary = getPreferredNotary(serviceHub)).apply {
        addOutputState(sample(), SampleContract.sample_id)
        addCommand(Command(SampleContract.Commands.SampleCommand(), listOf(ourIdentity.owningKey)))
    }
}