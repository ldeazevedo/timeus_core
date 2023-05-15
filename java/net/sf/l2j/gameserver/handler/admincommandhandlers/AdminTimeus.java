/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.events.EventManager;
import net.sf.l2j.gameserver.model.entity.events.TvTEvent;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.scripting.ScriptManager;

public class AdminTimeus implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_rf",
		"admin_clanchat",
		"admin_survival",
		"admin_add_player",
		"admin_remove_player",
		"admin_clear_players",
		"admin_clear",
		"admin_frintezza"
	};
	
	@Override
	public boolean useAdminCommand(String command, L2PcInstance activeChar)
	{
		StringTokenizer st = new StringTokenizer(command);
		command = st.nextToken();

		if (command.equals("admin_frintezza"))
			addPlayer(activeChar);
		if (command.equals("admin_add_player"))
			addTargetPlayer(true, activeChar);
		if (command.equals("admin_remove_player"))
			addTargetPlayer(false, activeChar);
		if (command.startsWith("admin_add_player"))
		{
			if (st.countTokens() > 1)
			{
				st.nextToken();
				String player = st.nextToken();
				L2PcInstance plyr = L2World.getInstance().getPlayer(player);
				if (plyr != null)
					register(true, plyr, activeChar);
			}
		}
		if (command.startsWith("admin_remove_player"))
		{
			if (st.countTokens() > 1)
			{
				st.nextToken();
				String player = st.nextToken();
				L2PcInstance plyr = L2World.getInstance().getPlayer(player);
				if (plyr != null)
					register(false, plyr, activeChar);
			}
		}
		else if (command.startsWith("admin_clear_players"))
			EventManager.getInstance().players.clear();
		else if (command.startsWith("admin_clear"))
			EventManager.getInstance().clear();
		else if (command.equals("admin_rf"))
			ScriptManager.getInstance().getQuest("EventsTask").startQuestTimer("doItJustOnceRF", 1000, null, null, false);
			//EventManager.getInstance().doItJustOnceRF();
		else if (command.equals("admin_survival"))
			ScriptManager.getInstance().getQuest("EventsTask").startQuestTimer("doItJustOnceSurvival", 1000, null, null, false);
			//EventManager.getInstance().doItJustOnceSurvival();
		else if (command.equals("admin_clanchat"))
		{
			try
			{
				final String clanName = st.nextToken();
				String message = "";
				while (st.hasMoreTokens())
					message += st.nextToken() + " ";
				
				L2Clan receiverClan = null;
				for (L2Clan clan : ClanTable.getInstance().getClans())
					if (clan.getName().equalsIgnoreCase(clanName))
					{
						receiverClan = clan;
						break;
					}
				if (receiverClan != null)
				{
					activeChar.sendMessage("[" + receiverClan.getName() + "]->" + message);
					receiverClan.broadcastToOnlineMembers(new ExShowScreenMessage(message, 3500, SMPOS.MIDDLE_RIGHT, false));
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				activeChar.sendMessage("Usage: //clanchat <clanname> [text]");
			}
		}
		return true;
	}

	private static void addPlayer(L2PcInstance activeChar)
	{
		final L2BossZone _LastImperialTomp = ZoneManager.getInstance().getZoneById(110011, L2BossZone.class);
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
			if (player != activeChar)
			{
				_LastImperialTomp.allowPlayerEntry(player, 60);
				player.teleToLocation(174232, -88020, -5110, 50);
			}
		}
	}
	
	private static void addTargetPlayer(boolean register, L2PcInstance activeChar)
	{
		L2Object target = activeChar.getTarget();
		L2PcInstance player = null;
		
		if (target instanceof L2PcInstance)
		{
			player = (L2PcInstance) target;
			if (player != activeChar)
				register(register, player, activeChar);
		}
	}
	
	private static void register(boolean register, L2PcInstance player, L2PcInstance activeChar)
	{
		if (EventManager.getInstance().isInProgress() || player.isInOlympiadMode() || player.isFestivalParticipant() || player.isInSiege() || player.isInJail() || player.isFestivalParticipant() || player.isDead() || player.getKarma() > 0 || player.isCursedWeaponEquipped() || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(player.getObjectId()))
			return;
		
		if (OlympiadManager.getInstance().isRegistered(player))
		{
			activeChar.sendMessage("No puede participar ni ver el evento mientras esta registrado en oly.");
			return;
		}
		if (register)
		{
			if (player.inObserverMode())
			{
				activeChar.sendMessage("No puedes anotar al player si esta mirando el evento.");
				return;
			}
			if (EventManager.getInstance().containsPlayer(player))
			{
				activeChar.sendMessage("Ya esta registrado en el evento.");
				return;
			}
			EventManager.getInstance().addPlayer(player);
			activeChar.sendMessage("Player registrado al evento.");
		}
		else
		{
			if (!EventManager.getInstance().containsPlayer(player))
			{
				activeChar.sendMessage("No esta registrado en el evento.");
				return;
			}
			EventManager.getInstance().removePlayer(player);
			activeChar.sendMessage("Player removido del evento.");
		}
		return;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
}