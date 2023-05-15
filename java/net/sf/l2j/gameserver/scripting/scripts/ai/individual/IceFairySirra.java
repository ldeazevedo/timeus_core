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
package net.sf.l2j.gameserver.scripting.scripts.ai.individual;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;

import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SpawnTable;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2DoorInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.scripts.ai.L2AttackableAIScript;

public class IceFairySirra extends L2AttackableAIScript
{
	private static final int STEWARD = 32029;
	private static final int SILVER_HEMOCYTE = 8057;
	private static L2BossZone _freyasZone;
	private static L2PcInstance _player = null;
	protected List<L2Npc> _allMobs = new ArrayList<>();
	protected Future<?> _onDeadEventTask = null;
	
	public IceFairySirra()
	{
		super("ai/individual");
		addEventId(STEWARD, EventType.QUEST_START);
		addEventId(STEWARD, EventType.ON_TALK);
		addEventId(STEWARD, EventType.ON_FIRST_TALK);
		init();
	}
	
	@Override
	protected void registerNpcs()
	{
		int[] mob =
		{
			STEWARD,
			22100,
			22102,
			22104
		};
		addEventIds(mob, EventType.ON_KILL, EventType.ON_SPAWN, EventType.ON_ATTACK);
	}
	
	public void init()
	{
		//TODO:		_freyasZone = GrandBossManager.getInstance().getZoneByXYZ(105546, -127892, -2768);
		if (_freyasZone == null)
		{
			_log.warning("IceFairySirraManager: Failed to load zone");
			return;
		}
		// _freyasZone.setZoneEnabled(false);
		L2Npc steward = findTemplate(STEWARD);
		if (steward != null)
			setBusy(false);
		openGates();
	}
	
