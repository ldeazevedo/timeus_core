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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author Fissban
 * @rework SoultakerNo1
 */
public class VoteRewardTopzone
{
	public static Logger _log = Logger.getLogger(VoteRewardTopzone.class.getName());
	
	public static int _delayForCheck = Config.TOPZONE_REWARD_CHECK_TIME * 1000;
	
	public static int _votesneed = 0;
	
	public static List<String> _ips = new ArrayList<>();
	
	public static List<String> _accounts = new ArrayList<>();
	
	public static int _lastVoteCount = 0;
	
	public VoteRewardTopzone()
	{
		setLastVoteCount(getVotes());
		// ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoReward(), _delayForCheck, _delayForCheck);
	}
	
	public class AutoReward implements Runnable
	{
		@Override
		public void run()
		{
			int votes = getVotes();
			_log.info("VoteReward: Current Votes Topzone: " + votes);
			
			if (votes >= (getLastVoteCount() + Config.TOPZONE_VOTE_COUNT))
			{
				for (L2PcInstance onlinePlayer : L2World.getInstance().getPlayers())
				{
					if (onlinePlayer.isOnline() && !onlinePlayer.getClient().isDetached() && !_accounts.contains(onlinePlayer.getAccountName()) && !_ips.contains(onlinePlayer.getClient().getConnection().getInetAddress().getHostAddress()))
					{
						String[] parase = Config.TOPZONE_SMALL_REWARD.split(",");
						String[] parase3 = Config.TOPZONE_SMALL_REWARD_COUNT.split(",");
						
						for (int o = 0; o < parase.length; o++)
						{
							int parase2 = Integer.parseInt(parase[o]);
							int parase4 = Integer.parseInt(parase3[o]);
							
							onlinePlayer.addItem("vote_reward", parase2, parase4, onlinePlayer, true);
						}
						
						_ips.add(onlinePlayer.getClient().getConnection().getInetAddress().getHostAddress());
						_accounts.add(onlinePlayer.getAccountName());
					}
				}
				
				_log.info("VoteReward Topzone: All players has been rewared!");
				
				Broadcast.announceToOnlinePlayers("Topzone: Aca tenes la recompenza por votar, gracias!.");
				setLastVoteCount(getLastVoteCount() + Config.TOPZONE_VOTE_COUNT);
			}
			
			if (getLastVoteCount() == 0)
			{
				setLastVoteCount(votes);
			}
			else if ((((getLastVoteCount() + Config.TOPZONE_VOTE_COUNT) - votes) > Config.TOPZONE_VOTE_COUNT) || (votes > (getLastVoteCount() + Config.TOPZONE_VOTE_COUNT)))
			{
				setLastVoteCount(votes);
			}
			
			_votesneed = (getLastVoteCount() + Config.TOPZONE_VOTE_COUNT) - votes;
			
			if (_votesneed == 0)
			{
				_votesneed = Config.TOPZONE_VOTE_COUNT;
			}
			
			Broadcast.announceToOnlinePlayers("Topzone: Cantidad de votos: " + votes + ".");
			Broadcast.announceToOnlinePlayers("Topzone: Necesitamos " + _votesneed + " votos mas para la recompenza! Vota!");
			
			_ips.clear();
			_accounts.clear();
		}
	}
	
	public int getVotes()
	{
		try
		{
			URL url = new URL(Config.TOPZONE_SERVER_LINK);
			URLConnection con = url.openConnection();
			con.addRequestProperty("User-Agent", "L2TopZone");
			InputStreamReader isr = new InputStreamReader(con.getInputStream());
			BufferedReader in = new BufferedReader(isr);
			String inputLine;
			
			while ((inputLine = in.readLine()) != null)
			{
				if (inputLine.contains("<span class=\"label label-info\"><i class=\"fa fa-fw fa-lg fa-thumbs-up\">"))
				{
					inputLine = inputLine.replace("        <h3>L2 Timeus<small style=\"float: right\"><a href=\"http://l2topzone.com/lineage2/list/chronicle/interlude\" style=\"text-decoration: none;\"><span class=\"label chronicle-interlude\">Interlude</span></a> <span class=\"label label-info\"><i class=\"fa fa-fw fa-lg fa-thumbs-up\"></i>", "");
					inputLine = inputLine.replace("</span></small></h3>", "");
					inputLine = inputLine.trim();
					return Integer.valueOf(inputLine);
				}
			}
		}
		catch (IOException e)
		{
			Broadcast.announceToOnlinePlayers("Topzone no esta funcionando.");
			_log.warning("AutoVoteRewardHandler: " + e);
		}
		return 0;
	}
	
	public void setLastVoteCount(int voteCount)
	{
		_lastVoteCount = voteCount;
	}
	
	public int getLastVoteCount()
	{
		return _lastVoteCount;
	}
	
	public static VoteRewardTopzone getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public static class SingletonHolder
	{
		protected static final VoteRewardTopzone _instance = new VoteRewardTopzone();
	}
}