/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.l2j.gameserver.scripting.quests;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.SpawnLocation;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.QuestTimer;

public class Q501_ProofOfClanAlliance extends Quest
{
	private static final String qn = "Q501_ProofOfClanAlliance";
	
	// Quest Npcs
	private static final int SIR_KRISTOF_RODEMAI = 30756;
	private static final int STATUE_OF_OFFERING = 30757;
	private static final int WITCH_ATHREA = 30758;
	private static final int WITCH_KALIS = 30759;

	// Monsters
	private static final int OEL_MAHUM_WITCH_DOCTOR = 20576;
	private static final int HARIT_LIZARDMAN_SHAMAN = 20644;
	private static final int VANOR_SILENOS_SHAMAN = 20685;
	
	// Quest Items
	private static final int HERB_OF_HARIT = 3832;
	private static final int HERB_OF_VANOR = 3833;
	private static final int HERB_OF_OEL_MAHUM = 3834;
	private static final int BLOOD_OF_EVA = 3835;
	private static final int SYMBOL_OF_LOYALTY = 3837;
	private static final int PROOF_OF_ALLIANCE = 3874;
	private static final int VOUCHER_OF_FAITH = 3873;
	private static final int ANTIDOTE_RECIPE = 3872;
	private static final int POTION_OF_RECOVERY = 3889;
	
	// Quest mobs, drop, rates and prices
	private static final int[] CHESTS =
	{
		27173,
		27174,
		27175,
		27176,
		27177
	};
	
	private static final SpawnLocation[] CHESTS_LOCATION =
	{
		new SpawnLocation(102273, 103433, -3512, 0),
		new SpawnLocation(102190, 103379, -3524, 0),
		new SpawnLocation(102107, 103325, -3533, 0),
		new SpawnLocation(102024, 103271, -3500, 0),
		new SpawnLocation(102327, 103350, -3511, 0),
		new SpawnLocation(102244, 103296, -3518, 0),
		new SpawnLocation(102161, 103242, -3529, 0),
		new SpawnLocation(102078, 103188, -3500, 0),
		new SpawnLocation(102381, 103267, -3538, 0),
		new SpawnLocation(102298, 103213, -3532, 0),
		new SpawnLocation(102215, 103159, -3520, 0),
		new SpawnLocation(102132, 103105, -3513, 0),
		new SpawnLocation(102435, 103184, -3515, 0),
		new SpawnLocation(102352, 103130, -3522, 0),
		new SpawnLocation(102269, 103076, -3533, 0),
		new SpawnLocation(102186, 103022, -3541, 0)
	};
	
	private static final int[][] MOBS =
	{
		{
			VANOR_SILENOS_SHAMAN,
			HERB_OF_VANOR
		},
		{
			HARIT_LIZARDMAN_SHAMAN,
			HERB_OF_HARIT
		},
		{
			OEL_MAHUM_WITCH_DOCTOR,
			HERB_OF_OEL_MAHUM
		}
	};

	private static final int CLAN_MEMBER_MIN_LEVEL = 40;
	
	private static final int RATE = 10;
	// stackable items paid to retry chest game: (default 10k adena)
	private static final int RETRY_PRICE = 10000;
	// Id of Poison Skill
	private static final IntIntHolder POISON_OF_DEATH = new IntIntHolder(4082, 1);
//	private static final IntIntHolder DIE_YOU_FOOL = new IntIntHolder(4083, 1);
	private static final List<L2Npc> _chests = new ArrayList<>();
	
	public Q501_ProofOfClanAlliance()
	{
		super(501, "Proof of Clan Alliance");
		
		addStartNpc(SIR_KRISTOF_RODEMAI, STATUE_OF_OFFERING, WITCH_ATHREA);
		addTalkId(WITCH_KALIS, SIR_KRISTOF_RODEMAI, WITCH_ATHREA, STATUE_OF_OFFERING);
		
		setItemsIds(SYMBOL_OF_LOYALTY, ANTIDOTE_RECIPE, HERB_OF_VANOR, HERB_OF_HARIT, HERB_OF_OEL_MAHUM, BLOOD_OF_EVA);
		
		for (int[] i : MOBS)
			addKillId(i[0]);
		
		for (int i : CHESTS)
			addKillId(i);
	}

	/**
	 * Gets the clan leader's quest state.
	 * @param st 
	 * @return the clan leader's quest state
	 */
	private static QuestState getLeaderQuestState(QuestState st)
	{
		if (st.getPlayer().getClan() != null)
		{
			final L2PcInstance leader = st.getPlayer().getClan().getLeader().getPlayerInstance();
			if (leader != null && leader.isOnline())
				return leader.getQuestState(qn);
		}
		return null;
	}
	
