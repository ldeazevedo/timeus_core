package net.sf.l2j.gameserver.scripting.quests;

import java.util.HashMap;
import java.util.Map;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

public class Q990_L2Timeus extends Quest
{
	private static final String qn = "Q990_L2Timeus";
	// NPCs
	private final static int Roxy = 50015;
	// Items
	private final static int Stolen_Infernium_Ore = 6363;
	
	private final static int Minima_cantidad = 100;
	private static final Map<Integer, Integer> dropchances = new HashMap<>();
	{
		dropchances.put(20670, 50000);
		dropchances.put(20671, 50000);
		dropchances.put(20954, 50000);
		dropchances.put(20956, 50000);
		dropchances.put(20958, 50000);
		dropchances.put(20959, 50000);
		dropchances.put(20960, 50000);
		dropchances.put(20964, 50000);
		dropchances.put(20969, 50000);
		dropchances.put(20967, 50000);
		dropchances.put(20970, 50000);
		dropchances.put(20971, 50000);
		dropchances.put(20974, 50000);
		dropchances.put(20975, 50000);
		dropchances.put(21001, 50000);
		dropchances.put(21003, 50000);
		dropchances.put(21005, 50000);
		dropchances.put(21020, 50000);
		dropchances.put(21021, 50000);
		dropchances.put(21259, 50000);
		dropchances.put(21089, 50000);
		dropchances.put(21108, 50000);
		dropchances.put(21110, 50000);
		dropchances.put(21113, 50000);
		dropchances.put(21114, 50000);
		dropchances.put(21116, 50000);
	}
	
	public final static int[][] Rewards_Win =
	{
		{
			5529,
			10
		}, // dragon_slayer_edge
		{
			5532,
			10
		}, // meteor_shower_head
		{
			5533,
			10
		}, // elysian_head
		{
			5534,
			10
		}, // soul_bow_shaft
		{
			5535,
			10
		}, // carnium_bow_shaft
		{
			5536,
			10
		}, // bloody_orchid_head
		{
			5537,
			10
		}, // soul_separator_head
		{
			5538,
			10
		}, // dragon_grinder_edge
		{
			5539,
			10
		}, // blood_tornado_edge
		{
			5541,
			10
		}, // tallum_glaive_edge
		{
			5542,
			10
		}, // halbard_edge
		{
			5543,
			10
		}, // dasparion_s_staff_head
		{
			5544,
			10
		}, // worldtree_s_branch_head
		{
			5545,
			10
		}, // dark_legion_s_edge_edge
		{
			5546,
			10
		}, // sword_of_miracle_edge
		{
			5547,
			10
		}, // elemental_sword_edge
		{
			5548,
			10
		}, // tallum_blade_edge
		{
			8331,
			10
		}, // Infernal Master Blade
		{
			8341,
			10
		}, // Spiritual Eye Piece
		{
			8342,
			10
		}, // Flaming Dragon Skull Piece
		{
			8346,
			10
		}, // Hammer Piece of Destroyer
		{
			8349,
			10
		}, // Doom Crusher Head
		{
			8712,
			10
		}, // Sirra's Blade Edge
		{
			8713,
			10
		}, // Sword of Ipos Blade
		{
			8714,
			10
		}, // Barakiel's Axe Piece
		{
			8715,
			10
		}, // Behemoth's Tuning Fork Piece
		{
			8716,
			10
		}, // Naga Storm Piece
		{
			8717,
			10
		}, // Tiphon's Spear Edge
		{
			8718,
			10
		}, // Shyeed's Bow Shaft
		{
			8719,
			10
		}, // Sobekk's Hurricane Edge
		{
			8720,
			10
		}, // Themis' Tongue Piece
		{
			8721,
			10
		}, // Cabrio's Hand Head
		{
			8722,
			10
		}, // Daimon Crystal Fragment
	};
	
