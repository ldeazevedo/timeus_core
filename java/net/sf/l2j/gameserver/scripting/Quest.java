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
package net.sf.l2j.gameserver.scripting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.cache.HtmCache;
import net.sf.l2j.gameserver.datatables.ItemTable;
import net.sf.l2j.gameserver.datatables.NpcTable;
import net.sf.l2j.gameserver.instancemanager.ZoneManager;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Object;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.L2Spawn;
import net.sf.l2j.gameserver.model.SpawnLocation;
import net.sf.l2j.gameserver.model.actor.L2Character;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.zone.L2ZoneType;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.scripting.scripts.ai.L2AttackableAIScript;

public class Quest
{
	protected static final Logger _log = Logger.getLogger(Quest.class.getName());
	
	private static final String LOAD_QUEST_STATES = "SELECT name,value FROM character_quests WHERE charId=? AND var='<state>'";
	private static final String LOAD_QUEST_VARIABLES = "SELECT name,var,value FROM character_quests WHERE charId=? AND var<>'<state>'";
	private static final String DELETE_INVALID_QUEST = "DELETE FROM character_quests WHERE name=?";
	
	private static final String HTML_NONE_AVAILABLE = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>";
	private static final String HTML_ALREADY_COMPLETED = "<html><body>This quest has already been completed.</body></html>";
	private static final String HTML_TOO_MUCH_QUESTS = "<html><body>You have already accepted the maximum number of quests. No more than 25 quests may be undertaken simultaneously.<br>For quest information, enter Alt+U.</body></html>";
	
	public static final byte STATE_CREATED = 0;
	public static final byte STATE_STARTED = 1;
	public static final byte STATE_COMPLETED = 2;
	
	private final Map<Integer, List<QuestTimer>> _eventTimers = new ConcurrentHashMap<>();
	
	private final int _id;
	private final String _descr;
	private boolean _onEnterWorld;
	private int[] _itemsIds;
	
	// Dimensional Diamond Rewards by Class for 2nd class transfer quest (35)
	protected static final Map<Integer, Integer> DF_REWARD_35 = new HashMap<>();
	{
		DF_REWARD_35.put(1, 61);
		DF_REWARD_35.put(4, 45);
		DF_REWARD_35.put(7, 128);
		DF_REWARD_35.put(11, 168);
		DF_REWARD_35.put(15, 49);
		DF_REWARD_35.put(19, 61);
		DF_REWARD_35.put(22, 128);
		DF_REWARD_35.put(26, 168);
		DF_REWARD_35.put(29, 49);
		DF_REWARD_35.put(32, 61);
		DF_REWARD_35.put(35, 128);
		DF_REWARD_35.put(39, 168);
		DF_REWARD_35.put(42, 49);
		DF_REWARD_35.put(45, 61);
		DF_REWARD_35.put(47, 61);
		DF_REWARD_35.put(50, 49);
		DF_REWARD_35.put(54, 85);
		DF_REWARD_35.put(56, 85);
	}
	
	// Dimensional Diamond Rewards by Race for 2nd class transfer quest (37)
	protected static final Map<Integer, Integer> DF_REWARD_37 = new HashMap<>();
	{
		DF_REWARD_37.put(0, 96);
		DF_REWARD_37.put(1, 102);
		DF_REWARD_37.put(2, 98);
		DF_REWARD_37.put(3, 109);
		DF_REWARD_37.put(4, 50);
	}
	
	// Dimensional Diamond Rewards by Class for 2nd class transfer quest (39)
	protected static final Map<Integer, Integer> DF_REWARD_39 = new HashMap<>();
	{
		DF_REWARD_39.put(1, 72);
		DF_REWARD_39.put(4, 104);
		DF_REWARD_39.put(7, 96);
		DF_REWARD_39.put(11, 122);
		DF_REWARD_39.put(15, 60);
		DF_REWARD_39.put(19, 72);
		DF_REWARD_39.put(22, 96);
		DF_REWARD_39.put(26, 122);
		DF_REWARD_39.put(29, 45);
		DF_REWARD_39.put(32, 104);
		DF_REWARD_39.put(35, 96);
		DF_REWARD_39.put(39, 122);
		DF_REWARD_39.put(42, 60);
		DF_REWARD_39.put(45, 64);
		DF_REWARD_39.put(47, 72);
		DF_REWARD_39.put(50, 92);
		DF_REWARD_39.put(54, 82);
		DF_REWARD_39.put(56, 23);
	}
	
	/**
	 * (Constructor)Add values to class variables and put the quest in HashMaps.
	 * @param questId : int pointing out the ID of the quest
	 * @param descr : String for the description of the quest
	 */
	public Quest(int questId, String descr)
	{
		_id = questId;
		_descr = descr;
	}
	
	/**
	 * Returns the name of the script.
	 * @return
	 */
	public final String getName()
	{
		return getClass().getSimpleName();
	}
	
