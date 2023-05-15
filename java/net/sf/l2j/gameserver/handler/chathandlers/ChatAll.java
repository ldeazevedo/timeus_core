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
package net.sf.l2j.gameserver.handler.chathandlers;

import java.util.StringTokenizer;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ClanTable;
import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.events.EventManager;
import net.sf.l2j.gameserver.model.entity.events.TvTEvent;
import net.sf.l2j.gameserver.model.entity.events.TvTManager;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.FloodProtectors.Action;

public class ChatAll implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		0
	};
	
	private static final String htmlPath = "data/html/mods/TvTEvent/";
	
	@Override
	public void handleChat(int type, L2PcInstance activeChar, String params, String text)
	{
		if (!FloodProtectors.performAction(activeChar.getClient(), Action.GLOBAL_CHAT))
			return;
		
		if (!TvTEvent.isInactive())
		{
			if (text.equalsIgnoreCase(".register"))
			{
				TvTEvent.onBypass("tvt_event_participation", activeChar);
				return;
			}
		}
		
		if (text.equalsIgnoreCase(".register") || text.equalsIgnoreCase(".unregister") || text.equalsIgnoreCase(".ver") || text.equalsIgnoreCase(".salir"))
		{
			EventManager.getInstance().checkTimeusEvents(text, activeChar);
			return;
		}
		if (activeChar.isGM())
		{
			if (text.equalsIgnoreCase(".blockglobal"))
			{
				EventManager.getInstance().setBlock();
				return;
			}
			if (text.startsWith(".clanchat"))
			{
				StringTokenizer st = new StringTokenizer(text);
				text = st.nextToken();
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
						receiverClan.broadcastToOnlineMembers(new CreatureSay(activeChar.getObjectId(), 4, activeChar.getName(), message));
						activeChar.sendPacket(new CreatureSay(activeChar.getObjectId(), 9, activeChar.getName(), "[" + receiverClan.getName() + "]:" + message));
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					activeChar.sendMessage("Usage: .clanchat <clanname> [text]");
				}
				return;
			}
			if (text.startsWith(".chat") || text.startsWith(".all") || text.startsWith(".clan"))
			{
				boolean global = text.startsWith(".all");
				boolean clan = text.startsWith(".clan");
				StringTokenizer st = new StringTokenizer(text);
				text = st.nextToken();
				try
				{
					final String charName = st.nextToken();
					String message = "";
					while (st.hasMoreTokens())
						message += st.nextToken() + " ";
					
					L2PcInstance victima = null;
					victima = L2World.getInstance().getPlayer(charName);
					
					if (victima != null)
					{
						if (!clan)
						{
							String chat = !global ? "" : victima.isGM() ? " [ADMIN]" : " [Global]";
							final CreatureSay cs = new CreatureSay(victima.getObjectId(), !global ? type : 1, victima.getName() + chat, message);
							if (global)
								for (L2PcInstance player : L2World.getInstance().getPlayers())
									player.sendPacket(cs);
							else
								for (L2PcInstance player : victima.getKnownList().getKnownTypeInRadius(L2PcInstance.class, 1250))
								{
									player.sendPacket(cs);
									victima.sendPacket(cs);
								}
						}
						else if (victima.getClan() != null)
						{
							GmListTable.broadcastToGMs(new CreatureSay(victima.getObjectId(), 9, victima.getName(), "[" + victima.getClan().getName() + "]:" + message));
							victima.getClan().broadcastToOnlineMembers(new CreatureSay(victima.getObjectId(), 4, victima.getName(), message));
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					activeChar.sendMessage("Usage: .clanchat <clanname> [text]");
				}
				return;
			}
		}
		if (text.equalsIgnoreCase(".pvp"))
		{
			activeChar.showPvP();
			return;
		}
		if (EventManager.getInstance().blockAllChat && !activeChar.isGM())
		{
			activeChar.sendMessage("Chat desactivado temporalmente por el administrador");
			return;
		}
		
		final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName(), text);
		for (L2PcInstance player : activeChar.getKnownList().getKnownTypeInRadius(L2PcInstance.class, 1250))
		{
			if (!BlockList.isBlocked(player, activeChar))
				player.sendPacket(cs);
		}
		activeChar.sendPacket(cs);
	}
	
	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
}