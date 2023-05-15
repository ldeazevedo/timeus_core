/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package net.sf.l2j.gameserver.scripting.scripts.ai.group;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.datatables.DoorTable;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2CommandChannel;
import net.sf.l2j.gameserver.model.L2Party;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.zone.type.L2BossZone;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.Earthquake;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.gameserver.scripting.ScriptManager;
import net.sf.l2j.gameserver.scripting.scripts.ai.L2AttackableAIScript;
import net.sf.l2j.gameserver.scripting.scripts.ai.individual.Frintezza;

public class LastImperialTomb extends L2AttackableAIScript
{
	//features unconfirmed retail
	private static boolean not_confirmed = true;
	
	private static final Location[] invadeLoc =
	{
		new Location(174102, -76039, -5105),
		new Location(173235, -76884, -5105),
		new Location(175003, -76933, -5105),
		new Location(174196, -76190, -5105),
		new Location(174013, -76120, -5105),
		new Location(173263, -75161, -5105)
	};
	private static final Location TeleOut = new Location(150037, -57720, -2976);
	
    private static final int[][] ROOM1_SPAWN =
    {
            { 18328,172894,-76019,-5107,243 },
            { 18328,174095,-77279,-5107,16216 },
            { 18328,174111,-74833,-5107,49043 },
            { 18328,175344,-76042,-5107,32847 },
            { 18330,173489,-76227,-5134,63565 },
            { 18330,173498,-75724,-5107,58498 },
            { 18330,174365,-76745,-5107,22424 },
            { 18330,174570,-75584,-5107,31968 },
            { 18330,174613,-76179,-5107,31471 },
            { 18332,173620,-75981,-5107,4588 },
            { 18332,173630,-76340,-5107,62454 },
            { 18332,173755,-75613,-5107,57892 },
            { 18332,173823,-76688,-5107,2411 },
            { 18332,174000,-75411,-5107,54718 },
            { 18332,174487,-75555,-5107,33861 },
            { 18332,174517,-76471,-5107,21893 },
            { 18332,174576,-76122,-5107,31176 },
            { 18332,174600,-75841,-5134,35927 }
    };
    //	Hall Keeper Captain
	private static final int[][] ROOM1_CAPTAIN =
	{
		{ 18329,173481,-76043,-5107,61312 },
		{ 18329,173539,-75678,-5107,59524 },
		{ 18329,173584,-76386,-5107,3041 },
		{ 18329,173773,-75420,-5107,51115 },
		{ 18329,173777,-76650,-5107,12588 },
		{ 18329,174585,-76510,-5107,21704 },
		{ 18329,174623,-75571,-5107,40141 },
		{ 18329,174744,-76240,-5107,29202 },
		{ 18329,174769,-75895,-5107,29572 }
	};
	//	Hall Keeper Suicidal Soldier
	private static final int[][] ROOM1_SUICIDAL_SOLDIER =
	{
		{ 18333,173861,-76011,-5107,383 },
		{ 18333,173872,-76461,-5107,8041 },
		{ 18333,173898,-75668,-5107,51856 },
		{ 18333,174422,-75689,-5107,42878 },
		{ 18333,174460,-76355,-5107,27311 },
		{ 18333,174483,-76041,-5107,30947 }
	};
	//	Hall Keeper Guard
	private static final int[][] ROOM1_GUARD =
	{
		{ 18331,173515,-76184,-5107,6971 },
		{ 18331,173516,-75790,-5134,3142 },
		{ 18331,173696,-76675,-5107,6757 },
		{ 18331,173766,-75502,-5134,60827 },
		{ 18331,174473,-75321,-5107,37147 },
		{ 18331,174493,-76505,-5107,34503 },
		{ 18331,174568,-75654,-5134,41661 },
		{ 18331,174584,-76263,-5107,31729 }
	};
	//	Dark Choir Player
	private static final int[][] ROOM2_SPAWN =
	{
		{ 18339,173892,-81592,-5123,50849 },
		{ 18339,173958,-81820,-5123,7459 },
		{ 18339,174128,-81805,-5150,21495 },
		{ 18339,174245,-81566,-5123,41760 }
	};
	private static final int[][] ROOM2_SPAWN2 =
	{
		{ 18334,173264,-81529,-5072,1646 },
		{ 18334,173265,-81656,-5072,441 },
		{ 18334,173267,-81889,-5072,0 },
		{ 18334,173271,-82015,-5072,65382 },
		{ 18334,174867,-81655,-5073,32537 },
		{ 18334,174868,-81890,-5073,32768 },
		{ 18334,174869,-81485,-5073,32315 },
		{ 18334,174871,-82017,-5073,33007 },
		{ 18335,173074,-80817,-5107,8353 },
		{ 18335,173128,-82702,-5107,5345 },
		{ 18335,173181,-82544,-5107,65135 },
		{ 18335,173191,-80981,-5107,6947 },
		{ 18335,174859,-80889,-5134,24103 },
		{ 18335,174924,-82666,-5107,38710 },
		{ 18335,174947,-80733,-5107,22449 },
		{ 18335,175096,-82724,-5107,42205 },
		{ 18336,173435,-80512,-5107,65215 },
		{ 18336,173440,-82948,-5107,417 },
		{ 18336,173443,-83120,-5107,1094 },
		{ 18336,173463,-83064,-5107,286 },
		{ 18336,173465,-80453,-5107,174 },
		{ 18336,173465,-83006,-5107,2604 },
		{ 18336,173468,-82889,-5107,316 },
		{ 18336,173469,-80570,-5107,65353 },
		{ 18336,173469,-80628,-5107,166 },
		{ 18336,173492,-83121,-5107,394 },
		{ 18336,173493,-80683,-5107,0 },
		{ 18336,173497,-80510,-5134,417 },
		{ 18336,173499,-82947,-5107,0 },
		{ 18336,173521,-83063,-5107,316 },
		{ 18336,173523,-82889,-5107,128 },
		{ 18336,173524,-80627,-5134,65027 },
		{ 18336,173524,-83007,-5107,0 },
		{ 18336,173526,-80452,-5107,64735 },
		{ 18336,173527,-80569,-5134,65062 },
		{ 18336,174602,-83122,-5107,33104 },
		{ 18336,174604,-82949,-5107,33184 },
		{ 18336,174609,-80514,-5107,33234 },
		{ 18336,174609,-80684,-5107,32851 },
		{ 18336,174629,-80627,-5107,33346 },
		{ 18336,174632,-80570,-5107,32896 },
		{ 18336,174632,-83066,-5107,32768 },
		{ 18336,174635,-82893,-5107,33594 },
		{ 18336,174636,-80456,-5107,32065 },
		{ 18336,174639,-83008,-5107,33057 },
		{ 18336,174660,-80512,-5107,33057 },
		{ 18336,174661,-83121,-5107,32768 },
		{ 18336,174663,-82948,-5107,32768 },
		{ 18336,174664,-80685,-5107,32676 },
		{ 18336,174687,-83008,-5107,32520 },
		{ 18336,174691,-83066,-5107,32961 },
		{ 18336,174692,-80455,-5107,33202 },
		{ 18336,174692,-80571,-5107,32768 },
		{ 18336,174693,-80630,-5107,32994 },
		{ 18336,174693,-82889,-5107,32622 },
		{ 18337,172837,-82382,-5107,58363 },
		{ 18337,172867,-81123,-5107,64055 },
		{ 18337,172883,-82495,-5107,64764 },
		{ 18337,172916,-81033,-5107,7099 },
		{ 18337,172940,-82325,-5107,58998 },
		{ 18337,172946,-82435,-5107,58038 },
		{ 18337,172971,-81198,-5107,14768 },
		{ 18337,172992,-81091,-5107,9438 },
		{ 18337,173032,-82365,-5107,59041 },
		{ 18337,173064,-81125,-5107,5827 },
		{ 18337,175014,-81173,-5107,26398 },
		{ 18337,175061,-82374,-5107,43290 },
		{ 18337,175096,-81080,-5107,24719 },
		{ 18337,175169,-82453,-5107,37672 },
		{ 18337,175172,-80972,-5107,32315 },
		{ 18337,175174,-82328,-5107,41760 },
		{ 18337,175197,-81157,-5107,27617 },
		{ 18337,175245,-82547,-5107,40275 },
		{ 18337,175249,-81075,-5107,28435 },
		{ 18337,175292,-82432,-5107,42225 },
		{ 18338,173014,-82628,-5107,11874 },
		{ 18338,173033,-80920,-5107,10425 },
		{ 18338,173095,-82520,-5107,49152 },
		{ 18338,173115,-80986,-5107,9611 },
		{ 18338,173144,-80894,-5107,5345 },
		{ 18338,173147,-82602,-5107,51316 },
		{ 18338,174912,-80825,-5107,24270 },
		{ 18338,174935,-80899,-5107,18061 },
		{ 18338,175016,-82697,-5107,39533 },
		{ 18338,175041,-80834,-5107,25420 },
		{ 18338,175071,-82549,-5107,39163 },
		{ 18338,175154,-82619,-5107,36345 }
	};

