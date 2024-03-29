package corexchange.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.FiatCurrency
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.UniqueIdentifier

data class CorexUserModel(
        val name: String,
        val wallet: List<Amount<TokenType>>,
        val linearId: String
)

data class CorexRegisterModel @JsonCreator constructor(
        val name: String,
        val amount: MutableList<Long>,
        val currency: MutableList<String>
)

data class CorexMoveTokensFromUserToUserModel @JsonCreator constructor(
         val senderId: String,
         val receiverId: String,
         val amount: Long,
         val currency: String
)

data class CorexTransferTokenModel @JsonCreator constructor(
        val preOrderId: String,
        val walletRef: String
)

data class CorexShareInfoModel @JsonCreator constructor(
        val recipient: String
)
