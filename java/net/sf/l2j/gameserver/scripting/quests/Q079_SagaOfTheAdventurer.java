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

import net.sf.l2j.gameserver.scripting.quests.SagasScripts.SagasSuperClass;

public class Q079_SagaOfTheAdventurer extends SagasSuperClass
{
	public Q079_SagaOfTheAdventurer()
	{
		super(79, "Saga of the Adventurer");
		
		NPC = new int[]
		{
			31603,
			31584,
			31579,
			31615,
			31619,
			31646,
			31647,
			31651,
			31654,
			31655,
			31658,
			31616
		};
		
		Items = new int[]
		{
			7080,
			7516,
			7081,
			7494,
			7277,
			7308,
			7339,
			7370,
			7401,
			7432,
			7102,
			0
		};
		
		Mob = new int[]
		{
			27299,
			27228,
			27302
		};
		
		classid = 93;
		prevclass = 0x08;
		
		X = new int[]
		{
			119518,
			181205,
			181215
		};
		
		Y = new int[]
		{
			-28658,
			36676,
			36676
		};
		
		Z = new int[]
		{
			-3811,
			-4816,
			-4812
		};
		
		registerNPCs();
	}
}