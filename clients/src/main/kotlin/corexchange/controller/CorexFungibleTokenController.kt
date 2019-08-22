package corexchange.controller

import com.fasterxml.jackson.databind.SerializationFeature
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import corexchange.issuerflows.MoveTokensToPlatformFlow
import corexchange.issuerflows.SelfIssueTokensFlow
import corexchange.models.*
import corexchange.webserver.NodeRPCConnection
import corexchange.webserver.utilities.FlowHandlerCompletion
import corexchange.webserver.utilities.Plugin
import net.corda.core.contracts.hash
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
//        plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status, result) = try {
            val infoStateRef = proxy.vaultQueryBy<FungibleToken>().states
            val infoStates = infoStateRef.map { it }
            val list = infoStates.map {
                    CorexFungibleTokenModel(
                        amount = it.state.data.amount.toString(),
                        holder = it.state.data.holder.toString(),
                        hash = it.ref.txhash.toString()
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
     * Self Issue Tokens on a Issuer Node
     */
    @PostMapping(value = ["selfissue/fungible"], produces = ["application/json"])
    private fun selfIssueFungibleTokens(@RequestBody corexSelfIssueModel: CorexSelfIssueModel): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val selfIssue = CorexSelfIssueModel(
                    amount = corexSelfIssueModel.amount,
                    currency = corexSelfIssueModel.currency
            )
            val flowReturn = proxy.startFlowDynamic(
                    SelfIssueTokensFlow::class.java,
                    selfIssue.amount,
                    selfIssue.currency
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to corexSelfIssueModel
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
     * Move fungible tokens from issuer to platform
     */
    @PostMapping(value = ["move/fungible"], produces = ["application/json"])
    private fun moveFungibleTokens(@RequestBody corexMoveModel: CorexMoveFungibleTokensModel): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val register = CorexMoveFungibleTokensModel(
                    recipient = corexMoveModel.recipient,
                    orderId = corexMoveModel.orderId
            )
            val flowReturn = proxy.startFlowDynamic(
                    MoveTokensToPlatformFlow::class.java,
                    register.recipient,
                    register.orderId
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to corexMoveModel
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