package corexchange.states

import corexchange.contracts.PreOrderContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(PreOrderContract::class)
data class PreOrderState (val amount: Long,
                          val currency: String,
                          override val linearId: UniqueIdentifier,
                          override val participants: List<Party>): LinearState