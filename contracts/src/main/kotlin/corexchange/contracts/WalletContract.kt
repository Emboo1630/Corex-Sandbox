package corexchange.contracts

import com.r3.corda.lib.tokens.contracts.AbstractTokenContract
import com.r3.corda.lib.tokens.contracts.FungibleTokenContract
import com.r3.corda.lib.tokens.contracts.commands.TokenCommand
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import net.corda.core.contracts.Attachment
import net.corda.core.contracts.CommandWithParties
import net.corda.core.contracts.Contract

class WalletContract : FungibleTokenContract(), Contract
{
    companion object
    {
        val walletId = "corexchange.contracts.WalletContract"
    }
}