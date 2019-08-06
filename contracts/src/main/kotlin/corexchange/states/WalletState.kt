package corexchange.states

import com.r3.corda.lib.tokens.contracts.states.AbstractToken
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import corexchange.contracts.WalletContract
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

@BelongsToContract(WalletContract::class)
data class WalletState(val amount: Amount<IssuedTokenType>,
                       override val holder: AbstractParty): AbstractToken
{
    override fun withNewHolder(newHolder: AbstractParty): AbstractToken
    {
        return FungibleToken(amount, newHolder, tokenTypeJarHash)
    }
    override val issuedTokenType: IssuedTokenType get() = amount.token
    override val tokenTypeJarHash: SecureHash? get() = amount.token.tokenType.getAttachmentIdForGenericParam()
}