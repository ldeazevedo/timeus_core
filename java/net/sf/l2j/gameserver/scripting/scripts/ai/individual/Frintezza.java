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
package net.sf.l2j.gameserver.scripting.scripts.ai.individual;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.Config;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ai.CtrlIntention;
import net.sf.l2j.gameserver.datatables.SkillTable;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.instancemanager.GrandBossManager;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Attackable;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2GrandBossInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2MonsterInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.model.zone.type.L2ScriptZone;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation;
import net.sf.l2j.gameserver.network.serverpackets.FlyToLocation.FlyType;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillCanceld;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;
import net.sf.l2j.gameserver.network.serverpackets.SpecialCamera;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.network.serverpackets.ValidateLocation;
import net.sf.l2j.gameserver.scripting.EventType;
import net.sf.l2j.gameserver.scripting.ScriptManager;
import net.sf.l2j.gameserver.scripting.scripts.ai.L2AttackableAIScript;
import net.sf.l2j.gameserver.skills.AbnormalEffect;
import net.sf.l2j.gameserver.taskmanager.DecayTaskManager;
import net.sf.l2j.gameserver.templates.StatsSet;
import net.sf.l2j.gameserver.templates.skills.L2SkillType;
import net.sf.l2j.gameserver.util.Util;

public class Frintezza extends L2AttackableAIScript
{
	// Skills
	private static final int DEWDROP_OF_DESTRUCTION_SKILL_ID = 2276;
	private static final int BREAKING_ARROW_SKILL_ID = 2234;
	
	private static int[][] daemonSkills;

	private static final IntIntHolder Frintezza_Melody = new IntIntHolder(5006, 1);
	private static final IntIntHolder Daemon_Morph = new IntIntHolder(5017, 1);
	private static final IntIntHolder Bomber_Ghost = new IntIntHolder(5011, 1);
	
	// NPCs
	public static final int FRINTEZZA = 29045;
	private static final int SCARLET1 = 29046;
	private static final int SCARLET2 = 29047;
	private static final int PORTRAIT1 = 29048;
	private static final int PORTRAIT2 = 29049;
	private static final int GHOST1 = 29050;
	private static final int GHOST2 = 29051;
	
	private static final int FIRST_SCARLET_WEAPON = 8204;
	private static final int SECOND_SCARLET_WEAPON = 7903;

	// Frintezza Status Tracking :
	public static final byte DORMANT = 0; // Frintezza is spawned and no one has entered yet. Entry is unlocked
	public static final byte WAITING = 1; // Frintezza is spawend and someone has entered, triggering a 30 minute window for additional people to enter
	// before he unleashes his attack. Entry is unlocked
	public static final byte FIGHTING = 2; // Frintezza is engaged in battle, annihilating his foes. Entry is locked
	public static final byte DEAD = 3; // Frintezza has been killed. Entry is locked
	
	private static long _lastAction = 0;
	private static int _scarletType = 0;
	private static boolean _isInCamera = false; // true is playing SpecialCamera
	
	private static final L2ScriptZone FRINTEZZA_LAIR = ZoneManager.getInstance().getZoneById(100004, L2ScriptZone.class); // <zone id="100004" type="ScriptZone" shape="Cuboid" ... />
	
	private L2GrandBossInstance activeScarlet, strongScarlet, frintezza, weakScarlet;
	private L2MonsterInstance ghost1, ghost2, ghost3, ghost4, portrait1, portrait2, portrait3, portrait4;
	private L2Npc _frintezzaDummy, _overheadDummy, _portraitDummy1, _portraitDummy3, _scarletDummy;
	
	private FrintezzaSong OnSong = null;
	private L2Character _actualVictim;

	private List<L2MonsterInstance> demons = new ArrayList<>();
	
