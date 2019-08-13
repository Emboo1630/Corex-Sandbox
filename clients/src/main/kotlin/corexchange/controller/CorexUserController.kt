package corexchange.controller

import corexchange.webserver.NodeRPCConnection

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.SerializationFeature
import corexchange.models.CorexMoveModel
import corexchange.userflows.UserRegisterFlow
import corexchange.models.CorexRegisterModel
import corexchange.models.CorexUserModel
import corexchange.states.UserState
import corexchange.userflows.MoveTokensFlow
import corexchange.webserver.utilities.FlowHandlerCompletion
import corexchange.webserver.utilities.Plugin
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("corex")
class CorexUserController(rpc: NodeRPCConnection, private val flowHandlerCompletion: FlowHandlerCompletion, private val plugin: Plugin) {
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
            val infoStateRef = proxy.vaultQueryBy<UserState>().states
            val infoStates = infoStateRef.map { it.state.data }
            val list = infoStates.map {
                CorexUserModel(
                        name = it.name,
                        wallet = it.wallet,
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
     * Register a user account
     */

    @PostMapping(value = ["user/register"], produces = ["application/json"])
    @JsonIgnoreProperties(ignoreUnknown = true)
    private fun registerUser(@RequestBody registerModel: CorexRegisterModel): ResponseEntity<Map<String, Any>> {
        plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status, result) = try {
            val register = CorexRegisterModel(
                    name = registerModel.name,
                    wallet = registerModel.wallet
            )
            val flowReturn = proxy.startFlowDynamic(
                    UserRegisterFlow::class.java,
                    register.name,
                    register.wallet
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to registerModel
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
     * Register a user account
     */

    @PostMapping(value = ["user/move"], produces = ["application/json"])
    @JsonIgnoreProperties(ignoreUnknown = true)
    private fun corexMoveModel(@RequestBody corexMoveModel: CorexMoveModel): ResponseEntity<Map<String, Any>> {
        plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status, result) = try {
            val register = CorexMoveModel(
                    senderId = corexMoveModel.senderId,
                    receiverId = corexMoveModel.receiverId,
                    amount = corexMoveModel.amount,
                    currency = corexMoveModel.currency
            )
            val flowReturn = proxy.startFlowDynamic(
                    MoveTokensFlow::class.java,
                    register.senderId,
                    register.receiverId,
                    register.amount,
                    register.currency
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to corexMoveModel
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