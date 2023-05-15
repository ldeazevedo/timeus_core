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
package net.sf.l2j.gameserver.votes;

import java.util.logging.Logger;

import net.sf.l2j.Config;

/**
 * @author Fissban
 * @rework SoultakerNo1
 */
public class VoteRewardManager
{
	private static Logger _log = Logger.getLogger(VoteRewardManager.class.getName());
	
	public VoteRewardManager()
	{
		if (Config.ALLOW_HOPZONE_VOTE_REWARD)
		{
			_log.info("[AutoVoteReward] Hopzone Enable");
			VoteRewardHopzone.getInstance();
		}
		else
		{
			_log.info("[AutoVoteReward] Hopzone Disable");
		}
		if (Config.ALLOW_TOPZONE_VOTE_REWARD)
		{
			// _log.info("[AutoVoteReward] Topzone Enable");
			// VoteRewardTopzone.getInstance();
		}
		else
		{
			_log.info("[AutoVoteReward] Topzone Disable");
		}
	}
	
	public static VoteRewardManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public static class SingletonHolder
	{
		protected static final VoteRewardManager _instance = new VoteRewardManager();
	}
}