	/**
	 * Return ID of the quest.
	 * @return int
	 */
	public int getQuestId()
	{
		return _id;
	}
	
	/**
	 * Return type of the quest.
	 * @return boolean : True for (live) quest, False for script, AI, etc.
	 */
	public boolean isRealQuest()
	{
		return _id > 0;
	}
	
	/**
	 * Return description of the quest.
	 * @return String
	 */
	public String getDescr()
	{
		return _descr;
	}
	
	public boolean getOnEnterWorld()
	{
		return _onEnterWorld;
	}
	
	public void setOnEnterWorld(boolean val)
	{
		_onEnterWorld = val;
	}
	
	/**
	 * Return registered quest items.
	 * @return int[]
	 */
	public int[] getItemsIds()
	{
		return _itemsIds;
	}
	
	/**
	 * Registers all items that have to be destroyed in case player abort the quest or finish it.
	 * @param itemIds
	 */
	public void setItemsIds(int... itemIds)
	{
		_itemsIds = itemIds;
	}
	
	/**
	 * Add a new QuestState to the database and return it.
	 * @param player
	 * @return QuestState : QuestState created
	 */
	public QuestState newQuestState(L2PcInstance player)
	{
		return new QuestState(player, this, STATE_CREATED);
	}
	
	/**
	 * Add quests to the L2PCInstance of the player.<BR>
	 * <BR>
	 * <U><I>Action : </U></I><BR>
	 * Add state of quests, drops and variables for quests in the HashMap _quest of L2PcInstance
	 * @param player : Player who is entering the world
	 */
	public final static void playerEnter(L2PcInstance player)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement invalidQuest = con.prepareStatement(DELETE_INVALID_QUEST);
			
			PreparedStatement statement = con.prepareStatement(LOAD_QUEST_STATES);
			statement.setInt(1, player.getObjectId());
			ResultSet rs = statement.executeQuery();
			while (rs.next())
			{
				String questId = rs.getString("name");
				
				Quest q = ScriptManager.getInstance().getQuest(questId);
				if (q == null)
				{
					if (Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuest.setString(1, questId);
						invalidQuest.executeUpdate();
					}
					
					_log.finer("Unknown  quest " + questId + " for player " + player.getName());
					continue;
				}
				
				new QuestState(player, q, rs.getByte("value"));
			}
			rs.close();
			statement.close();
			
			statement = con.prepareStatement(LOAD_QUEST_VARIABLES);
			statement.setInt(1, player.getObjectId());
			rs = statement.executeQuery();
			while (rs.next())
			{
				String questId = rs.getString("name");
				
				QuestState qs = player.getQuestState(questId);
				if (qs == null)
				{
					if (Config.AUTODELETE_INVALID_QUEST_DATA)
					{
						invalidQuest.setString(1, questId);
						invalidQuest.executeUpdate();
					}
					
					_log.finer("Unknown quest " + questId + " for player " + player.getName());
					continue;
				}
				
				qs.setInternal(rs.getString("var"), rs.getString("value"));
			}
			rs.close();
			statement.close();
			
