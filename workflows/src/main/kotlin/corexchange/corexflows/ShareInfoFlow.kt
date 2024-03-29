//package corexchange.corexflows
//
//import co.paralleluniverse.fibers.Suspendable
//import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
//import corexchange.*
//import corexchange.contracts.UserContract
//import corexchange.states.UserState
//import net.corda.core.contracts.Command
//import net.corda.core.flows.*
//import net.corda.core.node.services.*
//import net.corda.core.node.services.vault.QueryCriteria
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//
//@InitiatingFlow
//@StartableByRPC
//class ShareInfoFlow (private val recipient: String): CorexFunctions()
//{
//    @Suspendable
//    override fun call(): SignedTransaction
//    {
//        /**
//         * Minor bug on sharing info when a state in UserState
//         * is used in another transaction, it will throw a FlowException.
//         */
////        val listOfInputs = serviceHub.vaultService.queryBy<UserState>().states
////        for (x in listOfInputs)
////        {
////            subFlow(NotaryChangeFlow(x, stringToParty("Notary2")))
////        }
//
//        progressTracker.currentStep = CREATING
//        progressTracker.currentStep = VERIFYING
//        progressTracker.currentStep = SIGNING
//        val signedTransaction = verifyAndSign(shareInfo())
//        val session = initiateFlow(stringToParty(recipient))
//        val transactionSignedByParties = subFlow(CollectSignaturesFlow(signedTransaction, listOf(session)))
//
//        progressTracker.currentStep = NOTARIZING
//        progressTracker.currentStep = FINALIZING
//        return subFlow(FinalityFlow(transactionSignedByParties, listOf(session)))
//    }
//
//    @Suspendable
//    private fun shareInfo() = TransactionBuilder(notary = serviceHub.networkMapCache.getNotary(stringToNotary("O=Notary2,L=New York,C=US"))).apply {
//        val criteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.UNCONSUMED)
//        val listOfInputs = serviceHub.vaultService.queryBy<UserState>().states.map {
//            subFlow(NotaryChangeFlow(it, stringToParty("Notary2")))
//        }
//        val listOfOutputs = serviceHub.vaultService.queryBy<UserState>(criteria).states.map {
//            it.state.data
//        }
//
//        // List of user input states
//        for (itx in listOfInputs)
//        {
//            addInputState(itx)
//        }
//
//        // List of user states
//        for (ity in listOfOutputs)
//        {
//            addOutputState(ity.copy(participants = listOf(ourIdentity, stringToParty(recipient))), UserContract.CONTRACT_ID)
//        }
//
//        addCommand(Command(UserContract.Commands.Move(), listOf(ourIdentity.owningKey, stringToParty(recipient).owningKey)))
//    }
//}
//
//@InitiatedBy(ShareInfoFlow::class)
//class ShareInfoFlowResponder(private val flowSession: FlowSession): FlowLogic<SignedTransaction>()
//{
//    @Suspendable
//    override fun call(): SignedTransaction
//    {
//        subFlow(object : SignTransactionFlow(flowSession)
//        {
//            override fun checkTransaction(stx: SignedTransaction)
//            {
//            }
//        })
//        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession))
//    }
//}