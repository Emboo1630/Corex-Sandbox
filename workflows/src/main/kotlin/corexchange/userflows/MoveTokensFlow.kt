package corexchange.userflows

import co.paralleluniverse.fibers.Suspendable
import corexchange.contracts.UserContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class MoveTokensFlow (val senderId: UniqueIdentifier,
                     val receiverId: UniqueIdentifier,
                     val currency: String,
                     val amount: Long): UserFunctions() {
    @Suspendable
    override fun call(): SignedTransaction {
        val transaction = transaction()
        val verify = verifyAndSign(transaction)
        val transactionSignedByBothParties: SignedTransaction = verify
        return recordUpdate(transactionSignedByBothParties, listOf())
    }

    private fun transaction(): TransactionBuilder {
        val sender = queryState(linearId = senderId).state.data
        val receiver = queryState(linearId = receiverId).state.data
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val command = Command(UserContract.Commands.Register(), ourIdentity.owningKey)
        val newSenderWallet = updatesenderWallet(senderId, currency, amount)
                .sortedBy { it.token.tokenIdentifier }.toMutableList()
        val newReceiverWallet = updatereceiverWallet(receiverId, currency, amount)
                .sortedBy { it.token.tokenIdentifier }.toMutableList()
        val builder = TransactionBuilder(notary = notary)
                .addCommand(command)
                .addOutputState(sender.copy(wallet = newSenderWallet), contract = UserContract.CONTRACT_ID)
                .addOutputState(receiver.copy(wallet = newReceiverWallet), contract = UserContract.CONTRACT_ID)
                .addInputState(queryState(receiverId))
                .addInputState(queryState(senderId))
        return builder
    }
}