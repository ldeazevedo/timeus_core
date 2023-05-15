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

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.L2Summon;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;

/**
 * @author Maxi
 *
 */
public class NpcBuffer extends Quest
{
	private final static int BUFFER = 50017;
	private final static String qn = "NpcBuffer";

	public NpcBuffer()
	{
		super(-1, "custom");
		
		addStartNpc(BUFFER);
		addTalkId(BUFFER);
		addFirstTalkId(BUFFER);
		
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		L2Summon activeSummon = player.getPet() != null ? player.getPet() : null;
		boolean isMage = player.isMageClass();
		if (event.equalsIgnoreCase("nobless"))
		{
			player.getSkill(1323, 1); // 1323	1	Noblesse Blessing
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
		}
		if (event.equalsIgnoreCase("heal"))
		{
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			if (activeSummon != null)
				activeSummon.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		}
		if (event.equalsIgnoreCase("buff"))
		{
			player.getSkill(1259, 4); // Resist Shock
			player.getSkill(1035, 4); // Mental Shield
			player.getSkill(1204, 2); // Wind Walk
			player.getSkill(1040, 3); // Shield
			player.getSkill(1045, 6); // Bless the Body
			player.getSkill(1036, 2); // Magic Barrier
			player.getSkill(1062, 2); // Berserker Spirit
			if (!isMage)
			{
				player.getSkill(1388, 3); // Greater Might
				player.getSkill(1077, 3); // Focus
				player.getSkill(1068, 3); // Might
				player.getSkill(1086, 2); // Haste
				player.getSkill(1242, 3); // Death Whisper
			}
			else
			{
				player.getSkill(1389, 3); // Greater Shield
				player.getSkill(1085, 3); // Acumen
				player.getSkill(1078, 6); // Concentration
				player.getSkill(1303, 2); // Wild Magic
				player.getSkill(1059, 3); // Empower
			}
		}
		if (event.equalsIgnoreCase("dancer"))
		{
			if (!isMage)
			{
				player.getSkill(275, 1); // Fury
				player.getSkill(274, 1); // Fire
				player.getSkill(271, 1); // Warrior
			}
			else
			{
				player.getSkill(273, 1); // Mistyc
				player.getSkill(276, 1); // Concentration
				player.getSkill(365, 1); // Siren's Dance
			}
		}
		if (event.equalsIgnoreCase("songer"))
		{
			player.getSkill(267, 1); // Warding
			player.getSkill(264, 1); // Earth
			player.getSkill(268, 1); // Wind
			player.getSkill(304, 1); // Song of Vitality
			player.getSkill(349, 1); // Song of Renewal
			//363	1	Song of Meditation
			if (!isMage) // Si es guerrero
			{
				player.getSkill(269, 1); // Hunter
				player.getSkill(364, 1); // Song of Champion
			}
		}
		if (event.equalsIgnoreCase("summonmage"))
		{
			if (activeSummon != null)
			{
				activeSummon.getSkill(1259, 4); // Resist Shock
				activeSummon.getSkill(1035, 4); // Mental Shield
				activeSummon.getSkill(1204, 2); // Wind Walk
				activeSummon.getSkill(1062, 2); // Berserker Spirit
				activeSummon.getSkill(1040, 3); // Shield
				activeSummon.getSkill(1045, 6); // Bless the Body
				activeSummon.getSkill(1036, 2); // Magic Barrier
				activeSummon.getSkill(268, 1); // Wind
				activeSummon.getSkill(264, 1); // Earth
				activeSummon.getSkill(267, 1); // Warding
				activeSummon.getSkill(304, 1); // Song of Vitality
				activeSummon.getSkill(349, 1); // Song of Renewal
				activeSummon.getSkill(1085, 3); // Acumen
				activeSummon.getSkill(1078, 6); // Concentration
				activeSummon.getSkill(1303, 2); // Wild Magic
				activeSummon.getSkill(1059, 3); // Empower
				activeSummon.getSkill(1389, 3); // Greater Shield
				activeSummon.getSkill(273, 1); // Mistyc
				activeSummon.getSkill(276, 1); // Concentration
				activeSummon.getSkill(365, 1); // Siren's Dance
			}
		}
		if (event.equalsIgnoreCase("summonwarrior"))
		{
			if (activeSummon != null)
			{
				activeSummon.getSkill(1259, 4); // Resist Shock
				activeSummon.getSkill(1035, 4); // Mental Shield
				activeSummon.getSkill(1204, 2); // Wind Walk
				activeSummon.getSkill(1062, 2); // Berserker Spirit
				activeSummon.getSkill(1040, 3); // Shield
				activeSummon.getSkill(1045, 6); // Bless the Body
				activeSummon.getSkill(1036, 2); // Magic Barrier
				activeSummon.getSkill(268, 1); // Wind
				activeSummon.getSkill(264, 1); // Earth
				activeSummon.getSkill(267, 1); // Warding
				activeSummon.getSkill(304, 1); // Song of Vitality
				activeSummon.getSkill(349, 1); // Song of Renewal
				activeSummon.getSkill(1388, 3); // Greater Might
				activeSummon.getSkill(1077, 3); // Focus
				activeSummon.getSkill(1068, 3); // Might
				activeSummon.getSkill(1086, 2); // Haste
				activeSummon.getSkill(1242, 3); // Death Whisper
				activeSummon.getSkill(269, 1); // Hunter
				activeSummon.getSkill(274, 1); // Fire
				activeSummon.getSkill(271, 1); // Warrior
				activeSummon.getSkill(275, 1); // Fury
				activeSummon.getSkill(364, 1); // Song of Champion
			}
		}
		if (event.equalsIgnoreCase("dagger"))
		{
			player.getSkill(1259, 4); // Resist Shock
			player.getSkill(1035, 4); // Mental Shield
			player.getSkill(1204, 2); // Wind Walk
			player.getSkill(1040, 3); // Shield
			player.getSkill(1045, 6); // Bless the Body
			player.getSkill(1036, 2); // Magic Barrier
			player.getSkill(1068, 3); // Might
			player.getSkill(1077, 3); // Focus
			player.getSkill(266, 1); // Water
			player.getSkill(1087, 3); // Agility
			player.getSkill(1259, 4); // Resist Shock
			player.getSkill(1035, 4); // Mental Shield
			player.getSkill(1204, 2); // Wind Walk
			player.getSkill(1040, 3); // Shield
			player.getSkill(1045, 6); // Bless the Body
			player.getSkill(1036, 2); // Magic Barrier
			player.getSkill(1389, 3); // Greater Shield
			player.getSkill(1086, 2); // Haste
			player.getSkill(1242, 3); // Death Whisper
		}
		if (event.equalsIgnoreCase("bishop"))
		{
			player.getSkill(1259, 4); // Resist Shock
			player.getSkill(1035, 4); // Mental Shield
			player.getSkill(1204, 2); // Wind Walk
			player.getSkill(1040, 3); // Shield
			player.getSkill(1045, 6); // Bless the Body
			player.getSkill(1036, 2); // Magic Barrier
			player.getSkill(1389, 3); // Greater Shield
			player.getSkill(1085, 3); // Acumen
			player.getSkill(1078, 6); // Concentration
			player.getSkill(1059, 3); // Empower
		}
		if (event.equalsIgnoreCase("removerbuffs"))
			player.stopAllEffects();
		return "buffer.htm";
	}
	
	@Override
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null)
			st = newQuestState(player);
		
		return "buffer.htm";
	}
}