	public void cleanUp()
	{
		init();
		cancelQuestTimer("30MinutesRemaining", null, _player);
		cancelQuestTimer("20MinutesRemaining", null, _player);
		cancelQuestTimer("10MinutesRemaining", null, _player);
		cancelQuestTimer("End", null, _player);
		for (L2Npc mob : _allMobs)
		{
			try
			{
				//TODO:		mob.getSpawn().stopRespawn();
					mob.deleteMe();
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "IceFairySirraManager: Failed deleting mob.", e);
			}
		}
		_allMobs.clear();
	}
	
	public L2Npc findTemplate(int npcId)
	{
		L2Npc npc = null;
		for (L2Spawn spawn : SpawnTable.getInstance().getSpawnTable())
			if (spawn != null && spawn.getNpcId() == npcId)
			{
			//TODO:	npc = spawn.getLastSpawn();
				break;
			}
		return npc;
	}
	
	protected void openGates()
	{
		for (int i = 23140001; i < 23140003; i++)
		{
			try
			{
				L2DoorInstance door = DoorTable.getInstance().getDoor(i);
				if (door != null)
				{
					door.openMe();
				}
				else
				{
					_log.warning("IceFairySirraManager: Attempted to open undefined door. doorId: " + i);
				}
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "IceFairySirraManager: Failed closing door", e);
			}
		}
	}
	
	protected void closeGates()
	{
		for (int i = 23140001; i < 23140003; i++)
		{
			try
			{
				L2DoorInstance door = DoorTable.getInstance().getDoor(i);
				if (door != null)
					door.closeMe();
				else
					_log.warning("IceFairySirraManager: Attempted to close undefined door. doorId: " + i);
			}
			catch (Exception e)
			{
				_log.log(Level.SEVERE, "IceFairySirraManager: Failed closing door", e);
			}
		}
	}
	
	public boolean checkItems(L2PcInstance player)
	{
		if (player.getParty() != null)
		{
			for (L2PcInstance pc : player.getParty().getPartyMembers())
			{
				ItemInstance i = pc.getInventory().getItemByItemId(SILVER_HEMOCYTE);
				if (i == null || i.getCount() < 10)
					return false;
			}
		}
		else
			return false;
		return true;
	}
	
	public void destroyItems(L2PcInstance player)
	{
		if (player.getParty() != null)
		{
			for (L2PcInstance pc : player.getParty().getPartyMembers())
			{
				ItemInstance i = pc.getInventory().getItemByItemId(SILVER_HEMOCYTE);
				pc.destroyItem("Hemocytes", i.getObjectId(), 10, null, false);
			}
		}
		else
			cleanUp();
	}
	
	public void teleportInside(L2PcInstance player)
	{
		if (player.getParty() != null)
		{
			for (L2PcInstance pc : player.getParty().getPartyMembers())
			{
				pc.teleToLocation(113533, -126159, -3488, 0);
				if (_freyasZone == null)
				{
					_log.warning("IceFairySirraManager: Failed to load zone");
					cleanUp();
					return;
				}
				_freyasZone.allowPlayerEntry(pc, 2103);
			}
		}
		else
			cleanUp();
	}
	
	public void screenMessage(L2PcInstance player, String text, int time)
	{
		if (player.getParty() != null)
		{
			for (L2PcInstance pc : player.getParty().getPartyMembers())
			{
				pc.sendPacket(new ExShowScreenMessage(text, time));
			}
		}
		else
			cleanUp();
	}
	
	public void doSpawns()
	{
		int[][] mobs =
		{
			{
				29060,
				105546,
				-127892,
				-2768
			},
			{
				29056,
				102779,
				-125920,
				-2840
			},
			{
				22100,
				111719,
				-126646,
				-2992
			},
			{
				22102,
				109509,
				-128946,
				-3216
			},
			{
				22104,
				109680,
				-125756,
				-3136
			}
		};
		L2Spawn spawnDat;
		NpcTemplate template;
		try
		{
			for (int i = 0; i < 5; i++)
			{
				template = NpcTable.getInstance().getTemplate(mobs[i][0]);
				if (template != null)
				{
					spawnDat = new L2Spawn(template);
					// spawnDat.setAmount(1);
					spawnDat.setLoc(mobs[i][1],mobs[i][2],mobs[i][3], 0);
				//	spawnDat.setLocx(mobs[i][1]);
				//	spawnDat.setLocy(mobs[i][2]);
				//	spawnDat.setLocz(mobs[i][3]);
				//	spawnDat.setHeading(0);
					spawnDat.setRespawnDelay(60);
					SpawnTable.getInstance().addNewSpawn(spawnDat, false);
					_allMobs.add(spawnDat.doSpawn(true));
			//		spawnDat.stopRespawn();
				}
				else
				{
					_log.warning("IceFairySirraManager: Data missing in NPC table for ID: " + mobs[i][0]);
				}
			}
		}
		catch (Exception e)
		{
			_log.warning("IceFairySirraManager: Spawns could not be initialized: " + e);
		}
	}
	
	public String getHtmlPath(int val)
	{
		String pom = "";
		
		pom = "32029-" + val;
		if (val == 0)
			pom = "32029";
		
		String temp = "data/html/default/" + pom + ".htm";
		
		// if (!Config.LAZY_CACHE)
		// {
		// If not running lazy cache the file must be in the cache or it doesnt exist
		// if (HtmCache.getInstance().contains(temp))
		// return temp;
		// }
		// else
		{
			if (HtmCache.getInstance().isLoadable(temp))
				return temp;
		}
		
		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}
	
	public void sendHtml(L2Npc npc, L2PcInstance player, String filename)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(npc.getObjectId()));
		player.sendPacket(html);
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		if (player.getQuestState("IceFairySirra") == null)
			newQuestState(player);
		player.setLastQuestNpcObject(npc.getObjectId());
		String filename = "";
		if (isBusy)
			filename = getHtmlPath(10);
		else
			filename = getHtmlPath(0);
		sendHtml(npc, player, filename);
		return null;
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equalsIgnoreCase("check_condition"))
		{
			if (isBusy)// should never happen
				return super.onAdvEvent(event, npc, player);
			// else
			{
				String filename = "";
				if (player.isInParty() && player.getParty().getPartyLeaderOID() == player.getObjectId())
				{
					if (checkItems(player) == true)
					{
						startQuestTimer("start", 100000, null, player, false);
						_player = player;
						destroyItems(player);
						player.getInventory().addItem("Scroll", 8379, 3, player, null);
						setBusy(true);
						screenMessage(player, "Steward: Please wait a moment.", 100000);
						filename = getHtmlPath(3);
					}
					else
					{
						filename = getHtmlPath(2);
					}
				}
				else
				{
					filename = getHtmlPath(1);
				}
				sendHtml(npc, player, filename);
			}
		}
		else if (event.equalsIgnoreCase("start"))
		{
			if (_freyasZone == null)
			{
				_log.warning("IceFairySirraManager: Failed to load zone");
				cleanUp();
				return super.onAdvEvent(event, npc, player);
			}
			// _freyasZone.setZoneEnabled(true);
			closeGates();
			doSpawns();
			startQuestTimer("Party_Port", 2000, null, player, false);
			startQuestTimer("End", 1802000, null, player, false);
		}
		else if (event.equalsIgnoreCase("Party_Port"))
		{
			teleportInside(player);
			screenMessage(player, "Steward: Please restore the Queen's appearance!", 10000);
			startQuestTimer("30MinutesRemaining", 300000, null, player, false);
		}
		else if (event.equalsIgnoreCase("30MinutesRemaining"))
		{
			screenMessage(player, "30 minute(s) are remaining.", 10000);
			startQuestTimer("20minutesremaining", 600000, null, player, false);
		}
		else if (event.equalsIgnoreCase("20MinutesRemaining"))
		{
			screenMessage(player, "20 minute(s) are remaining.", 10000);
			startQuestTimer("10minutesremaining", 600000, null, player, false);
		}
		else if (event.equalsIgnoreCase("10MinutesRemaining"))
		{
			screenMessage(player, "Steward: Waste no time! Please hurry!", 10000);
		}
		else if (event.equalsIgnoreCase("End"))
		{
			screenMessage(player, "Steward: Was it indeed too much to ask.", 10000);
			cleanUp();
		}
		return super.onAdvEvent(event, npc, player);
	}
	
	private boolean isBusy;
	
	private void setBusy(boolean b)
	{
		isBusy = b;
	}
}