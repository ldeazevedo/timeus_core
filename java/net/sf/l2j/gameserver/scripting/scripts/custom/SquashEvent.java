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

import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.ScriptManager;

/**
 * @author Gnacik
 * @version 1.0
 * Warning! Mostly that event is custom!
 */
public class SquashEvent extends Quest
{
	private static final int MANAGER = 31860;
	
	boolean enable = false;
	private static final int NECTAR_SKILL = 2005;
	
	private static final int[] CHRONO_LIST =
	{
		4202,
		5133,
		5817,
		7058,
		8350
	};
	
	private static final int[] SQUASH_LIST = {
		12774,12775,12776,
		12777,12778,12779,
		13016,13017
	};
	
	private static final String[] _NOCHRONO_TEXT = {
		"You cannot kill me without Chrono",
		"Hehe...keep trying...",
		"Nice try...",
		"Tired ?",
		"Go go ! haha..."
	};
	
	private static final String[] _CHRONO_TEXT = {
		"Arghh... Chrono weapon...",
		"My end is coming...",
		"Please leave me !",
		"Heeellpppp...",
		"Somebody help me please..."
	};
	private static final String[] _NECTAR_TEXT = {
		"Yummy... Nectar...",
		"Plase give me more...",
		"Hmmm.. More.. I need more...",
		"I will like you more if you give me more...",
		"Hmmmmmmm...",
		"My favourite..."
	};
	
