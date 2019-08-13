package corexchange.states

import corexchange.contracts.OrderContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(OrderContract::class)
data class OrderState (val amount: Long,
                      val currency: String,
                      val issuer: Party,
                      override val linearId: UniqueIdentifier,
                      override val participants: List<Party>): LinearState