			invalidQuest.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "could not insert char quest:", e);
		}
	}
	
	/**
	 * @param player : The player to make checks on.
	 * @param object : to take range reference from
	 * @return A random party member or the passed player if he has no party.
	 */
	public L2PcInstance getRandomPartyMember(L2PcInstance player, L2Object object)
	{
		// No valid player instance is passed, there is nothing to check.
		if (player == null)
			return null;
		
		// No party or no object, return player.
		if (object == null || !player.isInParty())
			return player;
		
		// Player's party.
		List<L2PcInstance> members = new ArrayList<>();
		for (L2PcInstance member : player.getParty().getPartyMembers())
		{
			if (member.isInsideRadius(object, Config.ALT_PARTY_RANGE, true, false))
				members.add(member);
		}
		
		// No party members, return. (note: player is party member too, in most cases he is included in members too)
		if (members.isEmpty())
			return null;
		
		// Random party member.
		return members.get(Rnd.get(members.size()));
	}
	
	/**
	 * Auxiliary function for party quests. Checks the player's condition. Player member must be within Config.ALT_PARTY_RANGE distance from the npc. If npc is null, distance condition is ignored.
	 * @param player : the instance of a player whose party is to be searched
	 * @param npc : the instance of a L2Npc to compare distance
	 * @param var : a tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @param value : a tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @return QuestState : The QuestState of that player.
	 */
	public QuestState checkPlayerCondition(L2PcInstance player, L2Npc npc, String var, String value)
	{
		// No valid player instance is passed, there is nothing to check.
		if (player == null)
			return null;
		
		// Check player's quest conditions.
		QuestState st = player.getQuestState(getName());
		if (st == null)
			return null;
		
		// Condition exists? Condition has correct value?
		if (st.get(var) == null || !value.equalsIgnoreCase(st.get(var)))
			return null;
		
		// Invalid npc instance?
		if (npc == null)
			return null;
		
		// Player is in range?
		if (!player.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false))
			return null;
		
		return st;
	}
	
	/**
	 * Auxiliary function for party quests. Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own
	 * @param player : the instance of a player whose party is to be searched
	 * @param npc : the instance of a L2Npc to compare distance
	 * @param var : a tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @param value : a tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @return List<L2PcInstance> : List of party members that matches the specified condition, empty list if none matches. If the var is null, empty list is returned (i.e. no condition is applied). The party member must be within Config.ALT_PARTY_RANGE distance from the npc. If npc is null,
	 *         distance condition is ignored.
	 */
	public List<L2PcInstance> getPartyMembers(L2PcInstance player, L2Npc npc, String var, String value)
	{
		// Output list.
		List<L2PcInstance> candidates = new ArrayList<>();
		
		// Valid player instance is passed and player is in a party? Check party.
		if (player != null && player.isInParty())
		{
			// Filter candidates from player's party.
			for (L2PcInstance partyMember : player.getParty().getPartyMembers())
			{
				if (partyMember == null)
					continue;
				
				// Check party members' quest condition.
				if (checkPlayerCondition(partyMember, npc, var, value) != null)
					candidates.add(partyMember);
			}
		}
		// Player is solo, check the player
		else if (checkPlayerCondition(player, npc, var, value) != null)
			candidates.add(player);
		
		return candidates;
	}
	
	/**
	 * Auxiliary function for party quests. Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own
	 * @param player : the instance of a player whose party is to be searched
	 * @param npc : the instance of a L2Npc to compare distance
	 * @param var : a tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @param value : a tuple specifying a quest condition that must be satisfied for a party member to be considered.
	 * @return L2PcInstance : L2PcInstance for a random party member that matches the specified condition, or null if no match. If the var is null, null is returned (i.e. no condition is applied). The party member must be within 1500 distance from the npc. If npc is null, distance condition is
	 *         ignored.
	 */
	public L2PcInstance getRandomPartyMember(L2PcInstance player, L2Npc npc, String var, String value)
	{
		// No valid player instance is passed, there is nothing to check.
		if (player == null)
			return null;
		
		// Get all candidates fulfilling the condition.
		final List<L2PcInstance> candidates = getPartyMembers(player, npc, var, value);
		
		// No candidate, return.
		if (candidates.isEmpty())
			return null;
		
		// Return random candidate.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Auxiliary function for party quests. Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own.
	 * @param player : the instance of a player whose party is to be searched
	 * @param npc : the instance of a L2Npc to compare distance
	 * @param value : the value of the "cond" variable that must be matched
	 * @return L2PcInstance : L2PcInstance for a random party member that matches the specified condition, or null if no match.
	 */
	public L2PcInstance getRandomPartyMember(L2PcInstance player, L2Npc npc, String value)
	{
		return getRandomPartyMember(player, npc, "cond", value);
	}
	
	/**
	 * Auxiliary function for party quests. Checks the player's condition. Player member must be within Config.ALT_PARTY_RANGE distance from the npc. If npc is null, distance condition is ignored.
	 * @param player : the instance of a player whose party is to be searched
	 * @param npc : the instance of a L2Npc to compare distance
	 * @param state : the state in which the party member's QuestState must be in order to be considered.
	 * @return QuestState : The QuestState of that player.
	 */
	public QuestState checkPlayerState(L2PcInstance player, L2Npc npc, byte state)
	{
		// No valid player instance is passed, there is nothing to check.
		if (player == null)
			return null;
		
		// Check player's quest conditions.
		QuestState st = player.getQuestState(getName());
		if (st == null)
			return null;
		
		// State correct?
		if (st.getState() != state)
			return null;
		
		// Invalid npc instance?
		if (npc == null)
			return null;
		
		// Player is in range?
		if (!player.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false))
			return null;
		
		return st;
	}
	
	/**
	 * Auxiliary function for party quests. Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own.
	 * @param player : the instance of a player whose party is to be searched
	 * @param npc : the instance of a L2Npc to compare distance
	 * @param state : the state in which the party member's QuestState must be in order to be considered.
	 * @return List<L2PcInstance> : List of party members that matches the specified quest state, empty list if none matches. The party member must be within Config.ALT_PARTY_RANGE distance from the npc. If npc is null, distance condition is ignored.
	 */
	public List<L2PcInstance> getPartyMembersState(L2PcInstance player, L2Npc npc, byte state)
	{
		// Output list.
		List<L2PcInstance> candidates = new ArrayList<>();
		
		// Valid player instance is passed and player is in a party? Check party.
		if (player != null && player.isInParty())
		{
			// Filter candidates from player's party.
			for (L2PcInstance partyMember : player.getParty().getPartyMembers())
			{
				if (partyMember == null)
					continue;
				
				// Check party members' quest state.
				if (checkPlayerState(partyMember, npc, state) != null)
					candidates.add(partyMember);
			}
		}
		// Player is solo, check the player
		else if (checkPlayerState(player, npc, state) != null)
			candidates.add(player);
		
		return candidates;
	}
	
	/**
	 * Auxiliary function for party quests. Note: This function is only here because of how commonly it may be used by quest developers. For any variations on this function, the quest script can always handle things on its own.
	 * @param player : the instance of a player whose party is to be searched
	 * @param npc : the instance of a monster to compare distance
	 * @param state : the state in which the party member's QuestState must be in order to be considered.
	 * @return L2PcInstance: L2PcInstance for a random party member that matches the specified condition, or null if no match. If the var is null, any random party member is returned (i.e. no condition is applied).
	 */
	public L2PcInstance getRandomPartyMemberState(L2PcInstance player, L2Npc npc, byte state)
	{
		// No valid player instance is passed, there is nothing to check.
		if (player == null)
			return null;
		
		// Get all candidates fulfilling the condition.
		final List<L2PcInstance> candidates = getPartyMembersState(player, npc, state);
		
		// No candidate, return.
		if (candidates.isEmpty())
			return null;
		
		// Return random candidate.
		return candidates.get(Rnd.get(candidates.size()));
	}
	
	/**
	 * Retrieves the clan leader quest state.
	 * @param player : the player to test
	 * @param npc : the npc to test distance
	 * @return the QuestState of the leader, or null if not found
	 */
	public QuestState getClanLeaderQuestState(L2PcInstance player, L2Npc npc)
	{
		// If player is the leader, retrieves directly the qS and bypass others checks
		if (player.isClanLeader() && player.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false))
			return player.getQuestState(getName());
		
		// Verify if the player got a clan
		L2Clan clan = player.getClan();
		if (clan == null)
			return null;
		
		// Verify if the leader is online
		L2PcInstance leader = clan.getLeader().getPlayerInstance();
		if (leader == null)
			return null;
		
		// Verify if the player is on the radius of the leader. If true, send leader's quest state.
		if (leader.isInsideRadius(npc, Config.ALT_PARTY_RANGE, true, false))
			return leader.getQuestState(getName());
		
		return null;
	}
	
	/**
	 * Add a timer to the quest, if it doesn't exist already. If the timer is repeatable, it will auto-fire automatically, at a fixed rate, until explicitly canceled.
	 * @param name name of the timer (also passed back as "event" in onAdvEvent)
	 * @param time time in ms for when to fire the timer
	 * @param npc npc associated with this timer (can be null)
	 * @param player player associated with this timer (can be null)
	 * @param repeating indicates if the timer is repeatable or one-time.
	 */
	public void startQuestTimer(String name, long time, L2Npc npc, L2PcInstance player, boolean repeating)
	{
		// Get quest timers for this timer type.
		List<QuestTimer> timers = _eventTimers.get(name.hashCode());
		if (timers == null)
		{
			// None timer exists, create new list.
			timers = new CopyOnWriteArrayList<>();
			
			// Add new timer to the list.
			timers.add(new QuestTimer(this, name, npc, player, time, repeating));
			
			// Add timer list to the map.
			_eventTimers.put(name.hashCode(), timers);
		}
		else
		{
			// Check, if specific timer already exists.
			for (QuestTimer timer : timers)
			{
				// If so, return.
				if (timer != null && timer.equals(this, name, npc, player))
					return;
			}
			
			// Add new timer to the list.
			timers.add(new QuestTimer(this, name, npc, player, time, repeating));
		}
	}
	
	public QuestTimer getQuestTimer(String name, L2Npc npc, L2PcInstance player)
	{
		// Get quest timers for this timer type.
		List<QuestTimer> timers = _eventTimers.get(name.hashCode());
		
		// Timer list does not exists or is empty, return.
		if (timers == null || timers.isEmpty())
			return null;
		
		// Check, if specific timer exists.
		for (QuestTimer timer : timers)
		{
			// If so, return him.
			if (timer != null && timer.equals(this, name, npc, player))
				return timer;
		}
		return null;
	}
	
	public void cancelQuestTimer(String name, L2Npc npc, L2PcInstance player)
	{
		// If specified timer exists, cancel him.
		QuestTimer timer = getQuestTimer(name, npc, player);
		if (timer != null)
			timer.cancel();
	}
	
	public void cancelQuestTimers(String name)
	{
		// Get quest timers for this timer type.
		List<QuestTimer> timers = _eventTimers.get(name.hashCode());
		
		// Timer list does not exists or is empty, return.
		if (timers == null || timers.isEmpty())
			return;
		
		// Cancel all quest timers.
		for (QuestTimer timer : timers)
		{
			if (timer != null)
				timer.cancel();
		}
	}
	
	// Note, keep it default. It is used withing QuestTimer, when it terminates.
	/**
	 * Removes QuestTimer from timer list, when it terminates.
	 * @param timer : QuestTimer, which is beeing terminated.
	 */
	void removeQuestTimer(QuestTimer timer)
	{
		// Timer does not exist, return.
		if (timer == null)
			return;
		
		// Get quest timers for this timer type.
		List<QuestTimer> timers = _eventTimers.get(timer.toString().hashCode());
		
		// Timer list does not exists or is empty, return.
		if (timers == null || timers.isEmpty())
			return;
		
		// Remove timer from the list.
		timers.remove(timer);
	}
	
	/**
	 * Add a temporary (quest) spawn on the location of a character.
	 * @param npcId the NPC template to spawn.
	 * @param cha the position where to spawn it.
	 * @param randomOffset
	 * @param despawnDelay
	 * @param isSummonSpawn if true, spawn with animation (if any exists).
	 * @return instance of the newly spawned npc with summon animation.
	 */
	public L2Npc addSpawn(int npcId, L2Character cha, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		return addSpawn(npcId, cha.getX(), cha.getY(), cha.getZ(), cha.getHeading(), randomOffset, despawnDelay, isSummonSpawn);
	}
	
	/**
	 * Add a temporary (quest) spawn on the Location object.
	 * @param npcId the NPC template to spawn.
	 * @param loc the position where to spawn it.
	 * @param randomOffset
	 * @param despawnDelay
	 * @param isSummonSpawn if true, spawn with animation (if any exists).
	 * @return instance of the newly spawned npc with summon animation.
	 */
	public L2Npc addSpawn(int npcId, SpawnLocation loc, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		return addSpawn(npcId, loc.getX(), loc.getY(), loc.getZ(), loc.getHeading(), randomOffset, despawnDelay, isSummonSpawn);
	}
	
	/**
	 * Add a temporary (quest) spawn on the location of a character.
	 * @param npcId the NPC template to spawn.
	 * @param x
	 * @param y
	 * @param z
	 * @param heading
	 * @param randomOffset
	 * @param despawnDelay
	 * @param isSummonSpawn if true, spawn with animation (if any exists).
	 * @return instance of the newly spawned npc with summon animation.
	 */
	public L2Npc addSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffset, long despawnDelay, boolean isSummonSpawn)
	{
		try
		{
			final NpcTemplate template = NpcTable.getInstance().getTemplate(npcId);
			if (template == null)
				return null;
			
			if (randomOffset)
			{
				x += Rnd.get(-100, 100);
				y += Rnd.get(-100, 100);
			}
			
			final L2Spawn spawn = new L2Spawn(template);
			spawn.setLoc(x, y, z + 20, heading);
			spawn.setRespawnState(false);
			
			final L2Npc npc = spawn.doSpawn(isSummonSpawn);
			if (despawnDelay > 0)
				npc.scheduleDespawn(despawnDelay);
			
			return npc;
		}
		catch (Exception e1)
		{
			_log.warning("Could not spawn Npc " + npcId);
			return null;
		}
	}
	
	/**
	 * @return default html page "You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements."
	 */
	public static String getNoQuestMsg()
	{
		return HTML_NONE_AVAILABLE;
	}
	
	/**
	 * @return default html page "This quest has already been completed."
	 */
	public static String getAlreadyCompletedMsg()
	{
		return HTML_ALREADY_COMPLETED;
	}
	
	/**
	 * @return default html page "You have already accepted the maximum number of quests. No more than 25 quests may be undertaken simultaneously. For quest information, enter Alt+U."
	 */
	public static String getTooMuchQuestsMsg()
	{
		return HTML_TOO_MUCH_QUESTS;
	}
	
	/**
	 * Show a message to player.<BR>
	 * <BR>
	 * <U><I>Concept : </I></U><BR>
	 * 3 cases are managed according to the value of the parameter "res" :
	 * <UL>
	 * <LI><U>"res" ends with string ".html" :</U> an HTML is opened in order to be shown in a dialog box</LI>
	 * <LI><U>"res" starts with "<html>" :</U> the message hold in "res" is shown in a dialog box</LI>
	 * <LI><U>otherwise :</U> the message held in "res" is shown in chat box</LI>
	 * </UL>
	 * @param npc : which launches the dialog, null in case of random scripts
	 * @param player : the player.
	 * @param result : String pointing out the message to show at the player
	 */
	public void showResult(L2Npc npc, L2PcInstance player, String result)
	{
		if (player == null || result == null || result.isEmpty())
			return;
		
		if (result.endsWith(".htm") || result.endsWith(".html"))
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(npc == null ? 0 : npc.getNpcId());
			if (isRealQuest())
				npcReply.setFile("./data/html/scripts/quests/" + getName() + "/" + result);
			else
				npcReply.setFile("./data/html/scripts/" + getDescr() + "/" + getName() + "/" + result);
			
			if (npc != null)
				npcReply.replace("%objectId%", npc.getObjectId());
			
			player.sendPacket(npcReply);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else if (result.startsWith("<html>"))
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(npc == null ? 0 : npc.getNpcId());
			npcReply.setHtml(result);
			
			if (npc != null)
				npcReply.replace("%objectId%", npc.getObjectId());
			
			player.sendPacket(npcReply);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
		else
			player.sendMessage(result);
	}
	
	/**
	 * Show message error to player who has an access level greater than 0
	 * @param player : L2PcInstance
	 * @param e : Throwable
	 */
	public void showError(L2PcInstance player, Throwable e)
	{
		_log.log(Level.WARNING, getClass().getName(), e);
		
		if (e.getMessage() == null)
			e.printStackTrace();
		
		if (player != null && player.isGM())
		{
			NpcHtmlMessage npcReply = new NpcHtmlMessage(0);
			npcReply.setHtml("<html><body><title>Script error</title>" + e.getMessage() + "</body></html>");
			player.sendPacket(npcReply);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Returns String representation of given quest html.
	 * @param fileName : the filename to send.
	 * @return String : message sent to client.
	 */
	public String getHtmlText(String fileName)
	{
		if (isRealQuest())
			return HtmCache.getInstance().getHtmForce("./data/html/scripts/quests/" + getName() + "/" + fileName);
		
		return HtmCache.getInstance().getHtmForce("./data/html/scripts/" + getDescr() + "/" + getName() + "/" + fileName);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for the specified Event type.
	 * @param npcId : id of the NPC to register
	 * @param eventType : type of event being registered
	 */
	public void addEventId(int npcId, EventType eventType)
	{
		try
		{
			final NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);
			if (t != null)
				t.addQuestEvent(eventType, this);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on addEventId(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Add this script to the list of script that the passed mob will respond to for the specified Event type.
	 * @param npcId : id of the NPC to register
	 * @param eventTypes : types of events being registered
	 */
	public void addEventIds(int npcId, EventType... eventTypes)
	{
		try
		{
			final NpcTemplate t = NpcTable.getInstance().getTemplate(npcId);
			if (t != null)
				for (EventType eventType : eventTypes)
					t.addQuestEvent(eventType, this);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on addEventIds(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Register monsters on particular event types.
	 * @param npcIds An array of mobs.
	 * @param eventTypes Types of event to register mobs on.
	 */
	public void addEventIds(int[] npcIds, EventType... eventTypes)
	{
		for (int id : npcIds)
			addEventIds(id, eventTypes);
	}
	
	/**
	 * Register monsters on particular event types.
	 * @param npcIds An array of mobs.
	 * @param eventTypes Types of event to register mobs on.
	 */
	public void addEventIds(Iterable<Integer> npcIds, EventType... eventTypes)
	{
		for (int id : npcIds)
			addEventIds(id, eventTypes);
	}
	
	/**
	 * Add the quest to the NPC's startQuest
	 * @param npcIds A serie of ids.
	 */
	public void addStartNpc(int... npcIds)
	{
		for (int npcId : npcIds)
			addEventId(npcId, EventType.QUEST_START);
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for Attack Events.
	 * @param npcIds A serie of ids.
	 */
	public void addAttackId(int... npcIds)
	{
		for (int npcId : npcIds)
			addEventId(npcId, EventType.ON_ATTACK);
	}
	
	/**
	 * Quest event notifycator for player's or player's pet attack.
	 * @param npc Attacked npc instace.
	 * @param attacker Attacker or pet owner.
	 * @param damage Given damage.
	 * @param isPet Player summon attacked?
	 * @param skill the skill used to attack the NPC (can be null)
	 */
	public final void notifyAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
		String res = null;
		try
		{
			res = onAttack(npc, attacker, damage, isPet, skill);
		}
		catch (Exception e)
		{
			showError(attacker, e);
			return;
		}
		showResult(npc, attacker, res);
	}
	
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for AttackAct Events.
	 * @param npcIds A serie of ids.
	 */
	public void addAttackActId(int... npcIds)
	{
		for (int npcId : npcIds)
			addEventId(npcId, EventType.ON_ATTACK_ACT);
	}
	
	/**
	 * Quest event notifycator for player being attacked by NPC.
	 * @param npc Npc providing attack.
	 * @param victim Attacked npc player.
	 */
	public final void notifyAttackAct(L2Npc npc, L2PcInstance victim)
	{
		String res = null;
		try
		{
			res = onAttackAct(npc, victim);
		}
		catch (Exception e)
		{
			showError(victim, e);
			return;
		}
		showResult(npc, victim, res);
	}
	
	public String onAttackAct(L2Npc npc, L2PcInstance victim)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Character See Events.
	 * @param npcIds : A serie of ids.
	 */
	public void addAggroRangeEnterId(int... npcIds)
	{
		for (int npcId : npcIds)
			addEventId(npcId, EventType.ON_AGGRO);
	}
	
	private class OnAggroEnter implements Runnable
	{
		private final L2Npc _npc;
		private final L2PcInstance _pc;
		private final boolean _isPet;
		
		public OnAggroEnter(L2Npc npc, L2PcInstance pc, boolean isPet)
		{
			_npc = npc;
			_pc = pc;
			_isPet = isPet;
		}
		
		@Override
		public void run()
		{
			String res = null;
			try
			{
				res = onAggro(_npc, _pc, _isPet);
			}
			catch (Exception e)
			{
				showError(_pc, e);
			}
			showResult(_npc, _pc, res);
			
		}
	}
	
	public final void notifyAggro(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		ThreadPoolManager.getInstance().executeAi(new OnAggroEnter(npc, player, isPet));
	}
	
	public String onAggro(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		return null;
	}
	
	public final void notifyDeath(L2Character killer, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onDeath(killer, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult((killer instanceof L2Npc) ? (L2Npc) killer : null, player, res);
	}
	
	public String onDeath(L2Character killer, L2PcInstance player)
	{
		return onAdvEvent("", (killer instanceof L2Npc) ? (L2Npc) killer : null, player);
	}
	
	public final void notifyEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onAdvEvent(event, npc, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(npc, player, res);
	}
	
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		// if not overridden by a subclass, then default to the returned value of the simpler (and older) onEvent override
		// if the player has a state, use it as parameter in the next call, else return null
		if (player != null)
		{
			QuestState qs = player.getQuestState(getName());
			if (qs != null)
				return onEvent(event, qs);
		}
		return null;
	}
	
	public String onEvent(String event, QuestState qs)
	{
		return null;
	}
	
	public final void notifyEnterWorld(L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onEnterWorld(player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(null, player, res);
	}
	
	public String onEnterWorld(L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that triggers, when player enters specified zones.
	 * @param zoneIds : A serie of zone ids.
	 */
	public void addEnterZoneId(int... zoneIds)
	{
		for (int zoneId : zoneIds)
		{
			final L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
			if (zone != null)
				zone.addQuestEvent(EventType.ON_ENTER_ZONE, this);
		}
	}
	
	public final void notifyEnterZone(L2Character character, L2ZoneType zone)
	{
		L2PcInstance player = character.getActingPlayer();
		String res = null;
		try
		{
			res = onEnterZone(character, zone);
		}
		catch (Exception e)
		{
			if (player != null)
			{
				showError(player, e);
				return;
			}
		}
		if (player != null)
			showResult(null, player, res);
	}
	
	public String onEnterZone(L2Character character, L2ZoneType zone)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that triggers, when player leaves specified zones.
	 * @param zoneIds : A serie of zone ids.
	 */
	public void addExitZoneId(int... zoneIds)
	{
		for (int zoneId : zoneIds)
		{
			final L2ZoneType zone = ZoneManager.getInstance().getZoneById(zoneId);
			if (zone != null)
				zone.addQuestEvent(EventType.ON_EXIT_ZONE, this);
		}
	}
	
	public final void notifyExitZone(L2Character character, L2ZoneType zone)
	{
		L2PcInstance player = character.getActingPlayer();
		String res = null;
		try
		{
			res = onExitZone(character, zone);
		}
		catch (Exception e)
		{
			if (player != null)
			{
				showError(player, e);
				return;
			}
		}
		if (player != null)
			showResult(null, player, res);
	}
	
	public String onExitZone(L2Character character, L2ZoneType zone)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Faction Call Events.
	 * @param npcIds : A serie of ids.
	 */
	public void addFactionCallId(int... npcIds)
	{
		for (int npcId : npcIds)
			addEventId(npcId, EventType.ON_FACTION_CALL);
	}
	
	public final void notifyFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet)
	{
		String res = null;
		try
		{
			res = onFactionCall(npc, caller, attacker, isPet);
		}
		catch (Exception e)
		{
			showError(attacker, e);
			return;
		}
		showResult(npc, attacker, res);
	}
	
	public String onFactionCall(L2Npc npc, L2Npc caller, L2PcInstance attacker, boolean isPet)
	{
		return null;
	}
	
	/**
	 * Add the quest to the NPC's first-talk (default action dialog)
	 * @param npcIds A serie of ids.
	 */
	public void addFirstTalkId(int... npcIds)
	{
		for (int npcId : npcIds)
			addEventId(npcId, EventType.ON_FIRST_TALK);
	}
	
	public final void notifyFirstTalk(L2Npc npc, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onFirstTalk(npc, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		
		// if the quest returns text to display, display it.
		if (res != null && res.length() > 0)
			showResult(npc, player, res);
		else
			player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	public String onFirstTalk(L2Npc npc, L2PcInstance player)
	{
		return null;
	}
	
	/**
	 * Add the quest to an array of items templates.
	 * @param itemIds A serie of ids.
	 */
	public void addItemUse(int... itemIds)
	{
		for (int itemId : itemIds)
		{
			Item t = ItemTable.getInstance().getTemplate(itemId);
			if (t != null)
				t.addQuestEvent(this);
		}
	}
	
	public final void notifyItemUse(ItemInstance item, L2PcInstance player, L2Object target)
	{
		String res = null;
		try
		{
			res = onItemUse(item, player, target);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(null, player, res);
	}
	
	public String onItemUse(ItemInstance item, L2PcInstance player, L2Object target)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed mob will respond to for Kill Events.
	 * @param killIds A serie of ids.
	 */
	public void addKillId(int... killIds)
	{
		for (int killId : killIds)
			addEventId(killId, EventType.ON_KILL);
	}
	
	public final void notifyKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		String res = null;
		try
		{
			res = onKill(npc, killer, isPet);
		}
		catch (Exception e)
		{
			showError(killer, e);
			return;
		}
		showResult(npc, killer, res);
	}
	
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Spawn Events.
	 * @param npcIds : A serie of ids.
	 */
	public void addSpawnId(int... npcIds)
	{
		for (int npcId : npcIds)
			addEventId(npcId, EventType.ON_SPAWN);
	}
	
	public final void notifySpawn(L2Npc npc)
	{
		try
		{
			onSpawn(npc);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onSpawn() in notifySpawn(): " + e.getMessage(), e);
		}
	}
	
	public String onSpawn(L2Npc npc)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Decay Events.
	 * @param npcIds : A serie of ids.
	 */
	public void addDecayId(int... npcIds)
	{
		for (int npcId : npcIds)
			addEventId(npcId, EventType.ON_DECAY);
	}
	
	public final void notifyDecay(L2Npc npc)
	{
		try
		{
			onDecay(npc);
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, "Exception on onDecay() in notifyDecay(): " + e.getMessage(), e);
		}
	}
	
	public String onDecay(L2Npc npc)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Skill-See Events.
	 * @param npcIds : A serie of ids.
	 */
	public void addSkillSeeId(int... npcIds)
	{
		for (int npcId : npcIds)
			addEventId(npcId, EventType.ON_SKILL_SEE);
	}
	
	public class OnSkillSee implements Runnable
	{
		private final L2Npc _npc;
		private final L2PcInstance _caster;
		private final L2Skill _skill;
		private final L2Object[] _targets;
		private final boolean _isPet;
		
		public OnSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
		{
			_npc = npc;
			_caster = caster;
			_skill = skill;
			_targets = targets;
			_isPet = isPet;
		}
		
		@Override
		public void run()
		{
			String res = null;
			try
			{
				res = onSkillSee(_npc, _caster, _skill, _targets, _isPet);
			}
			catch (Exception e)
			{
				showError(_caster, e);
			}
			showResult(_npc, _caster, res);
			
		}
	}
	
	public final void notifySkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		ThreadPoolManager.getInstance().executeAi(new OnSkillSee(npc, caster, skill, targets, isPet));
	}
	
	public String onSkillSee(L2Npc npc, L2PcInstance caster, L2Skill skill, L2Object[] targets, boolean isPet)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to any skill being used by other npcs or players.
	 * @param npcIds : A serie of ids.
	 */
	public void addSpellFinishedId(int... npcIds)
	{
		for (int npcId : npcIds)
			addEventId(npcId, EventType.ON_SPELL_FINISHED);
	}
	
	public final void notifySpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		String res = null;
		try
		{
			res = onSpellFinished(npc, player, skill);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		showResult(npc, player, res);
	}
	
	public String onSpellFinished(L2Npc npc, L2PcInstance player, L2Skill skill)
	{
		return null;
	}
	
	/**
	 * Add this quest to the list of quests that the passed npc will respond to for Talk Events.
	 * @param talkIds : A serie of ids.
	 */
	public void addTalkId(int... talkIds)
	{
		for (int talkId : talkIds)
			addEventId(talkId, EventType.ON_TALK);
	}
	
	public final void notifyTalk(L2Npc npc, L2PcInstance player)
	{
		String res = null;
		try
		{
			res = onTalk(npc, player);
		}
		catch (Exception e)
		{
			showError(player, e);
			return;
		}
		player.setLastQuestNpcObject(npc.getObjectId());
		showResult(npc, player, res);
	}
	
	public String onTalk(L2Npc npc, L2PcInstance talker)
	{
		return null;
	}
	
	@Override
	public boolean equals(Object o)
	{
		// core AIs are available only in one instance (in the list of event of NpcTemplate)
		if (o instanceof L2AttackableAIScript && this instanceof L2AttackableAIScript)
			return true;
		
		if (o instanceof Quest)
		{
			Quest q = (Quest) o;
			if (_id > 0 && _id == q._id)
				return getName().equals(q.getName());
			
			// Scripts may have same names, while being in different sub-package
			return getClass().getName().equals(q.getClass().getName());
		}
		
		return false;
	}
}