	//Id del Mob, Id del Item, la chance del drop, y la cantidad maxima.
	private static final int[][] DROPLIST =
	{
		// must be sorted by npcId !
		// npcId, itemId, chance, cantidad maxima
		// Young Squash
		{ 12774,  1061, 800, 10 },		// Healing potion
		{ 12774,  6363, 700, 15 },		// Stolen Infernium Ore
		{ 12774,  1062, 600, 2 },		// Haste potion
		{ 12774,  1459, 400, 10 },		// Crystal c-grade
		{ 12774,  1458, 400, 15 },		// Crystal d-grade
		{ 12774,  1539, 250, 5 },		// Greater Healing potion

		// High Quality Squash
		{ 12775,  1539, 800, 15 },		// Greater Healing potion
		{ 12775,  1375, 700, 3 },		// Greater Swift Attack Potion
		{ 12775,  6036, 700, 3 },		// Greater Magic Haste Potion
		{ 12776,  6363, 500, 25 },		// Stolen Infernium Ore
		{ 12775,  1459, 500, 30 },		// Crystal c-grade
		{ 12775,  1458, 500, 40 },		// Crystal d-grade
		{ 12775,  955, 300, 3 },		// Scroll: Enchant Weapon (Grade D)
		// Low Quality Squash
		{ 12776,  1061, 800, 8 },		// Healing potion
		{ 12776,  1062, 700, 2 },		// Haste potion
		{ 12776,  6363, 600, 20 },		// Stolen Infernium Ore
		{ 12776,  1458, 500, 30 },		// Crystal d-grade
		{ 12776,  956, 200, 3 },		// Scroll: Enchant Armor (Grade D)
		
		// Large Young Squash
		{ 12777,  1061, 800, 20 },		// Healing potion
		{ 12777,  6363, 600, 25 },		// Stolen Infernium Ore
		{ 12777,  1374, 500, 3 },		// Greater Haste potion
		{ 12777,  1459, 200, 50 },		// Crystal c-grade
		{ 12777,  1458, 250, 100 },		// Crystal d-grade
		{ 12777,  956, 100, 2 },		// Scroll: Enchant Armor (Grade D)
		{ 12777,  955, 100, 2 },		// Scroll: Enchant Weapon (Grade D)
		{ 12777,  951, 50, 1 },			// Scroll: Enchant Weapon (Grade C)
		{ 12777,  952, 50, 1 },			// Scroll: Enchant Armor (Grade C)
		// High Quality Large
		{ 12778,  1539, 750, 20 },		// Greater Healing potion
		{ 12778,  1375, 700, 3 },		// Greater Swift Attack Potion
		{ 12778,  6036, 700, 5 },		// Greater Magic Haste Potion
		{ 12778,  6363, 600, 35 },		// Stolen Infernium Ore
		{ 12778,  1459, 300, 100 },		// Crystal c-grade
		{ 12779,  1458, 300, 150 },		// Crystal d-grade
		{ 12778,  955, 300, 3 },		// Scroll: Enchant Weapon (Grade D)
		{ 12778,  951, 150, 2 },		// Scroll: Enchant Weapon (Grade C)
		{ 12778,  947, 100, 1 },		// Scroll: Enchant Weapon (Grade B)
		// Low Quality Large
		{ 12779,  6035, 700, 10 },		// Magic Haste Potion
		{ 12779,  6363, 600, 25 },		// Stolen Infernium Ore
		{ 12779,  1459, 100, 20 },		// Crystal c-grade
		{ 12779,  1458, 300, 50 },		// Crystal d-grade
		{ 12779,  956, 200, 3 },		// Scroll: Enchant Armor (Grade D)
		{ 12779,  952, 100, 1 },		// Scroll: Enchant Armor (Grade C)
		{ 12779,  948, 50, 1 },			// Scroll: Enchant Armor (Grade B)
		
		// King
		{ 13016,  1540, 800, 5 },		// Quick Healing Potion
		{ 13016,  6363, 700, 50 },		// Stolen Infernium Ore
		{ 13016,  1540, 600, 10 },		// Quick Healing Potion
		{ 13016,  1460, 350, 30 },		// Crystal b-grade
		{ 13016,  1461, 250, 5 },		// Crystal a-grade
		{ 13016,  1461, 200, 10 },		// Crystal a-grade
		{ 13016,  2133, 250, 3 },		// Gemstone A	20%
		{ 13016,  2134, 150, 1 },		// Gemstone S	10%
		{ 13016,  2133, 100, 5 },		// Gemstone A	10%
		{ 13016,  2134, 50, 3 },		// Gemstone S	5%
		{ 13016,  729, 50, 1 },			// Scroll: Enchant Weapon (Grade A)
		{ 13016,  730, 50, 1 },			// Scroll: Enchant Armor (Grade A)
		{ 13016,  728, 200, 5 },		// Mana Potion
		{ 13016,  728, 100, 10 },		// Mana Potion
		
		// Emperor
		{ 13017,  1540, 700, 10 },		// Quick Healing Potion
		{ 13017,  6363, 600, 100 },		// Stolen Infernium Ore
		{ 13017,  1540, 500, 20 },		// Quick Healing Potion
		{ 13017,  1461, 300, 5 },		// Crystal a-grade
		{ 13017,  1461, 200, 10 },		// Crystal a-grade
		{ 13017,  1462, 150, 3 },		// Crystal: S Grade
		{ 13017,  1462, 100, 5 },		// Crystal: S Grade
		{ 13017,  2133, 150, 5 },		// Gemstone A
		{ 13017,  2134, 150, 1 },		// Gemstone S
		{ 13017,  2133, 100, 10 },		// Gemstone A
		{ 13017,  2134, 100, 5 },		// Gemstone S
		{ 13017,  729, 150, 1 },		// Scroll: Enchant Weapon (Grade A)
		{ 13017,  730, 150, 1 },		// Scroll: Enchant Armor (Grade A)
		{ 13017,  728, 350, 5 },		// Mana Potion
		{ 13017,  728, 250, 10 }		// Mana Potion
	};
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
		if (contains(SQUASH_LIST, npc.getNpcId()))
		{
			if (isPet)
			{
				noChronoText(npc);
				npc.setIsInvul(true);
				return null;
			}
			if (attacker.getActiveWeaponItem() != null && contains(CHRONO_LIST, attacker.getActiveWeaponItem().getItemId()))
			{
				ChronoText(npc);
				npc.setIsInvul(false);
				npc.getStatus().reduceHp(10, attacker);
				return null;
			}
			noChronoText(npc);
			npc.setIsInvul(true);
			return null;
		}
		return super.onAttack(npc, attacker, damage, isPet, skill);
	}
	
	@Override
	public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		if (contains(targets, npc) && contains(SQUASH_LIST, npc.getNpcId()) && (skill.getId() == NECTAR_SKILL))
		{
			switch (npc.getNpcId())
			{
				case 12774:
					randomSpawn(12775, 12776, npc);
					break;
				case 12777:
					randomSpawn(12778, 12779, npc);
					break;
				case 12775:
					randomSpawn(13016, npc);
					break;
				case 12778:
					randomSpawn(13017, npc);
					break;
			}
		}
		return super.onSkillSee(npc, caster, skill, targets, isPet);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		if (enable)
		{
			dropItem(npc, killer);
			if (npc instanceof L2MonsterInstance)
			{
				final L2MonsterInstance mob = (L2MonsterInstance) npc;
				if (mob.isRaid() || mob.isRaidMinion())
					return null;
				if (killer.getLevel() - mob.getLevel() < 9)
					dropPollen(mob, killer, 20, 4);
				else
					dropPollen(mob, killer, 5, 1);
				// <AllDrop Items="6391" Count="1,1" Chance="10%" />
			}
		}
		
		return super.onKill(npc, killer, isPet);
	}
	
	/**
	 * @param npc
	 * @param killer
	 * @param chance
	 * @param amount
	 */
	private static void dropPollen(L2MonsterInstance npc, L2PcInstance killer, int chance, int amount)
	{
		if (Rnd.get(100) < chance)
		{
			npc.dropItem(killer, new IntIntHolder(6391, Rnd.get(1, amount)));
			if (Rnd.get(100) < 5)
				npc.dropItem(killer, new IntIntHolder(Rnd.get(100) < 50 ? 6389 : 6390, 1));
		}
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		npc.setIsImmobilized(true);
		npc.disableCoreAI(true);
		return null;
	}
	
	private static final void dropItem(L2Npc mob, L2PcInstance player)
	{
		final int npcId = mob.getNpcId();
		final int chance = Rnd.get(1000);
		for (int i = 0; i < DROPLIST.length; i++)
		{
			int[] drop = DROPLIST[i];
			if (npcId == drop[0])
			{
				if (chance < drop[2])
				{
					((L2Attackable) mob).dropItem(player, new IntIntHolder(drop[1], Rnd.get(1, drop[3])));
					continue;
				}
			}
			if (npcId < drop[0])
				return; // not found
		}
	}
	
	private void randomSpawn(int lower, int higher, L2Npc npc)
	{
		int _random = Rnd.get(100);
		if (_random < 10)
			spawnNext(lower, npc);
		else if (_random < 30)
			spawnNext(higher, npc);
		else
			nectarText(npc);
	}
	
	private void randomSpawn(int npcId, L2Npc npc)
	{
		if (Rnd.get(100) < 10)
			spawnNext(npcId, npc);
		else
			nectarText(npc);
	}
	
	private static void ChronoText(L2Npc npc)
	{
		if (Rnd.get(100) < 5)
			npc.broadcastPacket(new CreatureSay(npc.getObjectId(), Say2.ALL, npc.getName(), _CHRONO_TEXT[Rnd.get(_CHRONO_TEXT.length)]));
	}
	
	private static void noChronoText(L2Npc npc)
	{
		if (Rnd.get(100) < 5)
			npc.broadcastPacket(new CreatureSay(npc.getObjectId(), Say2.ALL, npc.getName(), _NOCHRONO_TEXT[Rnd.get(_NOCHRONO_TEXT.length)]));
	}
	
	private static void nectarText(L2Npc npc)
	{
		if (Rnd.get(100) < 7)
			npc.broadcastPacket(new CreatureSay(npc.getObjectId(), Say2.ALL, npc.getName(), _NECTAR_TEXT[Rnd.get(_NECTAR_TEXT.length)]));
	}
	
	private void spawnNext(int npcId, L2Npc npc)
	{
		addSpawn(npcId, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 60000, true);
		npc.deleteMe();
	}
	
	public static <T> boolean contains(T[] array, T obj)
	{
		for (int i = 0; i < array.length; i++)
			if (array[i] == obj)
				return true;
		return false;
	}
	
	public static boolean contains(int[] array, int obj)
	{
		for (int i = 0; i < array.length; i++)
			if (array[i] == obj)
				return true;
		return false;
	}
	
	public SquashEvent()
	{
		super(-1, "custom");
		if (enable)
		{
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
			
			for (int mob : SQUASH_LIST)
			{
				addAttackId(mob);
				addKillId(mob);
				addSpawnId(mob);
				addSkillSeeId(mob);
			}
			
			addStartNpc(MANAGER);
			addFirstTalkId(MANAGER);
			addTalkId(MANAGER);
			
			addSpawn(MANAGER, 83077, 147910, -3471, 29412, false, 0, false);
		}
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		if (st == null)
		{
			Quest q = ScriptManager.getInstance().getQuest(getName());
			st = q.newQuestState(player);
		}
		htmltext = npc.getNpcId() + ".htm";
		return htmltext;
	}
	
