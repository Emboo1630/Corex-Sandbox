package corexchange.states

import corexchange.contracts.ReserveOrderContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(ReserveOrderContract::class)
data class ReserveOrderState (val amount: Long,
                              val currency: String,
<<<<<<< HEAD
=======
                              val owner: UniqueIdentifier,
>>>>>>> 18a88501047964343a90eb35ad761d11c5a5ae83
                              override val linearId: UniqueIdentifier,
                              override val participants: List<Party>): LinearState