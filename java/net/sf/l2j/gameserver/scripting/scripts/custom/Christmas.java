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
package net.sf.l2j.gameserver.scripting.scripts.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.model.SpawnLocation;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

/**
 * @author Maxi
 */
public class Christmas extends Quest
{
	private static String qn = "Christmas";
	List<L2Npc> christmas = new ArrayList<>();
	List<L2Npc> santa_trainee = new ArrayList<>();
	
	boolean enable = false;
	
	private static final int SANTA_TRAINEE = 31863;
	
	private static final int STAR_ORNAMENT = 5556; // x4
	private static final int BEAD_ORNAMENT = 5557; // x4
	private static final int FIR_TREE_BRANCH = 5558; // x10
	private static final int FLOWER_POT = 5559; // x1
	
	private static final int CHRISTMAS_TREE = 5560;
	private static final int SPECIAL_CHRISTMAS_TREE = 5561;
	
	private static final int CHRISTMAS_TREE_NPC = 13007;
	
	// 13006 Christmas Tree 9C E8 A9 -1
	// 13007 Special Christmas Tree 9C E8 A9 -1
	
	private static final SpawnLocation[] LOC_SANTA =
	{
		new SpawnLocation(83480, 148888, -3400, 48319), // giran
		new SpawnLocation(44088, -48392, -792, 16141), // rune
		new SpawnLocation(147720, -56504, -2776, 50), // goddard
		new SpawnLocation(147448, 26904, -2200, 50)
	// aden
	// 147462,26906,-2204 //aden
	// 147704,-56492,-2781 //goddard
	};
	
	private static final SpawnLocation[] LOC_CHRISTMAS_TREE =
	{
		new SpawnLocation(83417, 148877, -3405, 0), // giran
		new SpawnLocation(43965, -48402, -797, 0), // rune
		new SpawnLocation(147717, -56642, -2781, 0), // goddard
		new SpawnLocation(147459, 26970, -2204, 0)
	// aden
	};
	
