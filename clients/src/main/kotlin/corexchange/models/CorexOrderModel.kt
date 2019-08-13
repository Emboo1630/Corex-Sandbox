package corexchange.models

import com.fasterxml.jackson.annotation.JsonCreator
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

data class CorexOrderModel(
        val amount: Long,
        val currency: String,
        val issuer: Party,
        val linearId: UniqueIdentifier
)

data class CorexOrderFlowModel @JsonCreator constructor(
        val amount: Long,
        val currency: String
)

data class CorexIssueModel @JsonCreator constructor(
        val recipient: String,
        val orderId: String
)
