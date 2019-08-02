package corexchange.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.r3.corda.lib.tokens.contracts.types.TokenType
import net.corda.core.contracts.Amount

data class CorexUserModel(
        val name: String,
        val wallet: List<Amount<TokenType>>
)

data class CorexRegisterModel @JsonCreator constructor(
        val name: String,
        val wallet: List<Amount<TokenType>>
)

//data class