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

public class Q089_SagaOfTheMysticMuse extends SagasSuperClass
{
	public Q089_SagaOfTheMysticMuse()
	{
		super(89, "Saga of the Mystic Muse");
		
		NPC = new int[]
		{
			30174,
			31627,
			31283,
			31283,
			31643,
			31646,
			31648,
			31651,
			31654,
			31655,
			31658,
			31283
		};
		
		Items = new int[]
		{
			7080,
			7530,
			7081,
			7504,
			7287,
			7318,
			7349,
			7380,
			7411,
			7442,
			7083,
			0
		};
		
		Mob = new int[]
		{
			27251,
			27238,
			27255
		};
		
		classid = 103;
		prevclass = 0x1b;
		
		X = new int[]
		{
			119518,
			181227,
			181215
		};
		
		Y = new int[]
		{
			-28658,
			36703,
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