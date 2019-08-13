package corexchange.models

import com.fasterxml.jackson.annotation.JsonCreator
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
        val wallet: List<Amount<TokenType>>
)

data class CorexMoveModel @JsonCreator constructor(
         val senderId: String,
         val receiverId: String,
         val amount: Long,
         val currency: String
)

data class CorexTransferTokenModel @JsonCreator constructor(
        val amount: Long,
        val walletRef: StateRef,
        val userId: String
)

data class CorexShareInfoModel @JsonCreator constructor(
        val recipient: String
)