package corexchange.controller

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.SerializationFeature
import corexchange.issuerflows.CorexOrderFlow
import corexchange.models.CorexOrderFlowModel
import corexchange.models.CorexOrderModel
import corexchange.states.OrderState
import corexchange.webserver.NodeRPCConnection
import corexchange.webserver.utilities.FlowHandlerCompletion
import corexchange.webserver.utilities.Plugin
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("corexOrder")
class CorexController(rpc: NodeRPCConnection, private val flowHandlerCompletion: FlowHandlerCompletion, private val plugin: Plugin) {
    companion object
    {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    /**
     * Return all users
     */
    @GetMapping(value = ["users/all"], produces = ["application/json"])
    private fun getAllUsers(): ResponseEntity<Map<String, Any>>
    {
        plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status, result) = try {
            val infoStateRef = proxy.vaultQueryBy<OrderState>().states
            val infoStates = infoStateRef.map { it.state.data }
            val list = infoStates.map {
                CorexOrderModel(
                        amount = it.amount,
                        currency = it.currency,
                        issuer = it.issuer
                )
            }
            HttpStatus.CREATED to list
        }
        catch (e: Exception)
        {
            logger.info(e.message)
            HttpStatus.BAD_REQUEST to "No users found."
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

    /**
     * Register a user account
     */

    @PostMapping(value = ["user/register"], produces = ["application/json"])
    @JsonIgnoreProperties(ignoreUnknown = true)
    private fun corexOrderFlowModel(@RequestBody corexOrderFlowModel: CorexOrderFlowModel): ResponseEntity<Map<String, Any>> {
        plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status, result) = try {
            val register = CorexOrderFlowModel(
                    amount = corexOrderFlowModel.amount,
                    currency = corexOrderFlowModel.currency
            )
            val flowReturn = proxy.startFlowDynamic(
                    CorexOrderFlow::class.java,
                    register.amount,
                    register.currency
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to corexOrderFlowModel
        }
        catch (e: Exception) {
            HttpStatus.BAD_REQUEST to e
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

        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }
}