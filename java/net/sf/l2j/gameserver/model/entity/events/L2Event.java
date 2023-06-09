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
package net.sf.l2j.gameserver.model.entity.events;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Logger;

import net.sf.l2j.commons.lang.StringUtil;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.util.Broadcast;

public class L2Event
{
	protected static final Logger _log = Logger.getLogger(L2Event.class.getName());
	public static String eventName = "";
	public static int teamsNumber = 0;
	public static final HashMap<Integer, String> names = new HashMap<>();
	public static final LinkedList<String> participatingPlayers = new LinkedList<>();
	public static final HashMap<Integer, LinkedList<String>> players = new HashMap<>();
	public static int id = 12760;
	public static final LinkedList<String> npcs = new LinkedList<>();
	public static boolean active = false;
	public static final HashMap<String, EventData> connectionLossData = new HashMap<>();
	
	public static int getTeamOfPlayer(String name)
	{
		for (int i = 1; i <= players.size(); i++)
		{
			LinkedList<String> temp = players.get(i);
			Iterator<String> it = temp.iterator();
			while (it.hasNext())
			{
				if (it.next().equals(name))
					return i;
			}
		}
		return 0;
	}
	
	public static String[] getTopNKillers(int N)
	{ // this will return top N players sorted by kills, first element in the array will be the one with more kills
		String[] killers = new String[N];
		String playerTemp = "";
		int kills = 0;
		LinkedList<String> killersTemp = new LinkedList<>();
		
		for (int k = 0; k < N; k++)
		{
			kills = 0;
			for (int i = 1; i <= teamsNumber; i++)
			{
				LinkedList<String> temp = players.get(i);
				Iterator<String> it = temp.iterator();
				while (it.hasNext())
				{
					try
					{
						L2PcInstance player = L2World.getInstance().getPlayer(it.next());
						if (!killersTemp.contains(player.getName()))
							if (player.kills.size() > kills)
							{
								kills = player.kills.size();
								playerTemp = player.getName();
							}
					}
					catch (Exception e)
					{
					}
				}
			}
			killersTemp.add(playerTemp);
		}
		
		for (int i = 0; i < N; i++)
		{
			kills = 0;
			Iterator<String> it = killersTemp.iterator();
			while (it.hasNext())
			{
				try
				{
					L2PcInstance player = L2World.getInstance().getPlayer(it.next());
					if (player.kills.size() > kills)
					{
						kills = player.kills.size();
						playerTemp = player.getName();
					}
				}
				catch (Exception e)
				{
				}
			}
			killers[i] = playerTemp;
			killersTemp.remove(playerTemp);
		}
		return killers;
	}
	
	public static void showEventHtml(L2PcInstance player, String objectid)
	{
		try
		{
			NpcHtmlMessage adminReply = new NpcHtmlMessage(5);
			
			DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream("data/events/" + eventName)));
			@SuppressWarnings("resource")
			BufferedReader inbr = new BufferedReader(new InputStreamReader(in));
			
			final StringBuilder replyMSG = new StringBuilder();
			StringUtil.append(replyMSG, "<html><body>" + "<center><font color=\"LEVEL\">", eventName, "</font><font color=\"FF0000\"> bY ", inbr.readLine(), "</font></center><br>" + "<br>", inbr.readLine());
			
			if (L2Event.participatingPlayers.contains(player.getName()))
				replyMSG.append("<br><center>You are already in the event players list !!</center></body></html>");
			else
				StringUtil.append(replyMSG, "<br><center><button value=\"Participate !! \" action=\"bypass -h npc_", String.valueOf(objectid), "_event_participate\" width=90 height=15 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center></body></html>");
			
