package corexchange.controller

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.SerializationFeature
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import corexchange.issuerflows.IssueTokensFlow
import corexchange.models.CorexFungibleTokenModel
import corexchange.models.CorexIssueModel
import corexchange.models.CorexOrderModel
import corexchange.states.OrderState
import corexchange.webserver.NodeRPCConnection
import corexchange.webserver.utilities.FlowHandlerCompletion
import corexchange.webserver.utilities.Plugin
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("corextoken")
class CorexFungibleTokenController(rpc: NodeRPCConnection, private val flowHandlerCompletion: FlowHandlerCompletion, private val plugin: Plugin)
{
    companion object
    {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    /**
     * Get all corex fungible tokens
     */
    @GetMapping(value = ["get/fungible"], produces = ["application/json"])
    private fun getFungibleTokens(): ResponseEntity<Map<String, Any>>
    {
        plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status, result) = try {
            val infoStateRef = proxy.vaultQueryBy<FungibleToken>().states
            val infoStates = infoStateRef.map { it.state.data }
            val infoRef = infoStateRef.map { it.ref.txhash }.single()
            val list = infoStates.map {
                CorexFungibleTokenModel(
                        amount = it.amount.toString(),
                        holder = it.holder.toString(),
                        hash = infoRef.toString()
                )
            }
            HttpStatus.CREATED to list
        } catch (e: Exception)
        {
            logger.info(e.message)
            HttpStatus.BAD_REQUEST to "No fungible tokens found."
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
     * Issue fungible tokens from issuer to platform
     */
    @PostMapping(value = ["issue/fungible"], produces = ["application/json"])
    @JsonIgnoreProperties(ignoreUnknown = true)
    private fun issueFungible(@RequestBody corexIssueModel: CorexIssueModel): ResponseEntity<Map<String, Any>>
    {
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
}