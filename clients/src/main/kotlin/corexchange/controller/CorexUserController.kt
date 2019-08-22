package corexchange.controller

import com.fasterxml.jackson.databind.SerializationFeature
import corexchange.corexflows.TransferTokensToUserFlow
import corexchange.models.*
import corexchange.states.UserState
import corexchange.userflows.MoveTokensFromUserToUserFlow
import corexchange.userflows.UserRegisterFlow
import corexchange.webserver.NodeRPCConnection
import corexchange.webserver.utilities.FlowHandlerCompletion
import corexchange.webserver.utilities.Plugin
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("corex")
class CorexUserController(rpc: NodeRPCConnection, private val flowHandlerCompletion: FlowHandlerCompletion, private val plugin: Plugin)
{
    companion object
    {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy


    /**
     * Get all users on a platform
     */
    @GetMapping(value = ["get/users"], produces = ["application/json"])
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
            HttpStatus.BAD_REQUEST to "No corex users found."
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
     * Registration of user with wallet
     */
    @PostMapping(value = ["user/registration"], produces = ["application/json"])
    private fun registerUser(@RequestBody registerModel: CorexRegisterModel): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val register = CorexRegisterModel(
                    name = registerModel.name,
                    amount = registerModel.amount,
                    currency = registerModel.currency
            )
            val flowReturn = proxy.startFlowDynamic(
                    UserRegisterFlow::class.java,
                    register.name,
                    register.amount,
                    register.currency
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to registerModel
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
     * Move tokens from user to another user
     */
    @PostMapping(value = ["user/move"], produces = ["application/json"])
    private fun moveTokens(@RequestBody corexMoveModel: CorexMoveTokensFromUserToUserModel): ResponseEntity<Map<String, Any>>
    {
        val (status, result) = try {
            val register = CorexMoveTokensFromUserToUserModel(
                    senderId = corexMoveModel.senderId,
                    receiverId = corexMoveModel.receiverId,
                    amount = corexMoveModel.amount,
                    currency = corexMoveModel.currency
            )
            val flowReturn = proxy.startFlowDynamic(
                    MoveTokensFromUserToUserFlow::class.java,
                    register.senderId,
                    register.receiverId,
                    register.amount,
                    register.currency
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to corexMoveModel
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
     * Share info from platform to an issuer
     */
//    @PostMapping(value = ["share/info"], produces = ["application/json"])
//    private fun corexShareInfo(@RequestBody shareInfoModel: CorexShareInfoModel): ResponseEntity<Map<String,Any>>
//    {
//        val (status, result) = try {
//            val share = CorexShareInfoModel(
//                    recipient = shareInfoModel.recipient
//            )
//            val flowReturn = proxy.startFlowDynamic(
//                    ShareInfoFlow::class.java,
//                    share.recipient
//            )
//            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
//            HttpStatus.CREATED to shareInfoModel
//        } catch (e: Exception) {
//            HttpStatus.BAD_REQUEST to e
//        }
//        val stat = "status" to status
//        val mess = if (status == HttpStatus.CREATED)
//        {
//            "message" to "Successful"
//        }
//        else
//        {
//            "message" to "Failed"
//        }
//
//        val res = "result" to result
//        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
//    }

    /**
     * Transfer tokens from platform to user, remove pre-order states and update corex wallet
     */
    @PostMapping(value = ["platform/transfer"],produces = ["application/json"])
    private fun transferTokens(@RequestBody transferTokenModel: CorexTransferTokenModel):ResponseEntity<Map<String,Any>>
    {
        plugin.registerModule().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        val (status,result) = try {
            val transfer = CorexTransferTokenModel(
                    transferTokenModel.preOrderId,
                    transferTokenModel.walletRef
            )
            val flowReturn = proxy.startFlowDynamic(
                    TransferTokensToUserFlow::class.java,
                    transfer.preOrderId,
                    transfer.walletRef
            )
            flowHandlerCompletion.flowHandlerCompletion(flowReturn)
            HttpStatus.CREATED to transferTokenModel
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