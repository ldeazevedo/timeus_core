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
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

/**
 * Copy from http://www.l2jserver.com/forum/viewtopic.php?f=69&t=13999
 * @author KKnD
 */
public class EffectDanceStun extends L2Effect
{
	public EffectDanceStun(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.STUN;
	}
	
	/** Notify started */
	@Override
	public boolean onStart()
	{
		L2Character effected = getEffected();
		if (effected.isStunned() || effected.isImmobilized())
			return false;
		int e = effected.getAbnormalEffect();
		if ((e & AbnormalEffect.FLOATING_ROOT.getMask()) != 0 || (e & AbnormalEffect.DANCE_STUNNED.getMask()) != 0)
			return false;
		
		effected.setTarget(null);
		effected.getAI().setIntention(CtrlIntention.IDLE);
		effected.abortAttack();
		effected.abortCast();
		
		effected.startAbnormalEffect(AbnormalEffect.DANCE_STUNNED);
		effected.setIsImmobilized(true);
		effected.disableAllSkills();
		return true;
	}
	
	/** Notify exited */
	@Override
	public void onExit()
	{
		L2Character effected = getEffected();
		effected.stopAbnormalEffect(AbnormalEffect.DANCE_STUNNED);
		effected.setIsImmobilized(false);
		effected.enableAllSkills();
	}
	
	@Override
	public boolean onActionTime()
	{
		// just stop this effect
		return false;
	}
}