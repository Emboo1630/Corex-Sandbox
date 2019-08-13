package corexchange.models

import com.fasterxml.jackson.annotation.JsonCreator
import net.corda.core.identity.Party

data class CorexOrderModel(
        val amount: Long,
        val currency: String,
        val issuer: Party
)

data class CorexOrderFlowModel @JsonCreator constructor(
        val amount: Long,
        val currency: String
)