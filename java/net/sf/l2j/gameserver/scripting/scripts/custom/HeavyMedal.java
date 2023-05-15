package net.sf.l2j.gameserver.scripting.scripts.custom;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.SpawnLocation;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.ScriptManager;

/*
 *	@author: Gnacik
 *	Event 'Heavy Medal'
 *	http://www.lineage2.com/archive/2006/09/heavy_medal_1.html
 */
public class HeavyMedal extends Quest
{
	private boolean enable = true;
	private final static String qn = "HeavyMedal";
	private final static int CAT_ROY = 31228;
	private final static int CAT_WINNIE = 31229;
	private final static int GLITTERING_MEDAL = 6393;
	private L2Npc roy, winnie = null;
	private final static List<L2Skill> buffsMage = new ArrayList<>();
	private final static List<L2Skill> buffsFighter = new ArrayList<>();
	
	private static final int[][] _buffsMage =
	{
		{
			1259,
			4
		}, // Resist Shock
		{
			1035,
			4
		}, // Mental Shield
		{
			1204,
			2
		}, // Wind Walk
		{
			1062,
			2
		}, // Berserker Spirit
		{
			1040,
			3
		}, // Shield
		{
			1045,
			6
		}, // Bless the Body
		{
			1036,
			2
		}, // Magic Barrier
		{
			1389,
			3
		}, // Greater Shield
		{
			1259,
			4
		}, // Resist Shock
		{
			1035,
			4
		}, // Mental Shield
		{
			1085,
			3
		}, // Acumen
		{
			1078,
			6
		}, // Concentration
		{
			1303,
			2
		}, // Wild Magic
		{
			1059,
			3
		}, // Empower
		{
			1355,
			1
		}, // Prophecy of Water The spirit of an ancient wizard temporarily possesses the user. Consumes 5 spirit ores. none none
		{
			1363,
			1
		}
	// Chant of Victory The spirits of ancient heroes temporarily possess one's party members. Consumes 40 spirit ores. none none
	
	};
	private static final int[][] _buffsFighter =
	{
		{
			1259,
			4
		}, // Resist Shock
		{
			1035,
			4
		}, // Mental Shield
		{
			1204,
			2
		}, // Wind Walk
		{
			1040,
			3
		}, // Shield
		{
			1045,
			6
		}, // Bless the Body
		{
			1036,
			2
		}, // Magic Barrier
		{
			1388,
			3
		}, // Greater Might
		{
			1077,
			3
		}, // Focus
		{
			1068,
			3
		}, // Might
		{
			1086,
			2
		}, // Haste
		{
			1242,
			3
		}, // Death Whisper
		{
			1356,
			1
		}, // Prophecy of Fire The spirit of an ancient warrior temporarily possesses the user. Consumes 5 spirit ores. none none
		{
			1363,
			1
		}
	// Chant of Victory The spirits of ancient heroes temporarily possess one's party members. Consumes 40 spirit ores. none none
	};
	private final static int WIN_CHANCE = 50;
	
	private final static int[] MEDALS =
	{
		5,
		10,
		20,
		40
	};
	private final static int[] BADGES =
	{
		6399,
		6400,
		6401,
		6402
	};
	// Spawn Event Manager for evet "GlitteringMedals" 31228
	private static final SpawnLocation[] LOCATIONS_01 =
	{
		new SpawnLocation(-84512, 242954, -3735, 0),
		new SpawnLocation(-80332, 149772, -3044, 0),
		new SpawnLocation(-44358, -113754, -245, 0),
		new SpawnLocation(-14852, 123698, -3118, 0),
		new SpawnLocation(11307, 17768, -4580, 0),
		new SpawnLocation(18222, 145222, -3066, 0),
		new SpawnLocation(44462, -47580, -797, 0),
		new SpawnLocation(47164, 49387, -3065, 0),
		new SpawnLocation(82876, 14880, -3468, 0),
		new SpawnLocation(82714, 54140, -1496, 0),
		new SpawnLocation(87727, -142008, -1342, 0),
		new SpawnLocation(111276, 220001, -3671, 0),
		new SpawnLocation(114716, -178341, -826, 0),
		new SpawnLocation(115970, 76586, -2720, 0),
		new SpawnLocation(147688, 27352, -2204, 0),
		new SpawnLocation(148115, -56817, -2781, 0)
	};
	// Spawn Event Manager for evet "GlitteringMedals" 31229
	private static final SpawnLocation[] LOCATIONS_02 =
	{
		new SpawnLocation(-84515, 243009, -3735, 0),
		new SpawnLocation(-80292, 149817, -3070, 0),
		new SpawnLocation(-44361, -113804, -245, 0),
		new SpawnLocation(-14809, 123663, -3118, 0),
		new SpawnLocation(11266, 17729, -4580, 0),
		new SpawnLocation(18255, 145177, -3067, 0),
		new SpawnLocation(44410, -47590, -822, 0),
		new SpawnLocation(47169, 49439, -3060, 0),
		new SpawnLocation(82944, 147873, -3471, 0),
		new SpawnLocation(82644, 54141, -1496, 0),
		new SpawnLocation(87753, -141953, -1367, 0),
		new SpawnLocation(111234, 220042, -3671, 0),
		new SpawnLocation(114732, -178392, -826, 0),
		new SpawnLocation(115936, 76544, -2719, 0),
		new SpawnLocation(147633, 27351, -2204, 0),
		new SpawnLocation(148087, -56764, -2781, 0)
	};
	