	public Frintezza()
	{
		super("ai/individual");
		
		StatsSet info = GrandBossManager.getInstance().getStatsSet(FRINTEZZA);
		if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == DEAD)
		{
			long temp = (info.getLong("respawn_time") - System.currentTimeMillis());
			if (temp > 0)
				startQuestTimer("frintezza_unlock", temp, null, null, false);
			else
				setBossStatus(DORMANT);
		}
		else if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) != DORMANT)
			setBossStatus(DORMANT);
	}
	
	@Override
	protected void registerNpcs()
	{
		int[] mob =
		{
			SCARLET1,
			SCARLET2,
			FRINTEZZA,
			PORTRAIT1,
			PORTRAIT2,
			GHOST1,
			GHOST2, 20001
		};
		
		addEventIds(mob, EventType.ON_ATTACK, EventType.ON_KILL, EventType.ON_SPAWN, EventType.ON_AGGRO, EventType.ON_SKILL_SEE, EventType.ON_SPELL_FINISHED);
	}
	
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equalsIgnoreCase("beginning"))
		{
			if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == WAITING)
			{
				startQuestTimer("stop_pc", 30000, null, null, false);
				startQuestTimer("camera_1", 30000, null, null, false);
				startQuestTimer("camera_2", 31000, null, null, false); //100
				startQuestTimer("camera_3", 37500, null, null, false); //6500
				startQuestTimer("camera_4", 38400, null, null, false); //900
				startQuestTimer("camera_5", 42400, null, null, false); //ex - cam5b 4000
				startQuestTimer("camera_6", 43750, null, null, false); //1350
				startQuestTimer("camera_7", 51750, null, null, false); //7000
				startQuestTimer("camera_8", 52150, null, null, false); //1000
				startQuestTimer("camera_9", 54150, null, null, false); //2000
				startQuestTimer("camera_10", 58650, null, null, false); //4500
				startQuestTimer("camera_11", 59350, null, null, false); //700
				startQuestTimer("camera_12", 60650, null, null, false); //1300 -15
				startQuestTimer("camera_13", 62150, null, null, false); //1500 -16
				startQuestTimer("camera_14", 69650, null, null, false); //7500 -17
				startQuestTimer("camera_15", 79150, null, null, false); //9500
				startQuestTimer("camera_18", 81550, null, null, false); //2400
				startQuestTimer("camera_19", 84150, null, null, false); //5000 //TODO: 19
				startQuestTimer("throw_up", 85450, null, null, false); //6300
				startQuestTimer("camera_20", 87650, null, null, false); //3500
				startQuestTimer("camera_21", 89650, null, null, false); //2000
				startQuestTimer("camera_22", 92650, null, null, false); //3000
				startQuestTimer("camera_23", 94650, null, null, false); //2000
			}
		}
		else if (event.equalsIgnoreCase("deleteMe") && npc != null)
		{
			npc.deleteMe();
			npc = null;
		}
		else if (event.equalsIgnoreCase("ghostDie") && npc != null)
				npc.doDie(npc);
		else if (event.equalsIgnoreCase("lair_del"))
		{
			if (ghost1 != null)
				startQuestTimer("deleteMe", 0, ghost1, null, false);
			if (ghost2 != null)
				startQuestTimer("deleteMe", 0, ghost2, null, false);
			if (ghost3 != null)
				startQuestTimer("deleteMe", 0, ghost3, null, false);
			if (ghost4 != null)
				startQuestTimer("deleteMe", 0, ghost4, null, false);
			if (portrait1 != null)
				startQuestTimer("deleteMe", 0, portrait1, null, false);
			if (portrait2 != null)
				startQuestTimer("deleteMe", 0, portrait2, null, false);
			if (portrait3 != null)
				startQuestTimer("deleteMe", 0, portrait3, null, false);
			if (portrait4 != null)
				startQuestTimer("deleteMe", 0, portrait4, null, false);
			if (frintezza != null)
				startQuestTimer("deleteMe", 0, frintezza, null, false);
			if (weakScarlet != null)
				startQuestTimer("deleteMe", 0, weakScarlet, null, false);
			if (strongScarlet != null)
				startQuestTimer("deleteMe", 0, strongScarlet, null, false);
			if (_overheadDummy != null)
				startQuestTimer("deleteMe", 0, _overheadDummy, null, false);
			if (_scarletDummy != null)
				startQuestTimer("deleteMe", 0, _scarletDummy, null, false);
			activeScarlet = null;
		}
		else if (event.equalsIgnoreCase("clean"))
		{
			_lastAction = 0;
			_isInCamera = false;
			_scarletType = 0;
		}
		else if (event.equalsIgnoreCase("check"))
		{
			if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING)
				if ((System.currentTimeMillis() - _lastAction) >= 900000)
				{
					setBossStatus(DORMANT);
					
					startQuestTimer("attack_stop", 1000, null, null, false);
					startQuestTimer("clean", 1000, null, null, false);
					startQuestTimer("lair_del", 1000, null, null, false);
					
					cancelQuestTimers("spawn_minion");
					cancelQuestTimers("check");
					return null;
				}
				else if (!FRINTEZZA_LAIR.isInsideZone(npc) || npc.getX() < 171932 || npc.getX() > 176532 || npc.getY() < -90320 || npc.getY() > -85720 || npc.getZ() < -5130)
				{
					npc.enableAllSkills();
					npc.setIsImmobilized(false);
					npc.teleToLocation(174232, -88020, -5116, 0);
				}
		}
		else if (event.equalsIgnoreCase("camera_1")) //TODO: camera 1
		{
			setBossStatus(FIGHTING);
			_isInCamera = true;
			_scarletType = 1;
			
			_frintezzaDummy = addSpawn(29052, 174240, -89805, -5022, 16048, false, 0, false);
			_frintezzaDummy.setIsInvul(true);
			_frintezzaDummy.setIsImmobilized(true);

			_overheadDummy = addSpawn(29052, 174232, -88020, -5110, 16384, false, 0, false);
			_overheadDummy.setIsInvul(true);
			_overheadDummy.setIsImmobilized(true);
			_overheadDummy.setCollisionHeight(600);
			FRINTEZZA_LAIR.broadcastPacket(new NpcInfo(_overheadDummy, null));
			
			_portraitDummy1 = addSpawn(29052, 172450, -87890, -5100, 16048, false, 0, false);
			_portraitDummy1.setIsImmobilized(true);
			_portraitDummy1.setIsInvul(true);
			
			_portraitDummy3 = addSpawn(29052, 176012, -87890, -5100, 16048, false, 0, false);
			_portraitDummy3.setIsImmobilized(true);
			_portraitDummy3.setIsInvul(true);
			
			_scarletDummy = addSpawn(29053, 174232, -88020, -5110, 16384, false, 0, false);
			_scarletDummy.setIsInvul(true);
			_scarletDummy.setIsImmobilized(true);
		}
		else if (event.equalsIgnoreCase("camera_2"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 0, 75, -89, 0, 100, 0, 0, 1, 0));
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 0, 75, -89, 0, 100, 0, 0, 1, 0));
			
			frintezza = (L2GrandBossInstance) addSpawn(FRINTEZZA, 174240, -89805, -5022, 16048, false, 0, false);
			GrandBossManager.getInstance().addBoss(frintezza);
			frintezza.setIsInvul(true);
			frintezza.setIsImmobilized(true);
			updateKnownList(frintezza);
			
			ghost2 = (L2MonsterInstance) addSpawn(GHOST2, 175876, -88713, -5100, 28205, false, 0, false);
			ghost2.setIsImmobilized(true);
			ghost2.disableAllSkills();
			updateKnownList(ghost2);
			
			ghost3 = (L2MonsterInstance) addSpawn(GHOST2, 172608, -88702, -5100, 64817, false, 0, false);
			ghost3.setIsImmobilized(true);
			ghost3.disableAllSkills();
			updateKnownList(ghost3);
			
			ghost1 = (L2MonsterInstance) addSpawn(GHOST1, 175833, -87165, -5100, 35048, false, 0, false);
			ghost1.setIsImmobilized(true);
			ghost1.disableAllSkills();
			updateKnownList(ghost1);
			
			ghost4 = (L2MonsterInstance) addSpawn(GHOST1, 172634, -87165, -5100, 57730, false, 0, false);
			ghost4.setIsImmobilized(true);
			ghost4.disableAllSkills();
			updateKnownList(ghost4);
			
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 300, 90, -10, 6500, 7000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("camera_3"))
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezzaDummy.getObjectId(), 1800, 90, 8, 6500, 7000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_4"))
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_frintezzaDummy.getObjectId(), 140, 90, 10, 2500, 4500, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_5"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 40, 75, -10, 0, 1000, 0, 0, 1, 0));
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 40, 75, -10, 0, 12000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("camera_6"))
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(frintezza, 2));
		else if (event.equalsIgnoreCase("camera_7"))
		{
			startQuestTimer("deleteMe", 100, _frintezzaDummy, null, false);
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(ghost2, 1));
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(ghost3, 1));
		}
		else if (event.equalsIgnoreCase("camera_8"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(ghost1, 1));
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(ghost4, 1));
			
			SpecialCamera cam1 = new SpecialCamera(_portraitDummy1.getObjectId(), 1000, 118, 0, 0, 1000, 0, 0, 1, 0);
			SpecialCamera cam1b = new SpecialCamera(_portraitDummy1.getObjectId(), 1000, 118, 0, 0, 10000, 0, 0, 1, 0);
			SpecialCamera cam3 = new SpecialCamera(_portraitDummy3.getObjectId(), 1000, 62, 0, 0, 1000, 0, 0, 1, 0);
			SpecialCamera cam3b = new SpecialCamera(_portraitDummy3.getObjectId(), 1000, 62, 0, 0, 10000, 0, 0, 1, 0);
			for (L2Character pc : FRINTEZZA_LAIR.getCharactersInside())
				if (pc instanceof L2PcInstance)
					if (pc.getX() < 174232)
					{
						pc.sendPacket(pc.getX() < 174232 ? cam1 : cam3);
						pc.sendPacket(pc.getX() < 174232 ? cam1b : cam3b);
					}
		}
		else if (event.equalsIgnoreCase("camera_9"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 240, 90, 0, 0, 1000, 0, 0, 1, 0));
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 240, 90, 25, 5500, 10000, 0, 0, 1, 0));
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(frintezza, 3));
			startQuestTimer("deleteMe", 100, _portraitDummy1, null, false);
			startQuestTimer("deleteMe", 100, _portraitDummy3, null, false);
		}
		else if (event.equalsIgnoreCase("camera_10"))
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 100, 195, 35, 0, 10000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_11"))
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 100, 195, 35, 0, 10000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_12"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 120, 180, 45, 1500, 10000, 0, 0, 1, 0));
			frintezza.doCast(Frintezza_Melody.getSkill());
		}
		else if (event.equalsIgnoreCase("camera_13"))
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 520, 135, 45, 8000, 10000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_14"))
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 1500, 110, 25, 10000, 13000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_15"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 930, 160, -20, 0, 1000, 0, 0, 1, 0));
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_overheadDummy.getObjectId(), 600, 180, -25, 0, 10000, 0, 0, 1, 0));
			FRINTEZZA_LAIR.broadcastPacket(new MagicSkillUse(_scarletDummy, _scarletDummy, 5004, 1, 5800, 0));
			weakScarlet = (L2GrandBossInstance) addSpawn(SCARLET1, 174232, -88020, -5110, 16384, false, 0, true);
			weakScarlet.setRHandId(FIRST_SCARLET_WEAPON);
			weakScarlet.setIsInvul(true);
			weakScarlet.setIsImmobilized(true);
			weakScarlet.disableAllSkills();
			updateKnownList(weakScarlet);
			activeScarlet = weakScarlet;
		}
		else if (event.equalsIgnoreCase("camera_18"))
			weakScarlet.teleToLocation(174232, -88020, -5110, 0);
		else if (event.equalsIgnoreCase("camera_19"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(_scarletDummy, 3));
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_scarletDummy.getObjectId(), 800, 180, 10, 1000, 10000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("throw_up")) // TODO: throwUp
			throwUp(_scarletDummy, 500, SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(5004, 1));
		else if (event.equalsIgnoreCase("camera_20"))
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(_scarletDummy.getObjectId(), 300, 60, 8, 0, 10000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("camera_21"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(weakScarlet.getObjectId(), 500, 90, 10, 1000, 5000, 0, 0, 1, 0));
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(weakScarlet.getObjectId(), 500, 90, 10, 3000, 5000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("camera_22"))
		{
			portrait1 = (L2MonsterInstance) addSpawn(PORTRAIT1, 175833, -87165, -5126, 35048, false, 0, false);
			portrait1.setIsImmobilized(true);
			portrait1.disableAllSkills();
			updateKnownList(portrait1);
			
			portrait4 = (L2MonsterInstance) addSpawn(PORTRAIT1, 172634, -87165, -5126, 57730, false, 0, false);
			portrait4.setIsImmobilized(true);
			portrait4.disableAllSkills();
			updateKnownList(portrait4);
			
			portrait2 = (L2MonsterInstance) addSpawn(PORTRAIT2, 175876, -88713, -5126, 28205, false, 0, false);
			portrait2.setIsImmobilized(true);
			portrait2.disableAllSkills();
			updateKnownList(portrait2);
			
			portrait3 = (L2MonsterInstance) addSpawn(PORTRAIT2, 172608, -88702, -5126, 64817, false, 0, false);
			portrait3.setIsImmobilized(true);
			portrait3.disableAllSkills();
			updateKnownList(portrait3);

			startQuestTimer("deleteMe", 100, _overheadDummy, null, false);
			startQuestTimer("deleteMe", 100, _scarletDummy, null, false);
		}
		else if (event.equalsIgnoreCase("camera_23"))
		{
			_isInCamera = false;
			startQuestTimer("start_pc", 0, weakScarlet, null, false);
			
			startQuestTimer("songs_play", 5000, frintezza, null, true);
			startQuestTimer("skill_task", 2500, weakScarlet, null, true);
			
			_lastAction = System.currentTimeMillis();
			startQuestTimer("check", 60000, weakScarlet, null, true);
			
			weakScarlet.setIsInvul(false);
			weakScarlet.setShowSummonAnimation(false);
			
			frintezza.setIsInvul(false);
			frintezza.disableAllSkills();
			
			startQuestTimer("spawn_minion", 20000, portrait1, null, true);
			startQuestTimer("spawn_minion", 20000, portrait2, null, true);
			startQuestTimer("spawn_minion", 20000, portrait3, null, true);
			startQuestTimer("spawn_minion", 20000, portrait4, null, true);
		}
		else if (event.equalsIgnoreCase("spawn_minion"))
		{
			if (frintezza != null && !frintezza.isDead())
			{
				if (npc != null && !npc.isDead() && demons.size() <= 24)
				{
					L2Npc mob = addSpawn(npc.getNpcId() + 2, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0, false);
					((L2Attackable) mob).setIsRaidMinion(true);
					demons.add((L2MonsterInstance) mob);
				}
			}
			else 
				cancelQuestTimers("spawn_minion");
		}
		else if (event.equalsIgnoreCase("stop_pc"))
		{
			for (L2Character cha : FRINTEZZA_LAIR.getCharactersInside())
				if (_isInCamera || cha instanceof L2PcInstance)
				{
					cha.abortAttack();
					cha.abortCast();
					cha.disableAllSkills();
					cha.setTarget(null);
					cha.stopMove(null);
					cha.setIsImmobilized(true);
					cha.getAI().setIntention(CtrlIntention.IDLE);
				}
		}
		else if (event.equalsIgnoreCase("start_pc"))
		{
			for (L2Character cha : FRINTEZZA_LAIR.getCharactersInside())
				if (cha != frintezza)
				{
					cha.enableAllSkills();
					cha.setIsImmobilized(false);
				}
		}
		else if (event.equalsIgnoreCase("start_npc"))
		{
			npc.setRunning();
			npc.setIsInvul(false);
		}
		else if (event.equalsIgnoreCase("morph_end"))
			_isInCamera = false;
		else if (event.equalsIgnoreCase("first_morph_01")) //TODO: first_morph
		{
			npc.stopSkillEffects(5008);
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 250, calcCameraAngle(npc), 12, 2000, 15000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("first_morph_02"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(npc, 1));
			npc.setRHandId(SECOND_SCARLET_WEAPON);
		}
		else if (event.equalsIgnoreCase("first_morph_03"))
		{
			npc.setCollisionHeight(110);
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(npc, 4));
			if (Daemon_Morph.getSkill() != null)
				Daemon_Morph.getSkill().getEffects(npc, npc);
			FRINTEZZA_LAIR.broadcastPacket(new NpcInfo(npc, null));
			
			startQuestTimer("songs_play", 5000, frintezza, null, true);
			startQuestTimer("skill_task", 2500, npc, null, true);
			_lastAction = System.currentTimeMillis();
			startQuestTimer("check", 60000, npc, null, true);
		}
		else if (event.equalsIgnoreCase("second_morph_01"))
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(frintezza, 4));
		else if (event.equalsIgnoreCase("second_morph_02"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 250, 120, 15, 0, 1000, 0, 0, 1, 0));
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(frintezza.getObjectId(), 250, 120, 15, 0, 10000, 0, 0, 1, 0));

			npc.setIsInvul(true);
			npc.setIsImmobilized(true);
			npc.disableAllSkills();
			updateKnownList(npc);
		}
		else if (event.equalsIgnoreCase("second_morph_03"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new MagicSkillUse(npc, npc, 5006, 1, 34000, 0));
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 500, 70, 15, 3000, 10000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("second_morph_04"))
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 2500, 90, 12, 6000, 10000, 0, 0, 1, 0));
		else if (event.equalsIgnoreCase("second_morph_05"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 250, calcCameraAngle(npc), 12, 0, 1000, 0, 0, 1, 0));
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 250, calcCameraAngle(npc), 12, 0, 10000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("second_morph_06"))
		{
			npc.doDie(npc);
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 450, calcCameraAngle(npc), 14, 8000, 8000, 0, 0, 1, 0));
			activeScarlet = null;
		}
		else if (event.equalsIgnoreCase("second_morph_07"))
		{
			strongScarlet = (L2GrandBossInstance) addSpawn(SCARLET2, npc.getX(), npc.getY(), npc.getZ(), npc.getHeading(), false, 0, false);
			strongScarlet.setIsInvul(true);
			strongScarlet.setIsImmobilized(true);
			strongScarlet.disableAllSkills();
			updateKnownList(strongScarlet);
			activeScarlet = strongScarlet;
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(strongScarlet.getObjectId(), 450, calcCameraAngle(npc), 12, 500, 14000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("second_morph_08"))
		{
			strongScarlet.setCollisionHeight(130);
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(strongScarlet, 2));
			
			if (Daemon_Morph.getSkill() != null)
				Daemon_Morph.getSkill().getEffects(strongScarlet, strongScarlet);
			FRINTEZZA_LAIR.broadcastPacket(new NpcInfo(strongScarlet, null));
			
			startQuestTimer("start_npc", 6000, strongScarlet, null, false);
			startQuestTimer("songs_play", 5000, frintezza, null, true);
			startQuestTimer("skill_task", 2500, strongScarlet, null, true);
			_lastAction = System.currentTimeMillis();
			startQuestTimer("check", 60000, strongScarlet, null, true);
		}
		else if (event.equalsIgnoreCase("die_01")) //TODO: die_01
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 300, calcCameraAngle(npc) - 180, 5, 0, 7000, 0, 0, 1, 0));
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 200, calcCameraAngle(npc), 85, 4000, 10000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("die_02"))
			npc.doDie(npc);
		else if (event.equalsIgnoreCase("die_03"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 100, 120, 5, 0, 7000, 0, 0, 1, 0));
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 100, 90, 5, 5000, 15000, 0, 0, 1, 0));
		}
		else if (event.equalsIgnoreCase("die_04"))
		{
			FRINTEZZA_LAIR.broadcastPacket(new SpecialCamera(npc.getObjectId(), 900, 90, 25, 7000, 10000, 0, 0, 1, 0));
			addSpawn(29061, 174232, -88020, -5114, 16384, false, 900000, false);
			ScriptManager.getInstance().getQuest("LastImperialTomb").startQuestTimer("remove_players", 900000, null, null, false);
			startQuestTimer("start_pc", 7000, null, null, false);
		}
		else if (event.equalsIgnoreCase("die_05"))
		{
			cancelQuestTimers("spawn_minion");
			startQuestTimer("clean", 30000, null, null, false);
			startQuestTimer("lair_del", 60000, null, null, false);
			
			setBossStatus(DEAD);
			long respawnTime = (long) Config.SPAWN_INTERVAL_FRINTEZZA + Rnd.get(-Config.RANDOM_SPAWN_TIME_FRINTEZZA, Config.RANDOM_SPAWN_TIME_FRINTEZZA);
			respawnTime *= 3600000;
			startQuestTimer("frintezza_unlock", respawnTime, npc, null, false);
			// also save the respawn time so that the info is maintained past reboots
			StatsSet info = GrandBossManager.getInstance().getStatsSet(FRINTEZZA);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatsSet(FRINTEZZA, info);
		}
		else if (event.equalsIgnoreCase("songs_play")) //TODO: play song
			callSongAI(npc);
		else if (event.equalsIgnoreCase("songs_effect"))
		{
			int _songLevel = 0;
			L2Skill skill = OnSong.effectSkill.getSkill();
			if (skill == null)
				return null;
			if (frintezza != null && !frintezza.isDead() && activeScarlet != null && !activeScarlet.isDead())
			{
				_songLevel = skill.getLevel();
				for (L2Character cha : FRINTEZZA_LAIR.getCharactersInside())
					if (cha instanceof L2PcInstance)
					{
						if (skill.getEffectType() == L2SkillType.STUN || skill.isDebuff())
							if (_songLevel == 4 && Rnd.get(100) < 80)
							{
								skill.getEffects(frintezza, cha);
								cha.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(5008, _songLevel));
							}	
							else if (_songLevel == 5 && Rnd.get(100) < 70)
							{
								startQuestTimer("stop_pc", 0, null, null, false);
								cha.setIsParalyzed(true);
								skill.getEffects(frintezza, cha);
								cha.startAbnormalEffect(AbnormalEffect.DANCE_STUNNED);
								cha.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addSkillName(5008, _songLevel));
								int delay_effect = Rnd.get(3000, 15000);
								startQuestTimer("stop_effect", delay_effect, null, null, false);
								startQuestTimer("start_pc", delay_effect, null, null, false);
							}
					}
				if (_songLevel < 4) // skill.getEffects(frintezza, activeScarlet);
					npc.doCast(skill);
			}
		}
		else if (event.equalsIgnoreCase("stop_effect"))
		{
			for (L2Character cha : FRINTEZZA_LAIR.getCharactersInside())
				if (cha instanceof L2PcInstance)
				{
					cha.stopAbnormalEffect(AbnormalEffect.DANCE_STUNNED);
					cha.enableAllSkills();
					cha.setIsImmobilized(false);
					cha.setIsParalyzed(false);
				}
		}
		else if (event.equalsIgnoreCase("attack_stop"))
		{
			cancelQuestTimers("skill_task");
			cancelQuestTimers("songs_play");
			cancelQuestTimers("songs_effect");
			
			if (frintezza != null)
			{
				frintezza.setTarget(null);
				FRINTEZZA_LAIR.broadcastPacket(new MagicSkillCanceld(frintezza.getObjectId()));
				frintezza.abortCast();
				frintezza.getAI().setIntention(CtrlIntention.IDLE);
			}
		}
		else if (event.equalsIgnoreCase("skill_task")) //TODO: skill_task
			callSkillAI(npc);
		else if (event.equalsIgnoreCase("action"))
			FRINTEZZA_LAIR.broadcastPacket(new SocialAction(npc, 1));
		else if (event.equalsIgnoreCase("frintezza_unlock"))
			setBossStatus(DORMANT);
		else if (event.equalsIgnoreCase("timeNextSong")) //BreakingArrow - time next song
			npc.setScriptValue(0);
		else if (event.length() != 0)
			throw new RuntimeException(event);

		return super.onAdvEvent(event, npc, player);
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
		if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING)
		{
			if (npc.isInvul() || _isInCamera)
				return null;
			
			if (!FRINTEZZA_LAIR.isInsideZone(attacker))
			{
				attacker.teleToLocation(150037 + Rnd.get(500), -57720 + Rnd.get(500), -2976, 0);
				return null;
			}
			
			switch (npc.getNpcId())
			{
				case FRINTEZZA:
					npc.getStatus().setCurrentHp(npc.getMaxHp(), false);
					return null;
					
				case SCARLET1:
					synchronized (this)
					{
						if (!_isInCamera)
						{
							if (_scarletType == 1 && npc.getCurrentHp() < npc.getMaxHp() * 0.75)
							{
								_isInCamera = true;
								_scarletType = 2;
								startQuestTimer("attack_stop", 0, null, null, false);
								startQuestTimer("stop_pc", 1000, null, null, false);
								startQuestTimer("first_morph_01", 1100, npc, null, false);
								startQuestTimer("first_morph_02", 4100, npc, null, false);
								startQuestTimer("first_morph_03", 8100, npc, null, false);
								startQuestTimer("start_pc", 11100, null, null, false);
								startQuestTimer("morph_end", 14100, null, null, false);
							}
							else if (_scarletType == 2 && npc.getCurrentHp() < npc.getMaxHp() * 0.5)
							{
								_isInCamera = true;
								_scarletType = 3;
								startQuestTimer("attack_stop", 0, frintezza, null, false);
								startQuestTimer("stop_pc", 2000, null, null, false);
								startQuestTimer("second_morph_01", 2000, npc, null, false);
								startQuestTimer("second_morph_02", 2100, npc, null, false);
								startQuestTimer("second_morph_03", 6300, frintezza, null, false);
								startQuestTimer("second_morph_04", 9300, frintezza, null, false);
								startQuestTimer("second_morph_05", 12300, npc, null, false);
								startQuestTimer("second_morph_06", 12800, npc, null, false);
								startQuestTimer("deleteMe", 19050, npc, null, false);
								startQuestTimer("second_morph_07", 20000, npc, null, false);
								startQuestTimer("second_morph_08", 28100, npc, null, false);
								startQuestTimer("morph_end", 37100, null, null, false);
								startQuestTimer("start_pc", 34100, null, null, false);
							}
						}
					}
					break;
				
				case GHOST1:
				case GHOST2:
					double hp = npc.getCurrentHp();
					double mm = npc.getMaxHp() * 0.10;
					if (hp >= mm && hp - damage > 0 && hp - damage < mm)
						npc.enableSkill(Bomber_Ghost.getSkill());
					break;
			}
			_lastAction = System.currentTimeMillis();
		}
		return super.onAttack(npc, attacker, damage, isPet, skill);
	}
	
	@Override
	public String onSpawn(L2Npc npc)
	{
		if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING)
			switch (npc.getNpcId())
			{
				case FRINTEZZA:
				case SCARLET1:
				case SCARLET2:
					npc.disableCoreAI(true);
					break;
				case GHOST2:
				case GHOST1:
					npc.disableSkill(Bomber_Ghost.getSkill(), -1); // Bomb
					if (!_isInCamera)
						startQuestTimer("action", 200, npc, null, false);
					break;
			}
		return super.onSpawn(npc);
	}
	
	@Override
	public String onSkillSee(L2Npc npc, L2PcInstance player, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING)
			if (targets.length > 0 && targets[0] == npc)
			{
				if (npc.getNpcId() == FRINTEZZA)
				{
					npc.getStatus().setCurrentHp(npc.getMaxHp(), false);
					if (skill.getId() == BREAKING_ARROW_SKILL_ID)
					{
						npc.setScriptValue(1);
						npc.setTarget(null);
						npc.abortCast();
						npc.getAI().setIntention(CtrlIntention.IDLE);
						FRINTEZZA_LAIR.broadcastPacket(new MagicSkillCanceld(npc.getObjectId()));
						for (L2Character ch : FRINTEZZA_LAIR.getCharactersInside())
							if (ch instanceof L2PcInstance)
							{
								L2PcInstance pc = (L2PcInstance) ch;
								pc.stopSkillEffects(5008);
							}
						cancelQuestTimers("songs_effect");
					}
				}
				else if (npc.getNpcId() == PORTRAIT1 || npc.getNpcId() == PORTRAIT2)
					if (skill.getId() == DEWDROP_OF_DESTRUCTION_SKILL_ID)
						npc.doDie(player);
			}
		
		return super.onSkillSee(npc, player, skill, targets, isPet);
	}
	
	@Override
	public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		if (skill.isSuicideAttack())
			return onKill(npc, null, false);
		return super.onSpellFinished(npc, player, skill);
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		if (npc.getNpcId() == 20001 && killer.isGM())
		{
			setBossStatus(WAITING);
			startQuestTimer("clean", 1000, null, null, false);
			startQuestTimer("lair_del", 1000, null, null, false);
			startQuestTimer("beginning", 2000, null, null, false);
		}
		if (GrandBossManager.getInstance().getBossStatus(FRINTEZZA) == FIGHTING)
			switch (npc.getNpcId())
			{
				case FRINTEZZA:
					if (!_isInCamera)
						throw new RuntimeException();
					break;
					
				case SCARLET1:
					if ((_scarletType <= 2) && !_isInCamera)
					{
						// Do 3rd morph.
						npc.setIsDead(false);
						DecayTaskManager.getInstance().cancel(npc);
						_scarletType = 2;
						npc.setCurrentHp(npc.getMaxHp() * 0.45);
						npc.setCurrentMp(npc.getMaxMp() * 0.45);
						return this.onAttack(npc, killer, 0, isPet, null); // Redirect to onAttack.
					}
					break;
				
				case SCARLET2:
					_isInCamera = true;
					_scarletType = 0;
					
					cancelQuestTimers("check");
					cancelQuestTimers("spawn_minion");
					
					startQuestTimer("attack_stop", 0, null, null, false);
					startQuestTimer("stop_pc", 0, null, null, false);
					frintezza.abortCast();
					FRINTEZZA_LAIR.broadcastPacket(new MagicSkillCanceld(frintezza.getObjectId())); //TODO die
					startQuestTimer("die_01", 0, npc, null, false);
					startQuestTimer("die_02", 7400, frintezza, null, false);
					startQuestTimer("die_03", 7500, frintezza, null, false);
					startQuestTimer("die_04", 14500, frintezza, null, false);
					startQuestTimer("die_05", 22500, frintezza, null, false);
					break;
					
				case PORTRAIT1:
				case PORTRAIT2:
					if (npc == portrait1 && ghost1 != null)
						startQuestTimer("ghostDie", 0, ghost1, null, false);
					else if (npc == portrait2 && ghost2 != null)
						startQuestTimer("ghostDie", 0, ghost2, null, false);
					else if (npc == portrait3 && ghost3 != null)
						startQuestTimer("ghostDie", 0, ghost3, null, false);
					else if (npc == portrait4 && ghost4 != null)
						startQuestTimer("ghostDie", 0, ghost4, null, false);
					break;
			}
		return super.onKill(npc, killer, isPet);
	}
	
	private void callSongAI(L2Npc npc) //TODO: callSongAI
	{
		if (npc != null && !npc.isDead())
		{
			if (_isInCamera || npc.getScriptValue() == 2)
				return;
			if (npc.getScriptValue() == 0)
			{
				int rnd = Rnd.get(100);
				for (FrintezzaSong element : FRINTEZZASONGLIST)
				{
					if (rnd < element.chance)
					{
						OnSong = element;
						FRINTEZZA_LAIR.broadcastPacket(new ExShowScreenMessage(1, -1, 2, false, 0, 0, 0, true, 4000, true, element.songName));
						FRINTEZZA_LAIR.broadcastPacket(new MagicSkillUse(npc, npc, element.skill.getId(), element.skill.getSkill().getLevel(), element.skill.getSkill().getHitTime(), 0));
						int delayMusic = element.skill.getSkill().getHitTime();
						if (activeScarlet != null)
							startQuestTimer("songs_effect", (delayMusic - 10000), activeScarlet, null, false);
						startQuestTimer("timeNextSong", delayMusic + Rnd.get(5000, 15000), npc, null, false);
						npc.setScriptValue(2);
						break;
					}
				}
			}
			else if (npc.getScriptValue() == 1)
			{
				npc.setScriptValue(3);
				startQuestTimer("timeNextSong", 35000, npc, null, false);
			}
		}
		else
		{
			cancelQuestTimers("songs_play");
			cancelQuestTimers("songs_effect");
		}
	}
	
	private void callSkillAI(L2Npc npc) //TODO: callSkillAI
	{
		if (npc != null && !npc.isDead() && frintezza != null && !frintezza.isDead())
		{
			if (npc.isCastingNow() || _isInCamera)
				return;
			final L2Skill skill = getRandomSkill();
			
			if (_actualVictim == null || _actualVictim.isDead() || !npc.isMoving() || npc.isMoving() && Rnd.get(10) == 0)
				_actualVictim = getRandomTarget(npc);
			if (_actualVictim == null)
				return;
			if (Util.checkIfInRange((int) (skill.getCastRange() + npc.getCollisionRadius()) * 80 / 100, npc, _actualVictim, true))
			{
				npc.getAI().setIntention(CtrlIntention.IDLE);
				npc.setTarget(_actualVictim);
				npc.doCast(skill);
				return;
			}
			else if (npc.isMoving())
				return;
			else
				npc.getAI().setIntention(CtrlIntention.FOLLOW, _actualVictim, null);
		}
		else
		{
			cancelQuestTimers("skill_task");
			return;
		}
	}
	
	/**
	 *  [0]skillId, [1]level, [1]chance%
	 * @return
	 */
	private static L2Skill getRandomSkill() //TODO: getRandomSkill()
	{
		int[][] DAEMON1_SKILLS = 
		{
			{ 5014, 1 ,100},
			{ 5015, 1 ,5  },
			{ 5015, 4 ,5  },
		};
		int[][] DAEMON2_SKILLS = 
		{
			{ 5014, 2 ,100},
			{ 5015, 2 ,5  },
			{ 5015, 5 ,5  },
			{ 5018, 1 ,10 },
			{ 5016, 1 ,10 },
		};
		int[][] DAEMON3_SKILLS = 
		{
			{ 5014, 3 ,100},
			{ 5015, 3 ,5  },
			{ 5015, 6 ,5  },
			{ 5018, 2 ,10 },
			{ 5019, 1 ,10 },
			{ 5016, 1 ,10 },
		};
		
		if (_scarletType == 1)
			daemonSkills = DAEMON1_SKILLS;
		else if (_scarletType == 2)
			daemonSkills = DAEMON2_SKILLS;
		else if (_scarletType == 3)
			daemonSkills = DAEMON3_SKILLS;
		
		int rnd = Rnd.get(100);
		for (int i = daemonSkills.length; --i >= 0;)
		{
			int[] t = daemonSkills[i];
			int chance = t[2];
			if ((rnd -= chance) <= 0)
				return SkillTable.getInstance().getInfo(t[0], t[1]);
		}
		return null;
	}

	/**
	 * This method allows to select a random target, and is used both for Scarlet.
	 * @param npc to check.
	 * @return the random target.
	 */
	private static L2Character getRandomTarget(L2Npc npc)
	{
		List<L2Character> result = new ArrayList<>();
		
		for (L2Character obj : npc.getKnownList().getKnownType(L2Character.class))
		{
			if (obj instanceof L2PcInstance)
			{
				if (obj.isDead() || !(GeoEngine.getInstance().canSeeTarget(npc, obj)))
					continue;
				
				if (((L2PcInstance) obj).isGM() && ((L2PcInstance) obj).getAppearance().getInvisible())
					continue;
				
				result.add(obj);
			}
		}
		return (result.isEmpty()) ? null : Rnd.get(result);
	}
	
	private static int calcCameraAngle(L2Character npc)
	{
		int _Heading = npc.getHeading();
		return Math.abs((npc.getHeading() < 32768 ? 180 : 540) - (int)(_Heading / 182.044444444));
	}
	
	private static void throwUp(L2Character attacker, final double range, SystemMessage msg)
	{
		final int mx = attacker.getX(), my = attacker.getY();
		for (L2Character target : FRINTEZZA_LAIR.getCharactersInside())
		{
			if (target == attacker)
				continue;
			if (target instanceof L2Npc && ((L2Npc) target).getNpcId() >= 29045 && ((L2Npc) target).getNpcId() <= 29053)
				continue;
			double dx = target.getX() - mx;
			double dy = target.getY() - my;
			if (dx == 0 && dy == 0)
				dx = dy = range / 2;
			double aa = range / Math.sqrt(dx * dx + dy * dy);
			if (aa > 1.0)
			{
				int x = mx + (int) (dx * aa);
				int y = my + (int) (dy * aa);
				int z = target.getZ();
				
				target.getAI().setIntention(CtrlIntention.IDLE);
				target.abortAttack();
				target.abortCast();
				
				target.broadcastPacket(new FlyToLocation(target, x, y, z, FlyType.THROW_UP));
				target.setXYZ(x, y, z);
				target.setHeading(Util.calculateHeadingFrom(x, y, mx, my));
				target.broadcastPacket(new ValidateLocation(target));
				if (msg != null)
					target.sendPacket(msg);
				if (target instanceof L2PcInstance)
					((L2PcInstance) target).standUp();
			}
		}
	}
	
	public static void setBossStatus(int status)
	{
		GrandBossManager.getInstance().setBossStatus(FRINTEZZA, status);
	}
	
	/**
	 * Updates knownlist for the monster. Updates players in the room list.
	 * @param npc
	 */
	public void updateKnownList(L2Npc npc)
	{
		if (npc == null || FRINTEZZA_LAIR.getCharactersInside().isEmpty())
			return;
//		for (L2Character character : FRINTEZZA_LAIR.getCharactersInside())
//			if (character instanceof L2PcInstance)
//				npc.getKnownList().addKnownObject(character);
		for (L2Character character : FRINTEZZA_LAIR.getCharactersInside())
			if (character instanceof L2PcInstance)
				character.getKnownList().addKnownObject(npc);
		for (L2PcInstance player : FRINTEZZA_LAIR.getKnownTypeInside(L2PcInstance.class))
			if (player.isOnline())
				npc.getKnownList().getKnownType(L2PcInstance.class).add(player);//.put(player.getObjectId(), player);
		return;
	}
	
	private static class FrintezzaSong
	{
		public IntIntHolder skill;
		public IntIntHolder effectSkill;
		public String songName;
		public int chance;
		
		public FrintezzaSong(IntIntHolder sk, IntIntHolder esk, String sn, int ch)
		{
			skill = sk;
			effectSkill = esk;
			songName = sn;
			chance = ch;
		}
	}
	
	private final FrintezzaSong[] FRINTEZZASONGLIST =
	{
		new FrintezzaSong(new IntIntHolder(5007, 1), new IntIntHolder(5008, 1), "Requiem of Hatred", _scarletType == 3 && strongScarlet != null && strongScarlet.getCurrentHp() < strongScarlet.getMaxHp() * 0.6 ? 80 : 5),
		new FrintezzaSong(new IntIntHolder(5007, 2), new IntIntHolder(5008, 2), "Rondo of Solitude", 50),
		new FrintezzaSong(new IntIntHolder(5007, 3), new IntIntHolder(5008, 3), "Frenetic Toccata", 70),
		new FrintezzaSong(new IntIntHolder(5007, 4), new IntIntHolder(5008, 4), "Fugue of Jubilation", 90),
		new FrintezzaSong(new IntIntHolder(5007, 5), new IntIntHolder(5008, 5), "Hypnotic Mazurka", 100),
	};
}