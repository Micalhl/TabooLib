package taboolib.module.nms

import net.minecraft.server.v1_16_R3.*
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import taboolib.common.reflect.Reflex
import taboolib.common.reflect.Reflex.Companion.reflex
import taboolib.common.reflect.Reflex.Companion.unsafeInstance
import java.util.*

class NMSScoreboardImpl : NMSScoreboard() {

    override fun setupScoreboard(player: Player, remove: Boolean) {
        val packet = PacketPlayOutScoreboardObjective::class.java.unsafeInstance()
        val reflex = Reflex(PacketPlayOutScoreboardObjective::class.java).instance(packet)
        if (MinecraftVersion.isUniversal) {
            reflex.set("objectiveName", if (remove) "REMOVE" else "TabooScore")
            reflex.set("displayName", ChatComponentText("ScoreBoard"))
            reflex.set("renderType", IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER)
            reflex.set("method", 0)
        } else {
            reflex.set("a", if (remove) "REMOVE" else "TabooScore")
            if (MinecraftVersion.major >= 5) {
                reflex.set("b", ChatComponentText("ScoreBoard"))
            } else {
                reflex.set("b", "ScoreBoard")
            }
            reflex.set("c", IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER)
            reflex.set("d", 0)
        }
        player.sendPacket(packet)
        initTeam(player)
    }

    override fun changeContent(player: Player, content: List<String>, lastContent: Map<Int, String>) {
        if (content.size != lastContent.size) {
            updateLineCount(player, content.size, lastContent.size)
        }
        content.forEachIndexed { line, ct ->
            if (ct != lastContent[content.size - line - 1]) {
                sendTeamPrefixSuffix(player, uniqueColors[content.size - line - 1], ct)
            }
        }
    }

    override fun display(player: Player) {
        val packet = PacketPlayOutScoreboardDisplayObjective::class.java.unsafeInstance()
        val reflex = Reflex(PacketPlayOutScoreboardDisplayObjective::class.java).instance(packet)
        if (MinecraftVersion.isUniversal) {
            reflex.set("slot", 1)
            reflex.set("objectiveName", "TabooScore")
        } else {
            reflex.set("a", 1)
            reflex.set("b", "TabooScore")
        }
        player.sendPacket(packet)
    }

    override fun setDisplayName(player: Player, title: String) {
        val packet = PacketPlayOutScoreboardObjective::class.java.unsafeInstance()
        val reflex = Reflex(PacketPlayOutScoreboardObjective::class.java).instance(packet)
        if (MinecraftVersion.isUniversal) {
            reflex.set("objectiveName", "TabooScore")
            reflex.set("displayName", ChatComponentText(title))
            reflex.set("renderType", IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER)
            reflex.set("method", 2)
        } else {
            reflex.set("a", "TabooScore")
            if (MinecraftVersion.major >= 5) {
                reflex.set("b", ChatComponentText(title))
            } else {
                reflex.set("b", title)
            }
            reflex.set("c", IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER)
            reflex.set("d", 2)
        }
        player.sendPacket(packet)
    }

    /**
     *
     * a -> Team Name
     * b -> Team Display Name
     * c -> Team Prefix
     * d -> Team Suffix
     * e -> Name Tag Visibility
     * f -> Color
     * g -> Players, Player Count
     * h -> Mode
     *
     *  If 0 then the team is created.
     *  If 1 then the team is removed.
     *  If 2 the team team information is updated.
     *  If 3 then new players are added to the team.
     *  If 4 then players are removed from the team.
     *
     * i -> Friendly Fire
     *
     * @see EnumChatFormat
     * @see PacketPlayOutScoreboardTeam
     */
    private fun initTeam(player: Player) {
        uniqueColors.forEachIndexed { _, color ->
            if (MinecraftVersion.major >= 9) {
                val packet = PacketPlayOutScoreboardTeam::class.java.unsafeInstance()
                packet.reflex("method", 0)
                packet.reflex("name", color)
                packet.reflex("players", listOf(color))
                val b = universalTeamData.unsafeInstance()
                b.reflex("displayName", ChatComponentText(color))
                b.reflex("nametagVisibility", "always")
                b.reflex("collisionRule", "always")
                b.reflex("color", EnumChatFormat.RESET)
                b.reflex("options", -1)
                packet.reflex("parameters", Optional.of(b))
                return@forEachIndexed
            }
            if (MinecraftVersion.major >= 5) {
                val packet = PacketPlayOutScoreboardTeam::class.java.unsafeInstance()
                val reflex = Reflex(PacketPlayOutScoreboardTeam::class.java).instance(packet)
                reflex.set("a", color)
                reflex.set("b", ChatComponentText(color))
                reflex.set("e", ScoreboardTeamBase.EnumNameTagVisibility.ALWAYS.e)
                reflex.set("f", ScoreboardTeamBase.EnumTeamPush.ALWAYS.e)
                reflex.set("g", EnumChatFormat.RESET)
                reflex.set("h", listOf(color))
                reflex.set("i", 0)
                reflex.set("j", -1)
                player.sendPacket(packet)
                return@forEachIndexed
            }
            val packet = PacketPlayOutScoreboardTeam::class.java.unsafeInstance()
            val reflex = Reflex(PacketPlayOutScoreboardTeam::class.java).instance(packet)
            reflex.set("a", color)
            reflex.set("b", color)
            reflex.set("e", ScoreboardTeamBase.EnumNameTagVisibility.ALWAYS.e)
            // Collections$SingletonList cannot be cast to java.lang.Number
            if (MinecraftVersion.major >= 1) {
                reflex.set("f", "always")
                reflex.set("g", -1)
                reflex.set("h", listOf(color))
                reflex.set("i", 0)
            } else {
                reflex.set("f", -1)
                reflex.set("g", listOf(color))
                reflex.set("h", 0)
            }
            player.sendPacket(packet)
        }
    }

