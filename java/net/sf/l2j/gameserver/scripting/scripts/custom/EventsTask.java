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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.entity.events.EventManager;
import net.sf.l2j.gameserver.scripting.Quest;

public class EventsTask extends Quest
{
	private Timer time = new Timer();
	public EventsTask()
	{
		super(-1, "custom");
		setTask();
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		if (event.equalsIgnoreCase("clear"))
		{
			if (time != null)
				time.cancel();
			setTask();
			EventManager.getInstance().clean();
		}
		else if (event.equalsIgnoreCase("Survival"))
		{
			startQuestTimer("Survival", 10800000, null, null, true); //10800000
			setSurvival(0);
			
			startQuestTimer("Survival01", 60000, null, null, false); // checkRegist
			startQuestTimer("Survival02", 75000, null, null, false);
			startQuestTimer("Survival03", 105000, null, null, false);
			startQuestTimer("Survival04", 405000, null, null, false);
		}
		else if (event.equalsIgnoreCase("doItJustOnceSurvival"))
		{
			setSurvival(0);
			startQuestTimer("Survival01", 60000, null, null, false);
			startQuestTimer("Survival02", 75000, null, null, false);
			startQuestTimer("Survival03", 105000, null, null, false);
			startQuestTimer("Survival04", 405000, null, null, false);
		}
		else if (event.equalsIgnoreCase("Survival01"))
			setSurvival(1);
		else if (event.equalsIgnoreCase("Survival02"))
			setSurvival(2);
		else if (event.equalsIgnoreCase("Survival03"))
			setSurvival(3);
		else if (event.equalsIgnoreCase("Survival04"))
			setSurvival(4);
		else if (event.equalsIgnoreCase("RF"))
		{
			startQuestTimer("RF", 3600000, null, null, true);
			setRandomFight(0);
			
			startQuestTimer("RF01", 60000, null, null, false); // checkRegist
			startQuestTimer("RF02", 70000, null, null, false);
			startQuestTimer("RF03", 85000, null, null, false);
			startQuestTimer("RF04", 115000, null, null, false);
			startQuestTimer("RF05", 295000, null, null, false);
		}
		else if (event.equalsIgnoreCase("doItJustOnceRF"))
		{
			setRandomFight(0);
			startQuestTimer("RF01", 60000, null, null, false);
			startQuestTimer("RF02", 70000, null, null, false);
			startQuestTimer("RF03", 85000, null, null, false);
			startQuestTimer("RF04", 115000, null, null, false);
			startQuestTimer("RF05", 295000, null, null, false);
		}
		else if (event.equalsIgnoreCase("RF01"))
			setRandomFight(1);
		else if (event.equalsIgnoreCase("RF02"))
			setRandomFight(2);
		else if (event.equalsIgnoreCase("RF03"))
			setRandomFight(3);
		else if (event.equalsIgnoreCase("RF04"))
			setRandomFight(4);
		else if (event.equalsIgnoreCase("RF05"))
			setRandomFight(5);
		else if (event.equalsIgnoreCase("cancelQuestTimers"))
		{
			cancelQuestTimers("Survival01");
			cancelQuestTimers("Survival02");
			cancelQuestTimers("Survival03");
			cancelQuestTimers("Survival04");
			
			cancelQuestTimers("DM01");
			cancelQuestTimers("DM02");
			cancelQuestTimers("DM03");
			cancelQuestTimers("DM04");
			
			cancelQuestTimers("RF01");
			cancelQuestTimers("RF02");
			cancelQuestTimers("RF03");
			cancelQuestTimers("RF04");
			cancelQuestTimers("RF05");
			_log.info("EventsTask: cancelQuestTimers");
		}
		
		else if (event.equalsIgnoreCase("DM"))
		{
		//	startQuestTimer("Survival", 10800000, null, null, true); //10800000
			setDM(0);
			
			startQuestTimer("DM01", 60000, null, null, false); // checkRegist
			startQuestTimer("DM02", 75000, null, null, false);
			startQuestTimer("DM03", 105000, null, null, false);
			startQuestTimer("DM04", 405000, null, null, false);
		}
		else if (event.equalsIgnoreCase("doItJustOnceDM"))
		{
			setDM(0);
			startQuestTimer("DM01", 60000, null, null, false);
			startQuestTimer("DM02", 75000, null, null, false);
			startQuestTimer("DM03", 105000, null, null, false);
			startQuestTimer("DM04", 405000, null, null, false);
		}
		else if (event.equalsIgnoreCase("DM01"))
			setDM(1);
		else if (event.equalsIgnoreCase("DM02"))
			setDM(2);
		else if (event.equalsIgnoreCase("DM03"))
			setDM(3);
		else if (event.equalsIgnoreCase("DM04"))
			setDM(4);
		return null;
	}
	
	private static void setDM(int stage)
	{
		EventManager.getInstance().setDM(stage);
	}
	
	private static void setSurvival(int stage)
	{
		EventManager.getInstance().setSurvival(stage);
	}
	
	private static void setRandomFight(int stage)
	{
		EventManager.getInstance().setRandomFight(stage);
	}
	
	private void setTask()
	{
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		if (hour > 0 && hour <= 3)
			calendar.set(Calendar.HOUR_OF_DAY, 3);
		else if (hour > 3 && hour <= 6)
			calendar.set(Calendar.HOUR_OF_DAY, 6);
		else if (hour > 6 && hour <= 9)
			calendar.set(Calendar.HOUR_OF_DAY, 9);
		else if (hour > 9 && hour <= 12)
			calendar.set(Calendar.HOUR_OF_DAY, 12);
		else if (hour > 12 && hour <= 15)
			calendar.set(Calendar.HOUR_OF_DAY, 15);
		else if (hour > 15 && hour <= 18)
			calendar.set(Calendar.HOUR_OF_DAY, 18);
		else if (hour > 18 && hour <= 21)
			calendar.set(Calendar.HOUR_OF_DAY, 21);
		else if (hour > 21 && hour <= 0)
			calendar.set(Calendar.HOUR_OF_DAY, 0);
		
		calendar.set(Calendar.MINUTE, 30);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		
		time.schedule(new setTimerTask(1000), calendar.getTime());
		time.schedule(new setTimerTask(1800000), calendar.getTime());
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		_log.info("EventsTask: " + String.valueOf(format.format(calendar.getTime())));
	}
	
	private class setTimerTask extends TimerTask
	{
		final int _time;
		public setTimerTask(int time)
		{
			_time = time;
		}

		@Override
		public void run()
		{
			startQuestTimer(_time <= 1000 ? "Survival" : "RF", _time, null, null, false);
		}
	}
}
