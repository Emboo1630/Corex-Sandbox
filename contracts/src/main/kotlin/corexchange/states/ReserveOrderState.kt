package corexchange.states

import corexchange.contracts.ReserveOrderContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(ReserveOrderContract::class)
data class ReserveOrderState (val amount: Long,
                              val currency: String,
                              val owner: UniqueIdentifier,
                              override val linearId: UniqueIdentifier,
                              override val participants: List<Party>): LinearState