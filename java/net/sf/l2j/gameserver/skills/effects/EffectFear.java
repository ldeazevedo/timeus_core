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
package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.instance.L2NpcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeFlagInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2SiegeSummonInstance;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectFlag;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

/**
 * Implementation of the Fear Effect
 * @author littlecrow
 */
public class EffectFear extends L2Effect
{
	public static final int FEAR_RANGE = 500;
	
	public EffectFear(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.FEAR;
	}
	
	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof L2PcInstance && getEffector() instanceof L2PcInstance)
		{
			switch (getSkill().getId())
			{
				case 1376:
				case 1169:
				case 65:
				case 1092:
				case 98:
				case 1272:
				case 1381:
				case 763:
					break;
				default:
					return false;
			}
		}
		
		if (getEffected() instanceof L2NpcInstance || getEffected() instanceof L2SiegeFlagInstance || getEffected() instanceof L2SiegeSummonInstance)
			return false;
		
		if (getEffected().isAfraid())
			return false;
		
		getEffected().startFear();
		onActionTime();
		return true;
	}
	
	@Override
	public void onExit()
	{
		getEffected().stopFear(true);
	}
	
	@Override
	public boolean onActionTime()
	{
		if (!(getEffected() instanceof L2PetInstance))
			getEffected().setRunning();
		
		final int victimX = getEffected().getX();
		final int victimY = getEffected().getY();
		final int victimZ = getEffected().getZ();
		
		final int posX = victimX + (((victimX > getEffector().getX()) ? 1 : -1) * FEAR_RANGE);
		final int posY = victimY + (((victimY > getEffector().getY()) ? 1 : -1) * FEAR_RANGE);
		
		getEffected().getAI().setIntention(CtrlIntention.MOVE_TO, GeoEngine.getInstance().canMoveToTargetLoc(victimX, victimY, victimZ, posX, posY, victimZ));
		return true;
	}
	
	@Override
	public int getEffectFlags()
	{
		return L2EffectFlag.FEAR.getMask();
	}
}