	public void removeQuestFromMembers(QuestState st, boolean leader)
	{
		removeQuestFromOfflineMembers(st);
		removeQuestFromOnlineMembers(st, leader);
	}
	
	public static final void removeQuestFromOfflineMembers(QuestState st)
	{
		if (st.getPlayer() == null || st.getPlayer().getClan() == null)
			return;
		
		int clan = st.getPlayer().getClan().getClanId();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement offline = con.prepareStatement("DELETE FROM character_quests WHERE name = ? AND charId IN (SELECT obj_Id FROM characters WHERE clanid = ? AND online = 0)");
			offline.setString(1, qn);
			offline.setInt(2, clan);
			offline.executeUpdate();
			offline.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, qn + e.getMessage(), e);
		}
	}
	
	public void removeQuestFromOnlineMembers(QuestState st, boolean leader)
	{
		if (st == null || st.getPlayer() == null || st.getPlayer().getClan() == null)
			return;
		
		QuestState stleader;
		L2PcInstance pleader = null;
		
		if (leader)
		{
			stleader = getLeaderQuestState(st);
			if (stleader != null)
				pleader = stleader.getPlayer();
		}
		
		for (L2PcInstance pl : st.getPlayer().getClan().getOnlineMembers())
			if (pl != null && pl.getQuestState(qn) != null)
				pl.getQuestState(qn).exitQuest(true);
		
		if (pleader != null)
		{
			L2Effect[] effects = pleader.getAllEffects();
			if (effects.length != 0)
			{
				for (L2Effect e : effects)
				{
					if (e == null)
						continue;
					if (e.getSkill() == POISON_OF_DEATH.getSkill())
					{	
						pleader.removeEffect(e);
						e.exit();
						return;
					}
				}
			}
			// pleader.getQuestState(qn) esta tirando null, hacemos check
			if (pleader.getQuestState(qn) != null)
				pleader.getQuestState(qn).exitQuest(true);
		}
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		
		if (st == null)
			return htmltext;
		
		if (st.getPlayer() == null || st.getPlayer().getClan() == null)
			return null;
		
		QuestState leader = getLeaderQuestState(st);
		if (leader == null)
		{
			removeQuestFromMembers(st, true);
			return "Quest Failed";
		}
		
		if (event.equalsIgnoreCase("30756-03.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30759-03.htm"))
		{
			st.set("cond", "2");
			st.set("dead_list", " ");
		}
		else if (event.equalsIgnoreCase("30759-07.htm"))
		{
			st.takeItems(SYMBOL_OF_LOYALTY, 1);
			st.takeItems(SYMBOL_OF_LOYALTY, 1);
			st.takeItems(SYMBOL_OF_LOYALTY, 1);
			st.giveItems(ANTIDOTE_RECIPE, 1);
			st.addNotifyOfDeath();
			st.set("cond", "3");
			st.set("chest_count", "0");
			st.set("chest_game", "0");
			st.set("chest_try", "0");
			startQuestTimer("poison_timer", 3600000, null, st.getPlayer(), false);
			npc.setTarget(player);
			npc.doCast(POISON_OF_DEATH.getSkill());
			htmltext = "30759-07.htm";
		}
		else if (event.equalsIgnoreCase("30757-04.htm"))
		{
			List<String> deadlist = new ArrayList<>();
			deadlist.addAll(Arrays.asList(leader.get("dead_list").toString().split(" ")));
			deadlist.add(player.getName());
			String deadstr = "";
			for (String s : deadlist)
				deadstr += s + " ";
			leader.set("dead_list", deadstr);
			if (Rnd.get(10) < 5)
			{
			//	npc.setTarget(player);
			//	npc.doCast(DIE_YOU_FOOL.getSkill());
				player.reduceCurrentHp(player.getCurrentHp() * 8, player, null);
			}
			st.giveItems(SYMBOL_OF_LOYALTY, 1);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30757-05.htm"))
			st.exitQuest(true);
		else if (event.equalsIgnoreCase("30758-03.htm"))
		{
			leader.set("chest_game", "1");
			leader.set("chest_count", "0");
			int attempts = leader.getInt("chest_try");
			leader.set("chest_try", String.valueOf(attempts + 1));
			
			for (SpawnLocation loc : CHESTS_LOCATION)
			{
				// Al azar elijo una de las cajas, cualquiera que sea y que salga solo una por LOC.
				L2Npc chest = addSpawn(CHESTS[Rnd.get(CHESTS.length)], loc, false, 60000, false);
				chest.getAI().setIntention(CtrlIntention.IDLE, null, null);
				((L2Attackable) chest).disableCoreAI(true);
				((L2Attackable) chest).setIsNoRndWalk(true);
				_chests.add(chest);
				startQuestTimer("chest_timer", 60000, chest, st.getPlayer(), false);
			}
		}
		else if (event.equalsIgnoreCase("30758-07.htm"))
		{
			if (st.getQuestItemsCount(57) < RETRY_PRICE)
				htmltext = "30758-06.htm";
			else
				st.takeItems(57, RETRY_PRICE);
		}
		// Timers
		else if (event.equalsIgnoreCase("poison_timer"))
		{
			if (hasAbnormal(player))
				removeQuestFromMembers(st, true);
			htmltext = "30759-09.htm";
		}
		else if (event.equalsIgnoreCase("chest_timer"))
		{
			htmltext = "";
			if (leader.getInt("chest_game") < 2)
				startQuestTimer("stop_chest_game", 1000, npc, st.getPlayer(), false);
		}
		else if (event.equalsIgnoreCase("stop_chest_game"))
		{
			htmltext = "";
			for (int i = 0; i < _chests.size(); i++)
			{
				final L2Npc mob = _chests.get(i);
				if (mob != null)
					mob.decayMe();
			}
			_chests.clear();
			leader.set("chest_game", "0");
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		int cond = st.getInt("cond");
		QuestState leader = getLeaderQuestState(st);
		
		int npcId = npc.getNpcId();
		switch (npcId)
		{
			case SIR_KRISTOF_RODEMAI:
				if (st.getState() == STATE_CREATED)
				{
					if (!st.getPlayer().isClanLeader())
						htmltext = "30756-10.htm";
					else if (st.getPlayer().getClan().getLevel() <= 2)
						htmltext = "30756-08.htm";
					else if (st.getPlayer().getClan().getLevel() >= 4)
						htmltext = "30756-09.htm";
					else if (st.getQuestItemsCount(PROOF_OF_ALLIANCE) == 0)
						htmltext = "30756-01.htm";
				}
				if (st.getQuestItemsCount(VOUCHER_OF_FAITH) > 0)
				{
					st.playSound(QuestState.SOUND_FANFARE);
					st.takeItems(VOUCHER_OF_FAITH, -1);
					st.giveItems(PROOF_OF_ALLIANCE, 1);
					st.rewardExpAndSp(0, 120000);
					htmltext = "30756-07.htm";
					st.exitQuest(true);
				}
				else if (cond == 1 || cond == 2)
					htmltext = "30756-06.htm";
				break;
			case WITCH_KALIS:
				if (st.getPlayer().isClanLeader())
				{
					if (cond == 1)
						htmltext = "30759-01.htm";
					else if (cond == 2)
					{
						htmltext = "30759-05.htm";
						
						if (st.getQuestItemsCount(SYMBOL_OF_LOYALTY) == 3)
						{
							int deads = 0;
							try
							{
								deads = st.get("dead_list").toString().split(" ").length;
							}
							finally
							{
								if (deads == 3)
									htmltext = "30759-06.htm";
							}
						}
					}
					else if (cond == 3)
					{
						if (st.hasQuestItems(HERB_OF_HARIT, HERB_OF_VANOR, HERB_OF_OEL_MAHUM, BLOOD_OF_EVA, ANTIDOTE_RECIPE) && hasAbnormal(player))
						{
							st.takeItems(ANTIDOTE_RECIPE, 1);
							st.takeItems(HERB_OF_HARIT, 1);
							st.takeItems(HERB_OF_VANOR, 1);
							st.takeItems(HERB_OF_OEL_MAHUM, 1);
							st.takeItems(BLOOD_OF_EVA, 1);
							st.giveItems(POTION_OF_RECOVERY, 1);
							st.giveItems(VOUCHER_OF_FAITH, 1);
							cancelQuestTimer("poison_timer", null, leader.getPlayer());
							removeQuestFromMembers(st, false);
							st.set("cond", "4");
							st.playSound(QuestState.SOUND_FINISH);
							htmltext = "30759-08.htm";
						}
						else if (st.hasQuestItems(VOUCHER_OF_FAITH))
							htmltext = "30759-10.htm";
					}
				}
				else if (leader.getInt("cond") == 3)
					htmltext = "30759-11.htm";
				break;
			case STATUE_OF_OFFERING:
				if (st.getPlayer().isClanLeader())
					htmltext = "30757-03.htm";
				else if (st.getPlayer().getLevel() < CLAN_MEMBER_MIN_LEVEL)
					htmltext = "30757-02.htm";
				else
				{
					String[] dlist;
					int deads;
					try
					{
						dlist = leader.get("dead_list").toString().split(" ");
						deads = dlist.length;
					}
					catch (Exception e)
					{
						removeQuestFromMembers(st, true);
						return "Who are you?";
					}
					if (deads < 3)
					{
						for (String str : dlist)
							if (st.getPlayer().getName().equalsIgnoreCase(str))
								return "you cannot die again!";
						htmltext = "30757-01.htm";
					}
				}
				break;
			case WITCH_ATHREA:
				if (st.getPlayer().isClanLeader())
					htmltext = "30757-03.htm";
				else
				{
					String[] dlist;
					try
					{
						dlist = leader.get("dead_list").toString().split(" ");
					}
					catch (Exception e)
					{
						return "Who are you?";
					}
					Boolean flag = false;
					if (dlist != null)
						for (String str : dlist)
							if (st.getPlayer().getName().equalsIgnoreCase(str))
								flag = true;
					if (!flag)
						return "Who are you?";
					
					int game_state = leader.getInt("chest_game");
					if (game_state == 0)
					{
						if (leader.getInt("chest_try") == 0)
							return "30758-01.htm";
						
						htmltext = "30758-05.htm";
					}
					else if (game_state == 1)
						htmltext = "30758-09.htm";
					else if (game_state == 2)
					{
						st.giveItems(BLOOD_OF_EVA, 1);
						st.playSound(QuestState.SOUND_FINISH);
						cancelQuestTimer("chest_timer", null, leader.getPlayer());
						startQuestTimer("stop_chest_game", 1000, npc, st.getPlayer(), false);
						leader.set("chest_game", "3");
						htmltext = "30758-08.htm";
					}
				}
				break;
		}
		return htmltext;
	}

	/**
	 * Verifies if the player has the poison.
	 * @param player the player to check
	 * @return {@code true} if the player has POISON_OF_DEATH
	 */
	private static boolean hasAbnormal(L2PcInstance player)
	{
		L2Effect[] effects = player.getAllEffects();
		if (effects.length != 0)
			for (L2Effect e : effects)
				return e.getSkill() == POISON_OF_DEATH.getSkill();
		return false;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		QuestState st = killer.getQuestState(qn);
		
		if (st == null || st.getPlayer() == null || st.getPlayer().getClan() == null)
			return null;
		
		QuestState leader = getLeaderQuestState(st);
		if (leader == null)
		{
			removeQuestFromMembers(st, true);
			return "Quest Failed";
		}

		if (hasAbnormal(leader.getPlayer()))
		{
			// first part, general checking
			int npcId = npc.getNpcId();
	
			// second part, herbs gathering
			for (int[] m : MOBS)
			{
				if (npcId == m[0] && st.getInt(String.valueOf(m[1])) == 0)
				{
					if (Rnd.get(100) < RATE)
					{
						// Sucedia que si hacia continue en la linea 595 lo tomaba como NPC
						// y tiraba CastException
						// Solucion devolviendo null
						if (st.getPlayer().getInventory().hasAtLeastOneItem(m[1]))
							return null;
						st.giveItems(m[1], 1);
						leader.set(String.valueOf(m[1]), "1");
						st.playSound(QuestState.SOUND_MIDDLE);
						return null;
					}
				}
			}
			
			// third part, chest game
			for (int chest : CHESTS)
			{
				if (npcId == chest)
				{
					QuestTimer timer = getQuestTimer("chest_timer", null, st.getPlayer());
					if (timer == null)
					{
						startQuestTimer("stop_chest_game", 1000, npc, st.getPlayer(), false);
						return "Time is up!";
					}
					if (Rnd.get(100) < 25)
					{
						npc.broadcastPacket(new NpcSay(npc.getObjectId(), 0, npc.getNpcId(), "###### BINGO! ######"));
						st.playSound(QuestState.SOUND_MIDDLE);
						int count = leader.getInt("chest_count");
						if (count < 4)
						{
							count += 1;
							leader.set("chest_count", String.valueOf(count));
						}
						if (count >= 4)
						{
							leader.set("chest_game", "2");
							cancelQuestTimer("chest_timer", null, leader.getPlayer());
							st.playSound(QuestState.SOUND_MIDDLE);
							for (int i = 0; i < _chests.size(); i++)
							{
								final L2Npc mob = _chests.get(i);
								if (mob != null)
									mob.decayMe();
							}
							_chests.clear();
						}
						else
							continue;
					}
				}
			}
		}
		return null;
	}
	
	@Override
	public String onDeath(L2Character pc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null || st.getPlayer() == null || st.getPlayer().getClan() == null)
			return null;
		
		QuestState leader = getLeaderQuestState(st);
		if (leader == null)
		{
			removeQuestFromMembers(st, true);
			return null;
		}
		
		if (st.getPlayer() == player && st.getPlayer().isClanLeader())
		{
			cancelQuestTimer("poison_timer", null, leader.getPlayer());
			cancelQuestTimer("chest_timer", null, leader.getPlayer());
			removeQuestFromMembers(st, true);
		}
		return null;
	}
}