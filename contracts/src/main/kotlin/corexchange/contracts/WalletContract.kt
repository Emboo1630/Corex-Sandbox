package corexchange.contracts

import com.r3.corda.lib.tokens.contracts.AbstractTokenContract
import com.r3.corda.lib.tokens.contracts.commands.IssueTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.MoveTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.RedeemTokenCommand
import com.r3.corda.lib.tokens.contracts.commands.TokenCommand
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import corexchange.states.WalletState
import net.corda.core.contracts.*
import net.corda.core.internal.uncheckedCast

class WalletContract : AbstractTokenContract<WalletState>(), Contract
{
    class UpdateTokenCommand (override val token: IssuedTokenType, val inputs: List<Int> = listOf(), val outputs: List<Int> = listOf()): TokenCommand(inputIndicies = inputs, outputIndicies = outputs, token = token)

    companion object
    {
        const val contractId = "corexchange.contracts.WalletContract"
    }

    override val accepts = uncheckedCast(WalletState::class.java)

    override fun verifyIssue(issueCommand: CommandWithParties<TokenCommand>, inputs: List<IndexedState<WalletState>>, outputs: List<IndexedState<WalletState>>, attachments: List<Attachment>) {
    }

    override fun verifyMove(moveCommands: List<CommandWithParties<TokenCommand>>, inputs: List<IndexedState<WalletState>>, outputs: List<IndexedState<WalletState>>, attachments: List<Attachment>) {
    }

    override fun verifyRedeem(redeemCommand: CommandWithParties<TokenCommand>, inputs: List<IndexedState<WalletState>>, outputs: List<IndexedState<WalletState>>, attachments: List<Attachment>) {
    }

    private fun verifyUpdate(updateCommand: CommandWithParties<TokenCommand>, inputs: List<IndexedState<WalletState>>, outputs: List<IndexedState<WalletState>>, attachments: List<Attachment>) {
    }

    override fun dispatchOnCommand(commands: List<CommandWithParties<TokenCommand>>, inputs: List<IndexedState<WalletState>>, outputs: List<IndexedState<WalletState>>, attachments: List<Attachment>) {
        super.dispatchOnCommand(commands, inputs, outputs, attachments)
        when (commands.first().value) {
            // Issuances should only contain one issue command.
            is IssueTokenCommand -> verifyIssue(commands.single(), inputs, outputs, attachments)
            // Moves may contain more than one move command.
            is MoveTokenCommand -> verifyMove(commands, inputs, outputs, attachments)
            // Redeems must only contain one redeem command.
            is RedeemTokenCommand -> verifyRedeem(commands.single(), inputs, outputs, attachments)
            // Transfer Command
            is UpdateTokenCommand -> verifyUpdate(commands.single(), inputs, outputs, attachments)
        }
    }
}