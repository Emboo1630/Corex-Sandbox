package corexchange.controller

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import corexchange.models.CorexExternalDataModel
import corexchange.webserver.NodeRPCConnection
import corexchange.webserver.utilities.FlowHandlerCompletion
import corexchange.webserver.utilities.Plugin
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.BufferedReader
import java.io.InputStreamReader

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping("externaldata")
class CorexExternalDataController(rpc: NodeRPCConnection, private val flowHandlerCompletion: FlowHandlerCompletion, private val plugin: Plugin)
{
    companion object
    {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    /**
     * Get external data from "https://api.exchangeratesapi.io/latest?base=USD&symbols=PHP,USD"
     */
    @GetMapping(value = ["get"], produces = ["application/json"])
    private fun getData(): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val httpclient = HttpClientBuilder.create().build()
            val request = HttpGet("https://api.exchangeratesapi.io/latest?base=USD&symbols=PHP,USD")
            val response = httpclient.execute(request)
            val inputStreamReader = InputStreamReader(response.entity.content)

            val list = BufferedReader(inputStreamReader).use {
                val stringBuff = StringBuffer()
                var inputLine = it.readLine()
                while (inputLine != null) {
                    stringBuff.append(inputLine)
                    inputLine = it.readLine()
                }

                val gson = GsonBuilder().create()
                val jsonWholeObject = gson.fromJson(stringBuff.toString(), JsonObject::class.java)
                val rates = jsonWholeObject.get("rates").asJsonObject
                val date = jsonWholeObject.get("date").asString

                CorexExternalDataModel(
                        usd = rates.get("USD").asDouble,
                        php = rates.get("PHP").asDouble,
                        date = date.toString()
                )
            }
            HttpStatus.CREATED to list
        } catch (e: Exception)
        {
            logger.info(e.message)
            HttpStatus.BAD_REQUEST to "No external data found."
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED)
        {
            "message" to "Successful"
        }
        else
        {
            "message" to "Failed"
        }

        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat,mess,res))
    }
}