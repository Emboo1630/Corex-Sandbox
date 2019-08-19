package corexchange.states

import corexchange.contracts.SampleContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(SampleContract::class)
class SampleState (val usd: String,
                   val php: String,
                   override val linearId: UniqueIdentifier,
                   override val participants: List<Party>): LinearState