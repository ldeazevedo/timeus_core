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

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.L2ClanMember;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q118_ToLeadAndBeLed extends Quest
{
	public final static String qn = "Q118_ToLeadAndBeLed";
	
	// Items
	private final int BLOOD = 8062;
	private final int LEG = 8063;
	
	// Npcs
	private final int PINTER = 30298;
	
	public Q118_ToLeadAndBeLed()
	{
		super(118, "To Lead and Be Led");
		
		setItemsIds(BLOOD, LEG);
		
		addStartNpc(PINTER);
		addTalkId(PINTER);
		
		addKillId(20919, 20920, 20921, 20927);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmlText = event;
		QuestState qs = player.getQuestState(qn);
		
		if (qs == null)
			return null;
		
		if ("30517-02.htm".equals(event))
		{
			qs.set("cond", "1");
			qs.setState(STATE_STARTED);
			qs.playSound(QuestState.SOUND_ACCEPT);
		}
		else if ("30517-05a.htm".equals(event))
		{
			if (qs.getQuestItemsCount(BLOOD) >= 10)
			{
				qs.takeItems(BLOOD, -1);
				qs.set("cond", "3");
				qs.set("settype", "1");
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			else
				htmlText = "30517-03.htm";
		}
		else if ("30517-05b.htm".equals(event))
		{
			if (qs.getQuestItemsCount(BLOOD) >= 10)
			{
				qs.takeItems(BLOOD, -1);
				qs.set("cond", "4");
				qs.set("settype", "2");
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			else
				htmlText = "30517-03.htm";
		}
		else if ("30517-05c.htm".equals(event))
		{
			if (qs.getQuestItemsCount(BLOOD) >= 10)
			{
				qs.takeItems(BLOOD, -1);
				qs.set("cond", "5");
				qs.set("settype", "3");
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			else
				htmlText = "30517-03.htm";
		}
		else if ("30517-09.htm".equals(event))
		{
			L2ClanMember cm_apprentice = qs.getPlayer().getClan().getClanMember(qs.getPlayer().getApprentice());
			if (cm_apprentice != null)
			{
				if (cm_apprentice.isOnline())
				{
					L2PcInstance apprentice = cm_apprentice.getPlayerInstance();
					if (apprentice != null)
					{
						QuestState ap_quest = apprentice.getQuestState(qn);
						if (ap_quest != null)
						{
							int ap_cond = ap_quest.getInt("cond");
							int crystals = 0;
							if (ap_cond == 3)
								crystals = 922;

							else if ((ap_cond == 4) || (ap_cond == 5))
								crystals = 771;
							if (qs.getQuestItemsCount(1458) >= crystals)
							{
								qs.takeItems(1458, crystals);
								ap_quest.set("cond", "6");
								qs.playSound(QuestState.SOUND_MIDDLE);
								ap_quest.playSound(QuestState.SOUND_MIDDLE);
								htmlText = "30517-10.htm";
							}
						}
					}
				}
			}
		}
		return htmlText;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance talker)
	{
		String htmlText = Quest.getNoQuestMsg();
		QuestState st = talker.getQuestState(qn);
		if (st == null)
		{
			return htmlText;
		}
		
		if (talker.getApprentice() > 0)
		{
			L2ClanMember cm_apprentice = talker.getClan().getClanMember(talker.getApprentice());
			if ((cm_apprentice != null) && cm_apprentice.isOnline())
			{
				L2PcInstance apprentice = cm_apprentice.getPlayerInstance();
				if (apprentice != null)
				{
					QuestState ap_quest = apprentice.getQuestState(qn);
					if (ap_quest != null)
					{
						int ap_cond = ap_quest.getInt("cond");
						if (ap_cond == 3)
							htmlText = "30517-09a.htm";
						else if (ap_cond == 4)
							htmlText = "30517-09b.htm";
						else if (ap_cond == 5)
							htmlText = "30517-09c.htm";
					}
				}
			}
			return htmlText;
		}
		
		switch (st.getState())
		{
			case STATE_CREATED:
				if ((talker.getClan() == null) || (talker.getLevel() < 19) || (talker.getSponsor() == 0))
				{
					htmlText = "30517-00.htm";
					st.exitQuest(true);
				}
				else if (talker.getPledgeType() == -1)
					htmlText = "30517-01.htm";
				break;
			case STATE_STARTED:
				int cond = st.getInt("cond");
				if (cond == 1)
					htmlText = "30517-03.htm";
				else if (cond == 2)
					htmlText = "30517-04.htm";
				else if (cond == 3)
					htmlText = "30517-05d.htm";
				else if (cond == 4)
					htmlText = "30517-05e.htm";
				else if (cond == 5)
					htmlText = "30517-05f.htm";
				else if (cond == 6)
				{
					htmlText = "30517-06.htm";
					st.set("cond", "7");
					st.playSound(QuestState.SOUND_MIDDLE);
				}
				else if (cond == 7)
					htmlText = "30517-07.htm";
				else if (cond == 8)
				{
					if (st.getQuestItemsCount(LEG) >= 8)
					{
						int setType = st.getInt("settype");
						htmlText = "30517-08.htm";
						st.takeItems(LEG, -1);
						
						if (setType == 1)
						{
							st.giveItems(7850, 1);
							st.giveItems(7851, 1);
							st.giveItems(7852, 1);
							st.giveItems(7853, 1);
						}
						else if (setType == 2)
						{
							st.giveItems(7850, 1);
							st.giveItems(7854, 1);
							st.giveItems(7855, 1);
							st.giveItems(7856, 1);
						}
						else if (setType == 3)
						{
							st.giveItems(7850, 1);
							st.giveItems(7857, 1);
							st.giveItems(7858, 1);
							st.giveItems(7859, 1);
						}
						
						st.exitQuest(false);
						st.playSound(QuestState.SOUND_FINISH);
					}
				}
				break;
			case STATE_COMPLETED:
				htmlText = getAlreadyCompletedMsg();
				break;
		}
		return htmlText;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState qs = null;
		switch (npc.getNpcId())
		{
			case 20919:
			case 20920:
			case 20921:
				qs = checkPlayerCondition(player, npc, "cond", "1");
				break;
			case 20927:
				qs = checkPlayerCondition(player, npc, "cond", "7");
				break;
		}
		if (qs == null)
			return null;
		
		int sponsor = player.getSponsor();
		if (sponsor == 0)
		{
			qs.exitQuest(true);
			return null;
		}
		
		// For mobs 20919, 20920, 20921
		int item = BLOOD;
		int chance = 90;
		int max = 10;
		boolean sponsorIsInRadius = true; // not required for cond = 1
		
		if (npc.getNpcId() == 20927)
		{
			item = LEG;
			chance = 100;
			max = 8;
		}
		
		int count = qs.getQuestItemsCount(item);
		
		if (qs.getInt("cond") == 7)
		{
			sponsorIsInRadius = false; // set false to verification
			L2ClanMember cmSponsor = player.getClan().getClanMember(sponsor);
			if (cmSponsor != null)
				if (cmSponsor.isOnline())
				{
					L2PcInstance sponsor2 = cmSponsor.getPlayerInstance();
					if (sponsor2 != null)
						if (player.isInsideRadius(sponsor2, 1100, true, false))
							sponsorIsInRadius = true;
				}
		}
		
		if (sponsorIsInRadius && (count < max) && (Rnd.get(100) < chance))
		{
			qs.giveItems(item, 1);
			if (count == (max - 1))
			{
				qs.set("cond", String.valueOf(qs.getInt("cond") + 1));
				qs.playSound(QuestState.SOUND_MIDDLE);
			}
			else
				qs.playSound(QuestState.SOUND_ITEMGET);
		}
		return null;
	}
}