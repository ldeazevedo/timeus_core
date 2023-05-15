package net.sf.l2j.gameserver.skills.effects;

import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.skills.Env;
import net.sf.l2j.gameserver.templates.skills.L2EffectType;

/**
 * Copy from EffectDanceStun.java
 */
public class EffectFloatStun extends L2Effect
{
	public EffectFloatStun(Env env, EffectTemplate template)
	{
		super(env, template);
	}
	
	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.STUN;
	}
	
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
		
		effected.startAbnormalEffect(AbnormalEffect.FLOATING_ROOT);
		effected.setIsImmobilized(true);
		effected.disableAllSkills();
		return true;
	}
	
	@Override
	public void onExit()
	{
		L2Character effected = getEffected();
		effected.stopAbnormalEffect(AbnormalEffect.FLOATING_ROOT);
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