	private static final int GUIDE = 32011;
	private static final int CUBE = 29061;
	
	private static final int ALARM_DEVICE = 18328;
	private static final int CHOIR_CAPTAIN = 18334;
	private static final int CHOIR_PRAYER = 18339;

	// Items
	private static final int FRINTEZZA_SCROLL = 8073; // Frintezza's Magic Force Field Removal Scroll.
	//http://legacy.lineage2.com/news/chronicle5_07.html
	private static final int DEWDROP_OF_DESTRUCTION = 8556; // Dewdrop of Destruction
	// Clicking the Dewdrop of Destruction will make the portrait disappear. It will disappear when one enters the Last Imperial Sepulcher.
	private static final int BREAKING_ARROW = 8192; // Breaking Arrow
	// Double-click the arrow to temporarily stop Frintezza's musical performance. This gets deleted when one enters the Tomb of the Last Emperor.
	
	private static int _LocCycle = 0, _killMobs = 0;

	private static final L2BossZone _LastImperialTomp = ZoneManager.getInstance().getZoneById(110011, L2BossZone.class);

	private final List<L2Npc> _mobs = new CopyOnWriteArrayList<>();
	
	public LastImperialTomb()
	{
		super("ai/group");

		addKillId(ALARM_DEVICE, 18329, 18330, 18331, 18332, 18333, CHOIR_CAPTAIN, 18335, 18336, 18337, 18338, CHOIR_PRAYER);
		addSpawnId(ALARM_DEVICE, CHOIR_CAPTAIN);
		addStartNpc(GUIDE, CUBE);
		addTalkId(GUIDE, CUBE);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equalsIgnoreCase("waiting"))
		{
			startQuestTimer("close", 2700, npc, null, false);
			ScriptManager.getInstance().getQuest("Frintezza").startQuestTimer("beginning", 3000, null, null, false);
			_LastImperialTomp.broadcastPacket(new Earthquake(174232, -88020, -5116, 45, 27));
		}
		else if (event.equalsIgnoreCase("room1_spawn"))
			for (int[] loc : ROOM1_SPAWN)
	            _mobs.add(addSpawn(loc[0], loc[1], loc[2], loc[3], loc[4], false, 0, false));
		else if (event.equalsIgnoreCase("room1_spawn2"))
			for (int[] loc : ROOM1_CAPTAIN)
				_mobs.add(addSpawn(loc[0], loc[1], loc[2], loc[3], loc[4], false, 0, false));
		else if (event.equalsIgnoreCase("room1_spawn3"))
			for (int[] loc : ROOM1_SUICIDAL_SOLDIER)
				_mobs.add(addSpawn(loc[0], loc[1], loc[2], loc[3], loc[4], false, 0, false));
		else if (event.equalsIgnoreCase("room1_spawn4"))
			for (int[] loc : ROOM1_GUARD)
				_mobs.add(addSpawn(loc[0], loc[1], loc[2], loc[3], loc[4], false, 0, false));
		else if (event.equalsIgnoreCase("room2_spawn"))
			for (int[] loc : ROOM2_SPAWN)
				_mobs.add(addSpawn(loc[0], loc[1], loc[2], loc[3], loc[4], false, 0, false));
		else if (event.equalsIgnoreCase("room2_spawn2"))
			for (int[] loc : ROOM2_SPAWN2)
				_mobs.add(addSpawn(loc[0], loc[1], loc[2], loc[3], loc[4], false, 0, false));
		else if (event.equalsIgnoreCase("mobs_del"))
		{
			for (L2Npc mob : _mobs)
				if (mob != null)
					mob.deleteMe();
			_mobs.clear();
		}
		else if (event.equalsIgnoreCase("clean"))
		{
			_LocCycle = 0;
			_killMobs = 0;
		}
		else if (event.equalsIgnoreCase("close"))
		{
			for (int i = 25150051; i <= 25150058; i++)
				openCloseDoor(false, i);
			
			for (int i = 25150061; i <= 25150070; i++)
				openCloseDoor(false, i);

			openCloseDoor(false, 25150042, 25150043, 25150045, 25150046);
		}
		else if (event.equalsIgnoreCase("room_final"))
		{
			_LastImperialTomp.broadcastPacket(new CreatureSay(npc.getObjectId(), 1, npc.getName(), "Exceeded his time limit, challenge failed!"));
			startQuestTimer("remove_players", 0, null, null, false);
			
			cancelQuestTimers("waiting");
			startQuestTimer("clean", 1000, npc, null, false);
			startQuestTimer("close", 1000, npc, null, false);
			startQuestTimer("mobs_del", 1000, npc, null, false);
			
			Frintezza.setBossStatus(Frintezza.DORMANT);
		}
		else if (event.equalsIgnoreCase("remove_players"))
			_LastImperialTomp.oustAllPlayers();
		
		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		NpcSay say = null;
		if (npc.getNpcId() == ALARM_DEVICE)
			say = new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getNpcId(), "Intruders! Sound the alarm!");
		if (npc.getNpcId() == CHOIR_CAPTAIN)
			say = new NpcSay(npc.getObjectId(), Say2.SHOUT, npc.getNpcId(), "How dare you ruin the performance of the Dark Choir... Unforgivable!");
		npc.setIsImmobilized(true);
		npc.setIsNoRndWalk(true);
		npc.disableCoreAI(true);
		if (say != null && not_confirmed)
			_LastImperialTomp.broadcastPacket(say);
		return super.onSpawn(npc);
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = "";
		if (npc.getNpcId() == CUBE)
		{
			player.teleToLocation(TeleOut.getX() + Rnd.get(500), TeleOut.getY() + Rnd.get(500),TeleOut.getZ(), 0);
			return null;
		}
		if (player.isGM())
		{
			_LocCycle++;
			_LastImperialTomp.allowPlayerEntry(player, 30);
			player.teleToLocation(invadeLoc[_LocCycle], 50);
			Frintezza.setBossStatus(Frintezza.WAITING);

			player.destroyItemByItemId(getName(), DEWDROP_OF_DESTRUCTION, player.getInventory().getInventoryItemCount(DEWDROP_OF_DESTRUCTION, -1), null, true);
			
			startQuestTimer("clean", 0, npc, null, false);
			startQuestTimer("close", 0, npc, null, false);
			startQuestTimer("room1_spawn", 5000, npc, null, false);
			startQuestTimer("room_final", 2100000, npc, null, false); // 35min
			
			return null;
		}
		
		if (GrandBossManager.getInstance().getBossStatus(Frintezza.FRINTEZZA) == Frintezza.DEAD)
			htmltext = "<html><body>There is nothing beyond the Magic Force Field. Come back later.<br>(You may not enter because Frintezza is not inside the Imperial Tomb.)</body></html>";
		else if (GrandBossManager.getInstance().getBossStatus(Frintezza.FRINTEZZA) == Frintezza.DORMANT)
		{
			if ((!player.isInParty() || !player.getParty().isLeader(player)) || (player.getParty().getCommandChannel() == null) || (player.getParty().getCommandChannel().getChannelLeader() != player))
				htmltext = "<html><body>No reaction. Contact must be initiated by the Command Channel Leader.</body></html>";
			else if (player.getParty().getCommandChannel().getPartys().size() < 2 || player.getParty().getCommandChannel().getPartys().size() > 5)
				htmltext = "<html><body>Your command channel needs to have at least 2 parties and a maximum of 5.</body></html>";
			else
			{
				if (!player.destroyItemByItemId("Quest", FRINTEZZA_SCROLL, 1, player, true))
					return "<html><body>You dont have required item.</body></html>";
				Frintezza.setBossStatus(Frintezza.WAITING);

				startQuestTimer("clean", 0, npc, null, false);
				startQuestTimer("close", 0, npc, null, false);
				startQuestTimer("room1_spawn", 5000, npc, null, false);
				startQuestTimer("room_final", 2100000, npc, null, false); // 35min
				
				if (player.getParty() != null)
				{
					L2CommandChannel CommandChannel = player.getParty().getCommandChannel();
					if (CommandChannel != null)
						for (L2Party party : CommandChannel.getPartys())
						{
							if (party == null)
								continue;
							for (L2PcInstance member : party.getPartyMembers())
							{
								if (member == null || member.getLevel() < 74 || !member.isInsideRadius(npc, 700, false, false))
									continue;
								if (CommandChannel.getMemberCount() > 45)
								{
									member.sendMessage("The number of challenges have been full, so can not enter.");
									break;
								}

								member.destroyItemByItemId(getName(), DEWDROP_OF_DESTRUCTION, member.getInventory().getInventoryItemCount(DEWDROP_OF_DESTRUCTION, -1), null, true);
								member.destroyItemByItemId(getName(), BREAKING_ARROW, member.getInventory().getInventoryItemCount(BREAKING_ARROW, -1), null, true);
								_LastImperialTomp.allowPlayerEntry(member, 30);
								member.teleToLocation(invadeLoc[_LocCycle], 50);
							}
							_LocCycle++;
							if (_LocCycle >= 6)
								_LocCycle = 1;
						}
				}
			}
		}
		else
			htmltext = "<html><body>Someone else is already inside the Magic Force Field. Try again later.</body></html>";
		
		return htmltext;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		if (GrandBossManager.getInstance().getBossStatus(Frintezza.FRINTEZZA) == Frintezza.WAITING)
		{
			switch(npc.getNpcId())
			{
				case 18329:
				case 18330:
				case 18331:
				case 18332:
				case 18333:
					if (Rnd.get(100) < 10)
						killer.addItem("Quest", DEWDROP_OF_DESTRUCTION, 1, npc, true);
					break;
				case ALARM_DEVICE:
					if (Rnd.get(100) < 33)
						killer.addItem("Quest", DEWDROP_OF_DESTRUCTION, 1, npc, true);
					_killMobs++;
					
					if (not_confirmed)
					{
						if (_killMobs <= 3 && Rnd.get(100) < _killMobs * 5) // 5% chance first alarm, 10% in the second alarm
							for (int i = 25150051; i <= 25150058; i++)
								openCloseDoor(true, i);
						if (_killMobs == 1)
							startQuestTimer("room1_spawn2", 200, npc, null, false);
						if (_killMobs == 2)
							startQuestTimer("room1_spawn3", 200, npc, null, false);
						if (_killMobs == 3)
							startQuestTimer("room1_spawn4", 200, npc, null, false);
					}
					if (_killMobs == 4)
					{
						for (int i = 25150051; i <= 25150058; i++)
							openCloseDoor(true, i);
						startQuestTimer("mobs_del", 100, npc, null, false);
						startQuestTimer("room2_spawn", 200, npc, null, false);
						
						openCloseDoor(true, 25150042, 25150043); //, 25150045, 25150046
					}
					break;
				case 18339:
					if (_killMobs <= 6)
						_killMobs++;
					if (_killMobs == 6)
					{
						openCloseDoor(false, 25150042, 25150043, 25150045, 25150046);
						for (int i = 25150061; i <= 25150070; i++)
							openCloseDoor(true, i);
							
						startQuestTimer("room2_spawn2", 1000, npc, null, false);
					}
					break;
				case 18334:
					if (Rnd.get(100) < 33)
						killer.addItem("Quest", BREAKING_ARROW, 1, npc, true); // Breaking Arrow
					if (_killMobs <= 14)
						_killMobs++;
					if (_killMobs == 14)
					{
				    	startQuestTimer("mobs_del", 100, npc, null, false);
						
						openCloseDoor(true, 25150045, 25150046);
						startQuestTimer("waiting", Config.WAIT_TIME_FRINTEZZA * 3, npc, null, false);
						cancelQuestTimers("room_final");
						startQuestTimer("clean", 1000, npc, null, false);
					}
					break;
			}
		}
		return super.onKill(npc, killer, isPet);
	}
	
	private static void openCloseDoor(boolean open, int... doorIds)
	{
		for (int doorId : doorIds)
			if (open)
				DoorTable.getInstance().getDoor(doorId).openMe();
			else
				DoorTable.getInstance().getDoor(doorId).closeMe();
	}
}