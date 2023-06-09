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

public class Q082_SagaOfTheSagittarius extends SagasSuperClass
{
	public Q082_SagaOfTheSagittarius()
	{
		super(82, "Saga of the Sagittarius");
		
		NPC = new int[]
		{
			30702,
			31627,
			31604,
			31640,
			31633,
			31646,
			31647,
			31650,
			31654,
			31655,
			31657,
			31641
		};
		
		Items = new int[]
		{
			7080,
			7519,
			7081,
			7497,
			7280,
			7311,
			7342,
			7373,
			7404,
			7435,
			7105,
			0
		};
		
		Mob = new int[]
		{
			27296,
			27231,
			27305
		};
		
		classid = 92;
		prevclass = 0x09;
		
		X = new int[]
		{
			191046,
			46066,
			46066
		};
		
		Y = new int[]
		{
			-40640,
			-36396,
			-36396
		};
		
		Z = new int[]
		{
			-3042,
			-1685,
			-1685
		};
		
		registerNPCs();
	}
}