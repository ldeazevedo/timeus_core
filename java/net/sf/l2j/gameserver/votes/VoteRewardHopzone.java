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
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.util.Broadcast;

/**
 * @author Fissban
 * @rework SoultakerNo1
 */
public class VoteRewardHopzone
{
	public static Logger _log = Logger.getLogger(VoteRewardHopzone.class.getName());
	
	public static int _delayForCheck = Config.HOPZONE_REWARD_CHECK_TIME * 1000;
	
	public static int _votesneed = 0;
	
	public static List<String> _ips = new ArrayList<>();
	
	public static List<String> _accounts = new ArrayList<>();
	
	public static int _lastVoteCount = 0;
	
	public VoteRewardHopzone()
	{
		setLastVoteCount(getVotes());
		ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new AutoReward(), _delayForCheck, _delayForCheck);
	}
	
	public class AutoReward implements Runnable
	{
		@Override
		public void run()
		{
			int votes = getVotes();
			_log.info("VoteReward: Current Votes Hopzone: " + votes);
			
			if (votes >= (getLastVoteCount() + Config.HOPZONE_VOTE_COUNT))
			{
				for (L2PcInstance onlinePlayer : L2World.getInstance().getPlayers())
				{
					if (onlinePlayer.isOnline() && !onlinePlayer.getClient().isDetached() && !_accounts.contains(onlinePlayer.getAccountName()) && !_ips.contains(onlinePlayer.getClient().getConnection().getInetAddress().getHostAddress()))
					{
						String[] parase = Config.HOPZONE_SMALL_REWARD.split(",");
						String[] parase3 = Config.HOPZONE_SMALL_REWARD_COUNT.split(",");
						
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
				
				_log.info("AutoVoteRewardManager Hopzone: All players has been rewared!");
				
				Broadcast.announceToOnlinePlayers("Hopzone: Aca tenes la recompenza por votar, gracias!.");
				setLastVoteCount(getLastVoteCount() + Config.HOPZONE_VOTE_COUNT);
			}
			
			if (getLastVoteCount() == 0)
			{
				setLastVoteCount(votes);
			}
			else if ((((getLastVoteCount() + Config.HOPZONE_VOTE_COUNT) - votes) > Config.HOPZONE_VOTE_COUNT) || (votes > (getLastVoteCount() + Config.HOPZONE_VOTE_COUNT)))
			{
				setLastVoteCount(votes);
			}
			
			_votesneed = (getLastVoteCount() + Config.HOPZONE_VOTE_COUNT) - votes;
			
			if (_votesneed == 0)
			{
				_votesneed = Config.HOPZONE_VOTE_COUNT;
			}
			
			Broadcast.announceToOnlinePlayers("Hopzone: Cantidad de votos: " + votes + ".");
			Broadcast.announceToOnlinePlayers("Hopzone: Necesitamos " + _votesneed + " votos mas para la recompenza! Vota!");
			
			_ips.clear();
			_accounts.clear();
		}
	}
	
	public int getVotes()
	{
		InputStreamReader isr = null;
		BufferedReader br = null;
		
		try
		{
			if (!Config.HOPZONE_SERVER_LINK.endsWith(".html"))
				Config.HOPZONE_SERVER_LINK += ".html";
			
			URLConnection con = new URL(Config.HOPZONE_SERVER_LINK).openConnection();
			
			con.addRequestProperty("User-L2Hopzone", "Mozilla/4.76");
			isr = new InputStreamReader(con.getInputStream());
			br = new BufferedReader(isr);
			
			String line;
			while ((line = br.readLine()) != null)
			{
				if (line.contains("no steal make love") || line.contains("no votes here") || line.contains("bang, you don't have votes") || line.contains("la vita e bella"))
				{
					int votes = Integer.valueOf(line.split(">")[2].replace("</span", ""));
					_log.info("HopZone Votes: " + votes);
					return votes;
				}
			}
			
			br.close();
			isr.close();
		}
		catch (Exception e)
		{
			System.out.println(e);
			System.out.println("Error while getting server vote count.");
		}
		
		return _lastVoteCount;
	}
	
	public void setLastVoteCount(int voteCount)
	{
		_lastVoteCount = voteCount;
	}
	
	public int getLastVoteCount()
	{
		return _lastVoteCount;
	}
	
	public static VoteRewardHopzone getInstance()
	{
		return SingletonHolder._instance;
	}
	
	static class SingletonHolder
	{
		protected static final VoteRewardHopzone _instance = new VoteRewardHopzone();
	}
}