    private fun validateLineCount(line: Int) {
        if (uniqueColors.size < line) {
            throw IllegalArgumentException("Lines size are larger than supported.")
        }
    }

    /**
     * @param team 为\[content.size - line - 1\]
     */
    private fun sendTeamPrefixSuffix(player: Player, team: String, content: String) {
        if (MinecraftVersion.major >= 9) {
            val packet = PacketPlayOutScoreboardTeam::class.java.unsafeInstance()
            packet.reflex("method", 2)
            packet.reflex("name", team)
            val b = universalTeamData.unsafeInstance()
            b.reflex("displayName", ChatComponentText(team))
            b.reflex("playerPrefix", ChatComponentText(content))
            b.reflex("nametagVisibility", "always")
            b.reflex("collisionRule", "always")
            b.reflex("color", EnumChatFormat.RESET)
            b.reflex("options", -1)
            packet.reflex("parameters", Optional.of(b))
            player.sendPacket(packet)
            return
        }
        if (MinecraftVersion.major >= 5) {
            val packet = PacketPlayOutScoreboardTeam::class.java.unsafeInstance()
            val reflex = Reflex(PacketPlayOutScoreboardTeam::class.java).instance(packet)
            reflex.set("a", team)
            reflex.set("c", ChatComponentText(content))
            reflex.set("i", 2)
            player.sendPacket(packet)
            return
        }
        var prefix = content
        var suffix = ""
        if (content.length > 16) {
            prefix = content.substring(0 until 16)
            val color = ChatColor.getLastColors(prefix)
            suffix = color + content.substring(16 until content.length)
            if (suffix.length > 16) {
                suffix = suffix.substring(0, 16)
            }
        }
        val packet = PacketPlayOutScoreboardTeam::class.java.unsafeInstance()
        val reflex = Reflex(PacketPlayOutScoreboardTeam::class.java).instance(packet)
        reflex.set("a", team)
        reflex.set(if (MinecraftVersion.major >= 1) "i" else "h", 2)
        reflex.set("c", prefix)
        reflex.set("d", suffix)
        player.sendPacket(packet)
    }

    private fun updateLineCount(player: Player, line: Int, lastLineCount: Int) {
        validateLineCount(line)
        if (line > lastLineCount) {
            (lastLineCount until line).forEach { i ->
                if (MinecraftVersion.major >= 9) {
                    val packet = PacketPlayOutScoreboardScore::class.java.unsafeInstance()
                    val reflex = Reflex(PacketPlayOutScoreboardScore::class.java).instance(packet)
                    reflex.set("owner", uniqueColors[i])
                    reflex.set("objectiveName", "TabooScore")
                    reflex.set("score", i)
                    reflex.set("method", ScoreboardServer.Action.CHANGE)
                    player.sendPacket(packet)
                    return@forEach
                }
                if (MinecraftVersion.major >= 5) {
                    val packet = PacketPlayOutScoreboardScore::class.java.unsafeInstance()
                    val reflex = Reflex(PacketPlayOutScoreboardScore::class.java).instance(packet)
                    reflex.set("a", uniqueColors[i])
                    reflex.set("b", "TabooScore")
                    reflex.set("c", i)
                    reflex.set("d", ScoreboardServer.Action.CHANGE)
                    player.sendPacket(packet)
                    return@forEach
                }
                val packet = PacketPlayOutScoreboardScore::class.java.unsafeInstance()
                val reflex = Reflex(PacketPlayOutScoreboardScore::class.java).instance(packet)
                reflex.set("a", uniqueColors[i])
                reflex.set("b", "TabooScore")
                reflex.set("c", i)
                reflex.set("d", net.minecraft.server.v1_12_R1.PacketPlayOutScoreboardScore.EnumScoreboardAction.CHANGE)
                player.sendPacket(packet)
            }
        } else {
            (line until lastLineCount).forEach { i ->
                if (MinecraftVersion.major >= 9) {
                    val packet = PacketPlayOutScoreboardScore::class.java.unsafeInstance()
                    val reflex = Reflex(PacketPlayOutScoreboardScore::class.java).instance(packet)
                    reflex.set("owner", uniqueColors[i])
                    reflex.set("objectiveName", "TabooScore")
                    reflex.set("method", ScoreboardServer.Action.REMOVE)
                    player.sendPacket(packet)
                    return@forEach
                }
                if (MinecraftVersion.major >= 5) {
                    val packet = PacketPlayOutScoreboardScore::class.java.unsafeInstance()
                    val reflex = Reflex(PacketPlayOutScoreboardScore::class.java).instance(packet)
                    reflex.set("a", uniqueColors[i])
                    reflex.set("b", "TabooScore")
                    reflex.set("d", ScoreboardServer.Action.REMOVE)
                    player.sendPacket(packet)
                    return@forEach
                }
                val packet = PacketPlayOutScoreboardScore::class.java.unsafeInstance()
                val reflex = Reflex(PacketPlayOutScoreboardScore::class.java).instance(packet)
                reflex.set("a", uniqueColors[i])
                reflex.set("b", "TabooScore")
                reflex.set("d", net.minecraft.server.v1_12_R1.PacketPlayOutScoreboardScore.EnumScoreboardAction.REMOVE)
                player.sendPacket(packet)
            }
        }
    }
}