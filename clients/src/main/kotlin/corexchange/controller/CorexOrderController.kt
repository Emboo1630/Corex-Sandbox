package corexchange.controller

import com.fasterxml.jackson.databind.SerializationFeature
import corexchange.issuerflows.CorexOrderFlow
import corexchange.issuerflows.VerifyOrderFlow
import corexchange.models.*
import corexchange.states.OrderState
import corexchange.states.ReserveOrderState
import corexchange.userflows.ReserveTokensFlow
import corexchange.webserver.NodeRPCConnection
import corexchange.webserver.utilities.FlowHandlerCompletion
import corexchange.webserver.utilities.Plugin
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("corexorder")
class CorexOrderController(rpc: NodeRPCConnection, private val flowHandlerCompletion: FlowHandlerCompletion, private val plugin: Plugin)
{
    companion object
    {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    /**
     * Get all order states from platform
     */
    @GetMapping(value = ["get/order"], produces = ["application/json"])
    private fun getOrderStates(): ResponseEntity<Map<String, Any>>
    {
        plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status, result) = try {
            val infoStateRef = proxy.vaultQueryBy<OrderState>().states
            val infoStates = infoStateRef.map { it.state.data }
            val list = infoStates.map {
                CorexOrderModel(
                        amount = it.amount,
                        currency = it.currency,
                        status = it.status,
                        issuer = it.issuer.toString(),
                        linearId = it.linearId.toString()
                )
            }
            HttpStatus.CREATED to list
        }
        catch (e: Exception)
        {
            logger.info(e.message)
            HttpStatus.BAD_REQUEST to "No orders found."
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
     * Order fungible tokens from platform to issuer
     */
    @PostMapping(value = ["order/fungible"], produces = ["application/json"])
    private fun orderFungibleTokens(@RequestBody corexOrderFlowModel: CorexOrderFlowModel): ResponseEntity<Map<String, Any>>
    {
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
        } catch (e: Exception) {
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

    /**
     * Verify order of platform to issuer
     */
    @PostMapping(value = ["verify/order"], produces = ["application/json"])
    private fun verifyOrder(@RequestBody issuerVerifyOrderModel: IssuerVerifyOrderModel): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val verify = IssuerVerifyOrderModel(
                    linearId = issuerVerifyOrderModel.linearId
            )
            val flowReturn = proxy.startFlowDynamic(
                    VerifyOrderFlow::class.java,
                    verify.linearId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to issuerVerifyOrderModel
        } catch (e: Exception)
        {
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

    /**
     * Get all reserved tokens state from user
     */
    @GetMapping(value = ["get/reserve"], produces = ["application/json"])
    private fun getReserveTokens(): ResponseEntity<Map<String, Any>>
    {
        plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status, result) = try {
            val infoStateRef = proxy.vaultQueryBy<ReserveOrderState>().states
            val infoStates = infoStateRef.map { it.state.data }
            val list = infoStates.map {
                CorexReserveModel(
                        amount = it.amount,
                        currency = it.currency,
                        linearId = it.linearId.toString()
                )
            }
            HttpStatus.CREATED to list
        } catch (e: Exception)
        {
            logger.info(e.message)
            HttpStatus.BAD_REQUEST to "No pre-orders found."
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
     * Reserve tokens from user to platform
     */
    @PostMapping(value = ["order/preOrder"],produces = ["application/json"])
    private fun reserveTokens(@RequestBody corexReserveTokensModel: CorexReserveTokensModel):ResponseEntity<Map<String,Any>>
    {
        val (status,result) = try {
            val preOrder = CorexReserveTokensModel(
                    amount = corexReserveTokensModel.amount,
                    currency = corexReserveTokensModel.currency
            )
            val flowReturn = proxy.startFlowDynamic(
                    ReserveTokensFlow::class.java,
                    preOrder.amount,
                    preOrder.currency
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to corexReserveTokensModel
        } catch (e: Exception) {
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