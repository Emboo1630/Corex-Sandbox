package corexchange.models

import com.fasterxml.jackson.annotation.JsonCreator

data class CorexFungibleTokenModel(
        val amount: String,
        val holder: String
)

data class CorexIssueModel @JsonCreator constructor(
        val recipient: String,
        val orderId: String
)