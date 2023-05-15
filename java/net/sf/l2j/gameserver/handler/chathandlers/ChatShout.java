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

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.MapRegionTable;
import net.sf.l2j.gameserver.handler.IChatHandler;
import net.sf.l2j.gameserver.model.BlockList;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.events.EventManager;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.util.FloodProtectors;
import net.sf.l2j.gameserver.util.FloodProtectors.Action;

public class ChatShout implements IChatHandler
{
	private static final int[] COMMAND_IDS =
	{
		1
	};
	
	@Override
	public void handleChat(int type, L2PcInstance activeChar, String target, String text)
	{
		if (!FloodProtectors.performAction(activeChar.getClient(), Action.GLOBAL_CHAT))
			return;
		
		if (EventManager.getInstance().blockAllChat && !activeChar.isGM())
		{
			activeChar.sendMessage("Chat desactivado temporalmente por el administrador");
			return;
		}
		String txt = activeChar.isGM() ? " [ADMIN]" : " [Global]";
		
		if (activeChar.isChatGlobal)
		{
			final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName() + txt, text);
			for (L2PcInstance player : L2World.getInstance().getPlayers())
				if (!BlockList.isBlocked(player, activeChar))
					player.sendPacket(cs);
			activeChar.isChatGlobal = false;
			ThreadPoolManager.getInstance().scheduleGeneral(new ChatTask(activeChar), 5000);
			// activeChar.sendMessage("Proximo mensaje global en 5 segundos");
		}
		else
		{
			final CreatureSay cs = new CreatureSay(activeChar.getObjectId(), type, activeChar.getName() + " [Regional]", text);
			final int region = MapRegionTable.getMapRegion(activeChar.getX(), activeChar.getY());
			
			for (L2PcInstance player : L2World.getInstance().getPlayers())
			{
				if (!BlockList.isBlocked(player, activeChar) && region == MapRegionTable.getMapRegion(player.getX(), player.getY()))
					player.sendPacket(cs);
			}
		}
	}
	
	@Override
	public int[] getChatTypeList()
	{
		return COMMAND_IDS;
	}
	
	private class ChatTask implements Runnable
	{
		L2PcInstance pc = null;
		
		ChatTask(L2PcInstance _pc)
		{
			pc = _pc;
		}
		
		@Override
		public void run()
		{
			if (pc == null)
				return;
			if (!pc.isChatGlobal)
				pc.isChatGlobal = true;
		}
	}
}