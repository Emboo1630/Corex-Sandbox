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
        val issuer: String,
        val status: String,
        val linearId: String
)

data class CorexOrderFlowModel @JsonCreator constructor(
        val amount: Long,
        val currency: String
)

data class CorexReserveModel @JsonCreator constructor(
        val amount: Long,
        val currency: String,
        val userId: String,
        val linearId: String
)

data class CorexReserveTokensModel @JsonCreator constructor(
        val amount: Long,
        val currency: String,
        val userId: String
)

data class IssuerVerifyOrderModel @JsonCreator constructor(
        val linearId: String
)

