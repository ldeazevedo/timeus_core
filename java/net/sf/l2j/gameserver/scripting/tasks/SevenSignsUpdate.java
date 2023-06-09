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
package net.sf.l2j.gameserver.scripting.tasks;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.SevenSigns;
import net.sf.l2j.gameserver.instancemanager.SevenSignsFestival;
import net.sf.l2j.gameserver.scripting.Quest;

/**
 * @author Hasha
 */
public final class SevenSignsUpdate extends Quest implements Runnable
{
	public SevenSignsUpdate()
	{
		super(-1, "tasks");
		
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, 1800000, 1800000);
	}
	
	@Override
	public final void run()
	{
		try
		{
			SevenSigns.getInstance().saveSevenSignsStatus();
			
			if (!SevenSigns.getInstance().isSealValidationPeriod())
				SevenSignsFestival.getInstance().saveFestivalData(false);
			
			_log.info("SevenSigns: Data updated successfully.");
		}
		catch (Exception e)
		{
			_log.warning("SevenSigns: Failed to save Seven Signs configuration: " + e);
		}
	}
}