	public final static int[][] Rewards_Lose =
	{
		{
			5529,
			4
		}, // dragon_slayer_edge
		{
			5532,
			4
		}, // meteor_shower_head
		{
			5533,
			4
		}, // elysian_head
		{
			5534,
			4
		}, // soul_bow_shaft
		{
			5535,
			4
		}, // carnium_bow_shaft
		{
			5536,
			4
		}, // bloody_orchid_head
		{
			5537,
			4
		}, // soul_separator_head
		{
			5538,
			4
		}, // dragon_grinder_edge
		{
			5539,
			4
		}, // blood_tornado_edge
		{
			5541,
			4
		}, // tallum_glaive_edge
		{
			5542,
			4
		}, // halbard_edge
		{
			5543,
			4
		}, // dasparion_s_staff_head
		{
			5544,
			4
		}, // worldtree_s_branch_head
		{
			5545,
			4
		}, // dark_legion_s_edge_edge
		{
			5546,
			4
		}, // sword_of_miracle_edge
		{
			5547,
			4
		}, // elemental_sword_edge
		{
			5548,
			4
		}, // tallum_blade_edge
		{
			8331,
			4
		}, // Infernal Master Blade
		{
			8341,
			4
		}, // Spiritual Eye Piece
		{
			8342,
			4
		}, // Flaming Dragon Skull Piece
		{
			8346,
			4
		}, // Hammer Piece of Destroyer
		{
			8349,
			4
		}, // Doom Crusher Head
		{
			8712,
			4
		}, // Sirra's Blade Edge
		{
			8713,
			4
		}, // Sword of Ipos Blade
		{
			8714,
			4
		}, // Barakiel's Axe Piece
		{
			8715,
			4
		}, // Behemoth's Tuning Fork Piece
		{
			8716,
			4
		}, // Naga Storm Piece
		{
			8717,
			4
		}, // Tiphon's Spear Edge
		{
			8718,
			4
		}, // Shyeed's Bow Shaft
		{
			8719,
			4
		}, // Sobekk's Hurricane Edge
		{
			8720,
			4
		}, // Themis' Tongue Piece
		{
			8721,
			4
		}, // Cabrio's Hand Head
		{
			8722,
			4
		}, // Daimon Crystal Fragment
	};
	
	public Q990_L2Timeus()
	{
		super(990, "Timeus");
		addStartNpc(Roxy);
		addTalkId(Roxy);
		
		for (int kill_id : dropchances.keySet())
			addKillId(kill_id);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;
		
		int level = st.getInt("level");
		if (event.equalsIgnoreCase("05.htm"))
		{
			st.setState(STATE_STARTED);
			st.set("cond", "1");
			st.set("level", "0");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("08.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
		}
		else if (event.equalsIgnoreCase("multisell"))
		{
			// if (st.getQuestItemsCount(Stolen_Infernium_Ore) < 1000)
			// return "11.htm";
			switch (level)
			{
				case 0:
					htmltext = "multisell-00.htm";
					break;
				case 1:
					htmltext = "multisell-01.htm";
					break;
				case 2:
					htmltext = "multisell-02.htm";
					break;
				case 3:
					htmltext = "multisell-03.htm";
					break;
			}
		}
		else if (event.equalsIgnoreCase("levelup"))
		{
			switch (level)
			{
				case 0:
					if (st.getQuestItemsCount(Stolen_Infernium_Ore) < Minima_cantidad)
					{
						player.sendMessage("Son necesario 100 Stolen Infernium ore");
						return "11.htm";
					}
					st.takeItems(Stolen_Infernium_Ore, Minima_cantidad);
					st.set("level", "1");
					htmltext = "levelup.htm";
					break;
				case 1:
					if (st.getQuestItemsCount(Stolen_Infernium_Ore) < 200)
					{
						player.sendMessage("Son necesario 200 Stolen Infernium ore");
						return "11.htm";
					}
					st.takeItems(Stolen_Infernium_Ore, 200);
					st.set("level", "2");
					htmltext = "levelup.htm";
					break;
				case 2:
					if (st.getQuestItemsCount(Stolen_Infernium_Ore) < 300)
					{
						player.sendMessage("Son necesario 300 Stolen Infernium ore");
						return "11.htm";
					}
					st.takeItems(Stolen_Infernium_Ore, 300);
					st.set("level", "3");
					htmltext = "levelup.htm";
					break;
			}
		}
		else if (event.equalsIgnoreCase("random_reward"))
		{
			if (st.getQuestItemsCount(Stolen_Infernium_Ore) < Minima_cantidad)
				return "11.htm";
			st.takeItems(Stolen_Infernium_Ore, Minima_cantidad);
			if (Rnd.get(10) == 3)
				reward(st, Rewards_Win);
			else
				reward(st, Rewards_Lose);
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;
		
		if (st.getState() == STATE_CREATED)
		{
			if (st.getPlayer().getLevel() < 55)
			{
				st.exitQuest(true);
				return "04.htm";
			}
			return "01.htm";
		}
		return st.getQuestItemsCount(Stolen_Infernium_Ore) < Minima_cantidad ? "06.htm" : "07.htm";
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = checkPlayerCondition(player, npc, "cond", "1");
		if (st == null)
			return null;
		
		st.dropItems(Stolen_Infernium_Ore, 1, 0, dropchances.get(npc.getNpcId()));
		
		return null;
	}
	
	private static void reward(QuestState st, int[][] rew)
	{
		int[] r = rew[Rnd.get(rew.length)];
		st.giveItems(r[0], r[1]);
	}
}