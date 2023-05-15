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
package net.sf.l2j.gameserver.scripting.quests;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q115_TheOtherSideOfTruth extends Quest
{
	private static final String qn = "Q115_TheOtherSideOfTruth";
	
	//npcs
	private static final int Misa = 32018;
	private static final int Suspicious = 32019;
	private static final int Rafforty = 32020;
	private static final int Sculpture1 = 32021;
	private static final int Kierre = 32022;
	private static final int Sculpture2 = 32077;
	private static final int Sculpture3 = 32078;
	private static final int Sculpture4 = 32079;
	
	//items
	private static final int Letter = 8079;
	private static final int Letter2 = 8080;
	private static final int Tablet = 8081;
	private static final int Report = 8082;
	
	public Q115_TheOtherSideOfTruth()
	{
		super(115, "The Other Side Of Truth");
		
		setItemsIds(Letter, Letter2, Tablet, Report);
		
		addStartNpc(Rafforty);
		addTalkId(Rafforty, Misa, Sculpture1, Sculpture2, Sculpture3, Sculpture4, Kierre);
	}
	
	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player)
	{
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		final int npcId = npc.getNpcId();
		
		if (st == null)
			return htmltext;

		if (event == "32018-04.htm")
		{
			st.playSound("ItemSound.quest_middle");
			st.set("cond", "7");
			st.takeItems(Letter2, 1);
		}
		else if (event == "32020-02.htm")
		{
			st.setState(Quest.STATE_STARTED);
			st.playSound("ItemSound.quest_accept");
			st.set("cond", "1");
		}
		else if (event == "32020-05.htm")
		{
			st.playSound("ItemSound.quest_middle");
			st.set("cond", "3");
			st.takeItems(Letter, 1);
		}
		else if ((event == "32020-06.htm") || (event == "32020-08a.htm"))
		{
			st.exitQuest(true);
			st.playSound("ItemSound.quest_finish");
		}
		else if ((event == "32020-08.htm") || (event == "32020-07a.htm"))
		{
			st.playSound("ItemSound.quest_middle");
			st.set("cond", "4");
		}
		else if (event == "32020-12.htm")
		{
			st.playSound("ItemSound.quest_middle");
			st.set("cond", "5");
		}
		else if (event == "32020-16.htm")
		{
			st.playSound("ItemSound.quest_middle");
			st.set("cond", "10");
			st.takeItems(Report, 1);
		}
		else if (event == "32020-18.htm")
		{
			if (st.getQuestItemsCount(Tablet) == 0)
			{
				st.playSound("ItemSound.quest_middle");
				st.set("cond", "11");
				htmltext = "32020-19.htm";
			}
			else
			{
				st.exitQuest(false);
				st.playSound("ItemSound.quest_finish");
				st.giveItems(57, 115673);
				st.rewardExpAndSp(493595, 40442);
			}
		}
		else if (event == "32020-19.htm")
		{
			st.playSound("ItemSound.quest_middle");
			st.set("cond", "11");
		}
		else if (event == "32022-02.htm")
		{
			st.playSound("ItemSound.quest_middle");
			st.set("cond", "9");
			L2Npc man = addSpawn(Suspicious, 104562, -107598, -3688, 0, false, 4000, false);
			man.broadcastPacket(new NpcSay(man.getObjectId(), 0, man.getNpcId(), "We meet again."));
			startQuestTimer("2", 3700, man, player, false);
			st.giveItems(Report, 1);
		}
		else if (event == "Sculpture-04.htm")
		{
			st.set("talk", "1");
			htmltext = "Sculpture-05.htm";
			st.set(String.valueOf(npcId), "1");
		}
		else if (event == "Sculpture-04a.htm")
		{
			st.playSound("ItemSound.quest_middle");
			st.set("cond", "8");
			L2Npc man = addSpawn(Suspicious, 117890, -126478, -2584, 0, false, 4000, false);
			man.broadcastPacket(new NpcSay(man.getObjectId(), 0, man.getNpcId(), "This looks like the right place..."));
			startQuestTimer("1", 3700, man, player, false);
			htmltext = "Sculpture-04.htm";
			st.giveItems(Tablet, 1);
		}
		else if (event == "Sculpture-05.htm")
			st.set(String.valueOf(npcId), "1");
		else if (event == "1")
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), 0, npc.getNpcId(), "I see someone. Is this fate?"));
		else if (event == "2")
			npc.broadcastPacket(new NpcSay(npc.getObjectId(), 0, npc.getNpcId(), "Don't bother trying to find out more about me. Follow your own destiny."));
		
		return htmltext;
	}
	
	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player)
	{
		String htmltext = getNoQuestMsg();
		final QuestState st = player.getQuestState(qn);
		final int state = st.getState();
		final int npcId = npc.getNpcId();
		final int cond = st.getInt("cond");
		
		if (state == Quest.STATE_COMPLETED)
			htmltext = "<html><body>This quest has already been completed.</body></html>";
		else if (npcId == Rafforty)
		{
			if (state == Quest.STATE_CREATED)
			{
				if (st.getPlayer().getLevel() >= 53)
					htmltext = "32020-01.htm";
				else
				{
					htmltext = "32020-00.htm";
					st.exitQuest(true);
				}
			}
			else if (cond == 1)
				htmltext = "32020-03.htm";
			else if (cond == 2)
				htmltext = "32020-04.htm";
			else if (cond == 3)
				htmltext = "32020-05.htm";
			else if (cond == 4)
				htmltext = "32020-11.htm";
			else if (cond == 5)
			{
				htmltext = "32020-13.htm";
				st.playSound("ItemSound.quest_middle");
				st.giveItems(Letter2, 1);
				st.set("cond", "6");
			}
			else if (cond == 6)
				htmltext = "32020-14.htm";
			else if (cond == 9)
				htmltext = "32020-15.htm";
			else if (cond == 10)
				htmltext = "32020-17.htm";
			else if (cond == 11)
				htmltext = "32020-20.htm";
			else if (cond == 12)
			{
				htmltext = "32020-18.htm";
				st.exitQuest(false);
				st.playSound("ItemSound.quest_finish");
				st.giveItems(57, 60044);
			}
		}
		else if (npcId == Misa)
		{
			if (cond == 1)
			{
				htmltext = "32018-01.htm";
				st.giveItems(Letter, 1);
				st.playSound("ItemSound.quest_middle");
				st.set("cond", "2");
			}
			else if (cond == 2)
				htmltext = "32018-02.htm";
			else if (cond == 6)
				htmltext = "32018-03.htm";
			else if (cond == 7)
				htmltext = "32018-05.htm";
		}
		else if (npcId == Sculpture1)
		{
			if (cond == 7)
			{
				if (npcId == 1)
					htmltext = "Sculpture-02.htm";
				else if (st.getInt("talk") == 1)
					htmltext = "Sculpture-06.htm";
				else
					htmltext = "Sculpture-03.htm";
			}
			else if (cond == 8)
				htmltext = "Sculpture-04.htm";
			else if (cond == 11)
			{
				st.giveItems(Tablet, 1);
				st.playSound("ItemSound.quest_middle");
				st.set("cond", "12");
				htmltext = "Sculpture-07.htm";
			}
			else if (cond == 12)
				htmltext = "Sculpture-08.htm";
		}
		else if (npcId == Sculpture2)
		{
			if (cond == 7)
			{
				if (npcId == 1)
				
					htmltext = "Sculpture-02.htm";
				
				else if (st.getInt("talk") == 1)
					htmltext = "Sculpture-06.htm";
				else
					htmltext = "Sculpture-03.htm";
			}
			else if (cond == 8)
				htmltext = "Sculpture-04.htm";
			else if (cond == 11)
			{
				st.giveItems(Tablet, 1);
				st.playSound("ItemSound.quest_middle");
				st.set("cond", "12");
				htmltext = "Sculpture-07.htm";
			}
			else if (cond == 12)
				htmltext = "Sculpture-08.htm";
		}
		else if (npcId == Sculpture3)
		{
			if (cond == 7)
			{
				if (npcId == 1)
					htmltext = "Sculpture-02.htm";
				else
				{
					htmltext = "Sculpture-01.htm";
					st.set(String.valueOf(npcId), "1");
				}
			}
			else if (cond == 8)
				htmltext = "Sculpture-04.htm";
			else if (cond == 11)
			{
				st.giveItems(Tablet, 1);
				st.playSound("ItemSound.quest_middle");
				st.set("cond", "12");
				htmltext = "Sculpture-07.htm";
			}
			else if (cond == 12)
				htmltext = "Sculpture-08.htm";
		}
		else if (npcId == Sculpture4)
		{
			if (cond == 7)
			{
				if (npcId == 1)
					htmltext = "Sculpture-02.htm";
				else
					htmltext = "Sculpture-01.htm";
				st.set(String.valueOf(npcId), "1");
			}
			else if (cond == 8)
				htmltext = "Sculpture-04.htm";
			else if (cond == 11)
			{
				st.giveItems(Tablet, 1);
				st.playSound("ItemSound.quest_middle");
				st.set("cond", "12");
				htmltext = "Sculpture-07.htm";
			}
			else if (cond == 12)
				htmltext = "Sculpture-08.htm";
		}
		else if (npcId == Kierre)
			if (cond == 8)
				htmltext = "32022-01.htm";
			else if (cond == 9)
				htmltext = "32022-03.htm";
		
		return htmltext;
	}
}