	public HeavyMedal()
	{
		super(-1, "custom");
		
		_log.info("Cargando Heavy Medal Event...");
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
		
		// TODO: agregar todos los buffs que vamos a poner
		// separados por magos y fighters
		for (int i = 0; i < _buffsMage.length; i++)
			buffsMage.add(SkillTable.getInstance().getInfo(_buffsMage[i][0], _buffsMage[i][1]));
		for (int i = 0; i < _buffsFighter.length; i++)
			buffsFighter.add(SkillTable.getInstance().getInfo(_buffsFighter[i][0], _buffsFighter[i][1]));
		_log.info("Evento Heavy Medal cargado.");
		
		if (roy != null)
		{
			roy.deleteMe();
			roy = null;
		}
		if (winnie != null)
		{
			winnie.deleteMe();
			winnie = null;
		}
		for (SpawnLocation rloc : LOCATIONS_01)
			roy = addSpawn(CAT_ROY, rloc, false, 0, false);
		for (SpawnLocation wloc : LOCATIONS_02)
			winnie = addSpawn(CAT_WINNIE, wloc, false, 0, false);
		
		addStartNpc(CAT_ROY, CAT_WINNIE);
		addTalkId(CAT_ROY, CAT_WINNIE);
		addFirstTalkId(CAT_ROY, CAT_WINNIE);
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = "";
		QuestState st = player.getQuestState(qn);
		htmltext = event;
		
		int level = checkLevel(st);
		
		if (event.equalsIgnoreCase("game"))
		{
			if (st.getQuestItemsCount(GLITTERING_MEDAL) < MEDALS[level])
				return "31229-no.htm";
			return "31229-game.htm";
		}
		else if (event.equalsIgnoreCase("heads") || event.equalsIgnoreCase("tails"))
		{
			if (st.getQuestItemsCount(GLITTERING_MEDAL) < MEDALS[level])
				return "31229-" + event.toLowerCase() + "-10.htm";
			
			st.takeItems(GLITTERING_MEDAL, MEDALS[level]);
			
			if (Rnd.get(100) > WIN_CHANCE)
				level = 0;
			else
			{
				if (level > 0)
					st.takeItems(BADGES[level - 1], -1);
				st.giveItems(BADGES[level], 1);
				st.playSound("Itemsound.quest_itemget");
				level++;
			}
			return "31229-" + event.toLowerCase() + "-" + String.valueOf(level) + ".htm";
		}
		else if (event.equalsIgnoreCase("talk"))
			return String.valueOf(npc.getNpcId()) + "-lvl-" + String.valueOf(level) + ".htm";
		return htmltext;
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null)
		{
			Quest q = ScriptManager.getInstance().getQuest(qn);
			st = q.newQuestState(player);
		}
		return npc.getNpcId() + ".htm";
	}
	
	public int checkLevel(QuestState st)
	{
		int _lev = 0;
		if (st == null)
			return 0;
		else if (st.getQuestItemsCount(6402) > 0)
			_lev = 4;
		else if (st.getQuestItemsCount(6401) > 0)
			_lev = 3;
		else if (st.getQuestItemsCount(6400) > 0)
			_lev = 2;
		else if (st.getQuestItemsCount(6399) > 0)
			_lev = 1;
		
		return _lev;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		if (npc instanceof L2MonsterInstance)
		{
			// Dos rand para que sea distinto, y no tire buff junto a las medallas
			// aunque hay una pequena posibilidad de que suceda eso
			int medalRand = Rnd.get(100);
			int buffRand = Rnd.get(100);
			final L2MonsterInstance mob = (L2MonsterInstance) npc;
			if (mob.isRaid() || mob.isRaidMinion())
				return null;
			if (enable)
			{
				if (killer.getLevel() - mob.getLevel() < 9)
					chanceDropMedals(30, 5, killer, mob, medalRand);
				else
					chanceDropMedals(20, 1, killer, mob, medalRand);
			}
			if (killer.getLevel() <= 76)
				giveRandomBuff(killer, mob, buffRand);
		}
		return null;
	}
	
	private static void giveRandomBuff(L2PcInstance killer, L2MonsterInstance mob, int rand)
	{
		boolean isMage = killer.isMageClass();
		if (rand <= 5)
		{
			L2Skill buff = isMage ? buffsMage.get(Rnd.get(buffsMage.size() - 1)) : buffsFighter.get(Rnd.get(buffsFighter.size() - 1));
			mob.setTarget(killer);
			if (!killer.isGM())
				mob.broadcastNpcSay(killer.getName() + ": Tuviste mucha suerte! Lograste quedarte con mi bendicion: " + buff.getName()); // Toma, te doy este buff sin ofender queda re choto xD
			killer.getSkill(buff.getId(), buff.getLevel());
		}
	}
	
	private static void chanceDropMedals(int eventMedalChance, int gliteringMedalChance, L2PcInstance killer, L2MonsterInstance mob, int rand)
	{
		if (rand < eventMedalChance)
			mob.dropItem(killer, new IntIntHolder(6392, Rnd.get(1, 3)));
		if (rand < gliteringMedalChance)
			mob.dropItem(killer, new IntIntHolder(6393, 1));
	}
}