/*
<Event ID="Squash Event" Active="24 Dec 2009-26 Dec 2012"> 
<Droplist>
	<AllDrop Items="6391" Count="1,4" Chance="20%" />	<!-- Nectar -->
	<AllDrop Items="10640" Count="1,2" Chance="3%" />	<!-- Cloned Squash Seed -->
	<AllDrop Items="10641" Count="1,1" Chance="1%" />	<!-- Cloned Large Squash Seed -->
</Droplist>
<Message Type="OnJoin" Msg="Squash Event started!" />
<EventEnd>
	<Take From="AllPlayers">
		<Item ItemID="6391,10640,10641"></Item>
	</Take>
</EventEnd>
</Event>
*/

/*
private static final String[] _NOCHRONO_TEXT = {
       "No puedes matarme sin una Chrono",
       "Jejeje...sigue sigue...",
       "Buen intento...",
       "No te cansas ?",
       "Jajajjaj ! Mas fuerte ..."
   };
  
   private static final String[] _CHRONO_TEXT = {
       "Arghh... una Chrono...",
       "Mi final se acerca ...",
       "por favor dejame !",
       "Ayuda !! ...",
       "Que alguien me ayude !!..."
   };
   private static final String[] _NECTAR_TEXT = {
       "Hummm... Nectar...",
       "Si! Dame mas!...",
       "Hmmm.. Mas.. necesito mas!...",
       "Sigue, Sigue! Sigue! OH! SIGUE!...",
       "Hmmmmmmm...",
       "Es mi elixir favorito !..."
   };*/
/*

EAT_TEXT = ["Mas... Dame mas...",
      "Mmmm, que nectar rico.",
      "Estoy creciendo...",
      "Dame mas!",
      "Necesito maaass."]
 
CHRONO_TEXT = ["Arghhh... Tenes un arma Chrono.",
      "Mi fin se acerca...",
      "Siento dolor....",
      "No.. No.. por favor no me mates."]
 
NOCHRONO_TEXT = ["Jajaja! No podes matarme.",
      "Es imposible matarme sin un arma Chrono.",
      "Solo puedo ser matada con un arma Chrono!",
      "Jajajaja... segui intentando..."]*/
}