			adminReply.setHtml(replyMSG.toString());
			player.sendPacket(adminReply);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void spawn(L2PcInstance target, int npcid)
	{
		NpcTemplate template1 = NpcTable.getInstance().getTemplate(npcid);
		try
		{
			// L2MonsterInstance mob = new L2MonsterInstance(template1);
			
			L2Spawn spawn = new L2Spawn(template1);
			
			spawn.setLoc(target.getX() + 50, target.getY() + 50, target.getZ(), target.getHeading());
		//	spawn.setLocy(target.getY() + 50);
		//	spawn.setLocz(target.getZ());
			// spawn.setAmount(1);
		//	spawn.setHeading(target.getHeading());
			spawn.setRespawnDelay(1);
			
			SpawnTable.getInstance().addNewSpawn(spawn, false);
			
			spawn.doSpawn(true);
			spawn.getNpc().setCurrentHp(999999999);
			spawn.getNpc().setName("event inscriptor");
			spawn.getNpc().setTitle(L2Event.eventName);
			spawn.getNpc().isEventMob = true;
			spawn.getNpc().isAggressive();
			spawn.getNpc().decayMe();
			spawn.getNpc().spawnMe(spawn.getNpc().getX(), spawn.getNpc().getY(), spawn.getNpc().getZ());
			
			spawn.getNpc().broadcastPacket(new MagicSkillUse(spawn.getNpc(), spawn.getNpc(), 1034, 1, 1, 1));
			
			npcs.add(String.valueOf(spawn.getNpc().getObjectId()));
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	public static void announceAllPlayers(String text)
	{
		Broadcast.announceToOnlinePlayers(text);
	}
	
	public static boolean isOnEvent(L2PcInstance player)
	{
		
		for (int k = 0; k < L2Event.teamsNumber; k++)
		{
			Iterator<String> it = L2Event.players.get(k + 1).iterator();
			boolean temp = false;
			while (it.hasNext())
			{
				temp = player.getName().equalsIgnoreCase(it.next());
				if (temp)
					return true;
			}
		}
		return false;
		
	}
	
	public static void inscribePlayer(L2PcInstance player)
	{
		
		try
		{
			L2Event.participatingPlayers.add(player.getName());
			player.eventkarma = player.getKarma();
			player.eventX = player.getX();
			player.eventY = player.getY();
			player.eventZ = player.getZ();
			player.eventpkkills = player.getPkKills();
			player.eventpvpkills = player.getPvpKills();
			player.eventTitle = player.getTitle();
			player.kills.clear();
			player.atEvent = true;
		}
		catch (Exception e)
		{
			_log.warning("error when signing in the event:" + e);
		}
	}
	
	public static void restoreChar(L2PcInstance player)
	{
		try
		{
			player.eventX = connectionLossData.get(player.getName()).eventX;
			player.eventY = connectionLossData.get(player.getName()).eventY;
			player.eventZ = connectionLossData.get(player.getName()).eventZ;
			player.eventkarma = connectionLossData.get(player.getName()).eventKarma;
			player.eventpvpkills = connectionLossData.get(player.getName()).eventPvpKills;
			player.eventpkkills = connectionLossData.get(player.getName()).eventPkKills;
			player.eventTitle = connectionLossData.get(player.getName()).eventTitle;
			player.kills = connectionLossData.get(player.getName()).kills;
			player.eventSitForced = connectionLossData.get(player.getName()).eventSitForced;
			player.atEvent = true;
		}
		catch (Exception e)
		{
		}
	}
	
	public static void restoreAndTeleChar(L2PcInstance target)
	{
		
		try
		{
			restoreChar(target);
			target.setTitle(target.eventTitle);
			target.setKarma(target.eventkarma);
			target.setPvpKills(target.eventpvpkills);
			target.setPkKills(target.eventpkkills);
			target.teleToLocation(target.eventX, target.eventY, target.eventZ, 30);
			target.kills.clear();
			target.eventSitForced = false;
			target.atEvent = false;
		}
		catch (Exception e)
		{
		}
	}
	
	class EventData
	{
		public int eventX;
		public int eventY;
		public int eventZ;
		public int eventKarma;
		public int eventPvpKills;
		public int eventPkKills;
		public String eventTitle;
		public LinkedList<String> kills = new LinkedList<>();
		public boolean eventSitForced = false;
		
		public EventData(int pEventX, int pEventY, int pEventZ, int pEventkarma, int pEventpvpkills, int pEventpkkills, String pEventTitle, LinkedList<String> pKills, boolean pEventSitForced)
		{
			eventX = pEventX;
			eventY = pEventY;
			eventZ = pEventZ;
			eventKarma = pEventkarma;
			eventPvpKills = pEventpvpkills;
			eventPkKills = pEventpkkills;
			eventTitle = pEventTitle;
			kills = pKills;
			eventSitForced = pEventSitForced;
		}
	}
}
