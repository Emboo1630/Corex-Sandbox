package corexchange.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

data class CorexOrderModel(
        val amount: Long,
        val currency: String,
        val issuer: Party,
        val linearId: String
)

data class CorexFungibleTokenModel(
        val amount: String,
        val holder: String
)

data class CorexOrderFlowModel @JsonCreator constructor(
        val amount: Long,
        val currency: String
)

data class CorexIssueModel @JsonCreator constructor(
        val recipient: String,
        val orderId: String
)

data class CorexPreOrderModel @JsonCreator constructor(
        val amount: Long,
        val currency: String,
        val linearId: String
)

data class CorexPreOrderRegModel @JsonCreator constructor(
        val amount: Long,
        val currency: String
)

