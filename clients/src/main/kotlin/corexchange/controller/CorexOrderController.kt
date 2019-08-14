package corexchange.controller

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.SerializationFeature
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import corexchange.issuerflows.CorexOrderFlow
import corexchange.issuerflows.IssueTokensFlow
import corexchange.models.*
import corexchange.states.OrderState
import corexchange.states.PreOrderState
import corexchange.userflows.PreOrderFlow
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
class CorexOrderController(rpc: NodeRPCConnection, private val flowHandlerCompletion: FlowHandlerCompletion, private val plugin: Plugin) {
    companion object
    {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    /**
     * vault for OrderStates
     */
    @GetMapping(value = ["order/getOrderStates"], produces = ["application/json"])
    private fun corexGetOrderStates(): ResponseEntity<Map<String, Any>>
    {
        plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status, result) = try {
            val infoStateRef = proxy.vaultQueryBy<OrderState>().states
            val infoStates = infoStateRef.map { it.state.data }
            val list = infoStates.map {
                CorexOrderModel(
                        amount = it.amount,
                        currency = it.currency,
                        issuer = it.issuer,
                        linearId = it.linearId.toString()
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
     * FungibleToken States
     */
    @GetMapping(value = ["order/getFungible"], produces = ["application/json"])
    private fun corexGetFungibleStates(): ResponseEntity<Map<String, Any>>
    {
        plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status, result) = try {
            val infoStateRef = proxy.vaultQueryBy<FungibleToken>().states
            val infoStates = infoStateRef.map { it.state.data }
            val list = infoStates.map {
                CorexFungibleTokenModel(
                        amount = it.amount.toString(),
                        holder = it.holder.toString()
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
     * Order to OrderState
     */
    @PostMapping(value = ["order/orderFungible"], produces = ["application/json"])
    @JsonIgnoreProperties(ignoreUnknown = true)
    private fun corexOrderFungible(@RequestBody corexOrderFlowModel: CorexOrderFlowModel): ResponseEntity<Map<String, Any>> {
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

    /**
     * Issue for OrderState
     */
    @PostMapping(value = ["order/issueFungible"], produces = ["application/json"])
    @JsonIgnoreProperties(ignoreUnknown = true)
    private fun corexIssueFungible(@RequestBody corexIssueModel: CorexIssueModel): ResponseEntity<Map<String, Any>> {
        plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status, result) = try {
            val register = CorexIssueModel(
                    recipient = corexIssueModel.recipient,
                    orderId = corexIssueModel.orderId
            )
            val flowReturn = proxy.startFlowDynamic(
                    IssueTokensFlow::class.java,
                    register.recipient,
                    register.orderId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to corexIssueModel
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

    /**
     * Vault for PreOrderState
     */
    @GetMapping(value = ["order/getPreOrder"], produces = ["application/json"])
    private fun corexGetPreOrderState(): ResponseEntity<Map<String, Any>>
    {
        plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status, result) = try {
            val infoStateRef = proxy.vaultQueryBy<PreOrderState>().states
            val infoStates = infoStateRef.map { it.state.data }
            val list = infoStates.map {
                CorexPreOrderModel(
                        amount = it.amount,
                        currency = it.currency,
                        linearId = it.linearId.toString()
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
     * PreOrderFlow at PreOrderState
     */
    @PostMapping(value = ["order/preOrder"],produces = ["application/json"])
    @JsonIgnoreProperties(ignoreUnknown = true)
    private fun corexPreOrder(@RequestBody corexPreOrderRegModel: CorexPreOrderRegModel):ResponseEntity<Map<String,Any>>
    {plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status,result) = try {
            val preOrder = CorexPreOrderRegModel(
                    amount = corexPreOrderRegModel.amount,
                    currency = corexPreOrderRegModel.currency

            )
            val flowReturn = proxy.startFlowDynamic(
                    PreOrderFlow::class.java,
                    preOrder.amount,
                    preOrder.currency

            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to corexPreOrderRegModel
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