	public Christmas()
	{
		super(-1, "custom");
		if (enable)
		{
			// TODO Auto-generated constructor stub
			for (NpcTemplate template : NpcTable.getInstance().getAllNpcs())
				try
				{
					if (L2Attackable.class.isAssignableFrom(Class.forName("net.sf.l2j.gameserver.model.actor.instance." + template.getType() + "Instance")))
						addEventId(template.getNpcId(), EventType.ON_KILL);
				}
				catch (ClassNotFoundException ex)
				{
					_log.info("Class not found: " + template.getType() + "Instance");
				}
		
			for (SpawnLocation loc : LOC_SANTA)
				santa_trainee.add(addSpawn(SANTA_TRAINEE, loc, false, 0, false));
			for (SpawnLocation loc : LOC_CHRISTMAS_TREE)
			{
				L2Npc arbol = addSpawn(CHRISTMAS_TREE_NPC, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), false, 0, true);
				arbol.setTitle("L2 Timeus");
				christmas.add(arbol); // christmas.add(addSpawn(CHRISTMAS_TREE_NPC, loc, false, 0, false));
			}
		
			// christmas.add(addSpawn(13007, npc.getY() + 150, npc.getX() + 150, npc.getZ(), npc.getHeading(), false, 0, false));
			
			addStartNpc(SANTA_TRAINEE);
			addTalkId(SANTA_TRAINEE);
			addFirstTalkId(SANTA_TRAINEE);
			ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new RewardTask(), 3600000, 3600000);
			// se auto ejecuta cada una hora delay inicial de una hora tmb
		}
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		
		if (event.equalsIgnoreCase("general"))
		{
			if (st.getQuestItemsCount(STAR_ORNAMENT) >= 4 && st.getQuestItemsCount(BEAD_ORNAMENT) >= 4 && st.getQuestItemsCount(FIR_TREE_BRANCH) >= 10 && st.getQuestItemsCount(FLOWER_POT) >= 1)
			{
				st.takeItems(STAR_ORNAMENT, 4);
				st.takeItems(BEAD_ORNAMENT, 4);
				st.takeItems(FIR_TREE_BRANCH, 10);
				st.takeItems(FLOWER_POT, 1);
				st.giveItems(CHRISTMAS_TREE, 1);
				if (Rnd.get(100) < 50)
					st.giveItems(9253, 1);
				htmltext = "<html><body>Merry Christmas.</body></html>";
			}
			else
				htmltext = "31863-2.htm";
		}
		else if (event.equalsIgnoreCase("greater"))
		{
			if (st.getQuestItemsCount(CHRISTMAS_TREE) >= 10)
			{
				st.takeItems(CHRISTMAS_TREE, 10);
				st.giveItems(SPECIAL_CHRISTMAS_TREE, 1);
				if (Rnd.get(100) < 50)
					st.giveItems(9253, 1);
				htmltext = "<html><body>Merry Christmas.</body></html>";
			}
			else
				htmltext = "31863-3.htm";
		}
		else if (event.equalsIgnoreCase("give_hat"))
		{
			if (st.getQuestItemsCount(CHRISTMAS_TREE) >= 10)
			{
				st.takeItems(CHRISTMAS_TREE, 10);
				st.giveItems(7836, 1);
				if (Rnd.get(100) < 50)
					st.giveItems(9253, 1);
				htmltext = "<html><body>Merry Christmas.</body></html>";
			}
			else
				htmltext = "31863-4.htm";
		}
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null)
			st = newQuestState(player);
		
		return "31863.htm";
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		if (enable)
		if (npc instanceof L2MonsterInstance)
		{
			if (killer.isGM() && npc.getNpcId() == 20002)
				setReward();
			final L2MonsterInstance mob = (L2MonsterInstance) npc;
			if (mob.isRaid() || mob.isRaidMinion())
				return null;
			int rate = killer.getLevel() - mob.getLevel() < 9 ? 50 : 10;
			dropItems(mob, killer, STAR_ORNAMENT, rate, 1);
			dropItems(mob, killer, BEAD_ORNAMENT, rate, 1);
			dropItems(mob, killer, FIR_TREE_BRANCH, rate, 2);
			dropItems(mob, killer, FLOWER_POT, rate, 1);
		}
		
		return super.onKill(npc, killer, isPet);
	}
	
	private static void dropItems(L2MonsterInstance npc, L2PcInstance killer, int itemId, int chance, int amount)
	{
		if (Rnd.get(1000) < chance)
			npc.dropItem(killer, new IntIntHolder(itemId, Rnd.get(1, amount)));
	}
	
	static void dropItems(L2Npc npc, int itemId, int chance, int amount)
	{
		if (Rnd.get(100) < chance)
			dropItemNpc(npc, new IntIntHolder(itemId, Rnd.get(1, amount)));
	}
	
	private static ItemInstance dropItemNpc(L2Npc npc, IntIntHolder item)
	{
		int randDropLim = 70;
		
		ItemInstance ditem = null;
		for (int i = 0; i < item.getValue(); i++)
		{
			int newX = npc.getX() + Rnd.get(randDropLim * 2 + 1) - randDropLim;
			int newY = npc.getY() + Rnd.get(randDropLim * 2 + 1) - randDropLim;
			int newZ = Math.max(npc.getZ(), npc.getZ()) + 20;
			
			if (ItemTable.getInstance().getTemplate(item.getId()) != null)
			{
				ditem = ItemTable.getInstance().createItem("Loot", item.getId(), item.getValue(), null, npc);
				ditem.dropMe(npc, newX, newY, newZ);
			}
			else
				_log.log(Level.SEVERE, "Item doesn't exist so cannot be dropped. Item ID: " + item.getId());
		}
		return ditem;
	}
	
	public void setReward()
	{
		ThreadPoolManager.getInstance().scheduleGeneral(new RewardTask(), 1000); // Event()
	}
	
	private class RewardTask implements Runnable
	{
		/**
		 * 
		 */
		public RewardTask()
		{
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void run()
		{
			for (L2Npc mob : santa_trainee)
				if (mob != null)
					mob.broadcastPacket(new NpcSay(mob.getObjectId(), Say2.SHOUT, mob.getNpcId(), "Merry Christmas!!!"));
			for (L2Npc mob : christmas)
				if (mob != null)
				{
					dropItems(mob, STAR_ORNAMENT, 100, 1);
					dropItems(mob, BEAD_ORNAMENT, 100, 1);
					dropItems(mob, FIR_TREE_BRANCH, 100, 1);
					dropItems(mob, FIR_TREE_BRANCH, 100, 1);
					dropItems(mob, STAR_ORNAMENT, 30, 3);
					dropItems(mob, BEAD_ORNAMENT, 30, 3);
					dropItems(mob, FIR_TREE_BRANCH, 30, 3);
					dropItems(mob, FLOWER_POT, 30, 3);
					dropItems(mob, 9253, 30, 1);
				}
		}
	}
}
