package net.sf.l2j.gameserver.scripting.quests;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.model.L2Clan;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.actor.L2Npc;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.network.serverpackets.NpcSay;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.ScriptManager;

public class Q503_PursuitOfClanAmbition extends Quest
{
	private static final String qn = "Q503_PursuitOfClanAmbition";
	
	// Items
	// first part
	private static final int G_Let_Martien = 3866;
	private static final int Th_Wyrm_Eggs = 3842;
	private static final int Drake_Eggs = 3841;
	private static final int Bl_Wyrm_Eggs = 3840;
	private static final int Mi_Drake_Eggs = 3839;
	private static final int Brooch = 3843;
	private static final int Bl_Anvil_Coin = 3871;
	
	// second Part
	private static final int G_Let_Balthazar = 3867;
	private static final int Recipe_Power_Stone = 3838; // Recipe_Spiteful_Soul_Energy = 14854;
	private static final int Power_Stones = 3846; // Spiteful_Soul_Energy1 = 14855;
	private static final int Nebulite_Crystals = 3844; // Spiteful_Soul_Vengeance1 = 14856;
	private static final int Broke_Power_Stone = 3845;
	
	// third part
	private static final int G_Let_Rodemai = 3868;
	private static final int Imp_Keys = 3847;
	private static final int Scepter_Judgement = 3869;
	
	// the final item
	private static final int Proof_Aspiration = 3870;
	
	private static final int[] EggList = new int[]
	{
		Mi_Drake_Eggs,
		Bl_Wyrm_Eggs,
		Drake_Eggs,
		Th_Wyrm_Eggs
	};
	
	private static final int[] ItemsList = new int[]
	{
		Power_Stones,
		Nebulite_Crystals
	};
	
	// NPCs
	private static final int Gustaf = 30760;
	private static final int Martien = 30645;
	private static final int Athrea = 30758;
	private static final int Kalis = 30759;
	private static final int Fritz = 30761;
	private static final int Lutz = 30762;
	private static final int Kurtz = 30763;
	private static final int Kusto = 30512;
	private static final int Balthazar = 30764;
	private static final int Rodemai = 30868;
	private static final int Coffer = 30765;
	private static final int Cleo = 30766;
	
	// MOBS
	private static final int ThunderWyrm1 = 20282;
	private static final int ThunderWyrm2 = 20243;
	private static final int Drake1 = 20137;
	private static final int Drake2 = 20285;
	private static final int BlitzWyrm = 27178;
	private static final int Giant_Soldier = 20654;
	private static final int Giant_Scouts = 20654;
	private static final int GraveGuard = 20668;
	private static final int GraveKeymaster = 27179;
	private static final int ImperialGravekeeper = 27181;
	private static L2Npc spawnedNpc = null;
	
	public Q503_PursuitOfClanAmbition()
	{
		super(503, "Pursuit of Clan Ambition!");
		
		setItemsIds(G_Let_Martien, Th_Wyrm_Eggs, Drake_Eggs, Bl_Wyrm_Eggs, Mi_Drake_Eggs, Brooch, Bl_Anvil_Coin, G_Let_Balthazar, Broke_Power_Stone, Power_Stones, Nebulite_Crystals, G_Let_Rodemai, Imp_Keys, Scepter_Judgement, Recipe_Power_Stone);

		addStartNpc(Gustaf);
		
		addTalkId(Gustaf, Martien, Athrea, Kalis, Fritz, Lutz, Kurtz, Kusto, Balthazar, Rodemai, Coffer, Cleo);
		addAttackId(ImperialGravekeeper);
		addKillId(ThunderWyrm1, ThunderWyrm2, Drake1, Drake2, BlitzWyrm, Giant_Scouts, Giant_Soldier, GraveGuard, GraveKeymaster, ImperialGravekeeper);
	}
	
	public void suscribe_members(QuestState st)
	{
		int clanId = st.getPlayer().getClan().getClanId();
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement offline = con.prepareStatement("SELECT obj_Id FROM characters WHERE clanid=? AND online=0");
			offline.setInt(1, clanId);
			ResultSet rs = offline.executeQuery();
			while (rs.next())
			{
				int charId = rs.getInt("obj_Id"); //charId
				try
				{
					PreparedStatement insertion = con.prepareStatement("REPLACE INTO character_quests (charId,name,var,value) VALUES (?,?,?,?)");
					insertion.setInt(1, charId);
					insertion.setString(2, qn);
					insertion.setString(3, "<state>");
					insertion.setInt(4, 1);
					insertion.executeUpdate();
					insertion.clearParameters();
					insertion.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			rs.close();
			offline.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, qn + e.getMessage(), e);
		}
	}
	
	public void offlineMemberExit(QuestState st)
	{
		int clan = st.getPlayer().getClan().getClanId();
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement offline = con.prepareStatement("DELETE FROM character_quests WHERE name = ? and charId IN (SELECT obj_Id FROM characters WHERE clanid =? AND online=0)");
			offline.setString(1, qn);
			offline.setInt(2, clan);
			offline.executeUpdate();
			offline.close();
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, qn + e.getMessage(), e);
		}
	}
	
	public L2PcInstance getLeader(QuestState st)
	{
		L2PcInstance player = st.getPlayer();
		if (player == null)
			return null;
		L2Clan clan = player.getClan();
		if (clan == null)
			return null;
		return clan.getLeader().getPlayerInstance();
	}
	
	// returns leaders quest cond, if he is offline will read out of database :)
	@SuppressWarnings("resource")
	public int getLeaderVar(QuestState st, String var)
	{
		final boolean cond = "cond".equalsIgnoreCase(var);
		try
		{
			L2PcInstance leader = getLeader(st);
			if (leader != null)
			{
				if (cond)
					return leader.getQuestState(qn).getInt("cond");
				return leader.getQuestState(qn).getInt(var);
			}
		}
		catch (Exception e)
		{
			return -1;
		}
		
		L2Clan clan = st.getPlayer().getClan();
		
		if (clan == null)
			return -1;
		
		int leaderId = clan.getLeaderId();
		
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement offline = con.prepareStatement("SELECT value FROM character_quests WHERE charId=? AND var=? AND name=?");
			offline.setInt(1, leaderId);
			offline.setString(2, var);
			offline.setString(3, qn);
			int val = -1;
			ResultSet rs = offline.executeQuery();
			while (rs.next())
			{
				val = rs.getInt("value");
				if (cond && (val & 0x80000000) != 0)
				{
					val &= 0x7fffffff;
					for (int i = 1; i < 32; i++)
					{
						val = (val >> 1);
						if (val == 0)
							return i;
					}
				}
			}
			rs.close();
			offline.close();
			return val;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, qn + e.getMessage(), e);
			return -1;
		}
	}
	
	/**
	 * set's leaders quest cond, if he is offline will read out of database :) for now, if the leader is not logged in, this assumes that the variable has already been inserted once (initialized, in some sense).
	 * @param st
	 * @param var
	 * @param value
	 */
	public void setLeaderVar(QuestState st, String var, String value)
	{
		L2Clan clan = st.getPlayer().getClan();
		if (clan == null)
			return;
		L2PcInstance leader = clan.getLeader().getPlayerInstance();
		if (leader != null)
		{
			if ("cond".equalsIgnoreCase(var))
				leader.getQuestState(qn).set("cond", String.valueOf(value));
			else
				leader.getQuestState(qn).set(var, value);
		}
		else
		{
			int leaderId = st.getPlayer().getClan().getLeaderId();
			
			try (Connection con = L2DatabaseFactory.getInstance().getConnection())
			{
				PreparedStatement offline = con.prepareStatement("UPDATE character_quests SET value=? WHERE charId=? AND var=? AND name=?");
				offline.setString(1, value);
				offline.setInt(2, leaderId);
				offline.setString(3, var);
				offline.setString(4, qn);
				offline.executeUpdate();
				offline.close();
			}
			catch (Exception e)
			{
				_log.log(Level.WARNING, "Error: " + qn + ": " + e.getMessage(), e);
			}
		}
	}
	
	public boolean checkEggs(QuestState st)
	{
		int count = 0;
		for (int item : EggList)
			if (st.getQuestItemsCount(item) > 9)
				count += 1;
		return count > 3;
	}
	
	public void giveItem(int item, long maxcount, QuestState st)
	{
		L2PcInstance player = st.getPlayer();
		if (player == null)
			return;
		L2PcInstance leader = getLeader(st);
		if (leader == null)
			return;
		QuestState qs = leader.getQuestState(qn);
		if (qs == null)
			return;
		long count = qs.getQuestItemsCount(item);
		if (count < maxcount)
		{
			qs.giveItems(item, 1);
			if (count == maxcount - 1)
				qs.playSound(QuestState.SOUND_MIDDLE);
			else
				qs.playSound(QuestState.SOUND_ITEMGET);
		}
	}
	
	public String Q503exit(boolean completed, QuestState st)
	{
		if (completed)
		{
			st.giveItems(Proof_Aspiration, 1);
			st.rewardExpAndSp(0, 250000);
			st.unset("cond");
			st.unset("Fritz");
			st.unset("Lutz");
			st.unset("Kurtz");
			st.unset("ImpGraveKeeper");
			st.exitQuest(true); //TODO: ?
		//	st.exitQuest(false);
		}
		else
			st.exitQuest(true);
		st.takeItems(Scepter_Judgement, -1);
		try
		{
			L2PcInstance[] members = st.getPlayer().getClan().getOnlineMembers();
			for (L2PcInstance player : members)
			{
				if (player == null)
					continue;
				QuestState qs = player.getQuestState(qn);
				if (qs != null)
					qs.exitQuest(true);
			}
			offlineMemberExit(st);
		}
		catch (Exception e)
		{
			return "You dont have any members in your Clan, so you can't finish the Pursuit of Aspiration";
		}
		return "Congratulations, you have finished the Pursuit of Clan Ambition";
	}
	
	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		
		// Events Gustaf
		if (event.equalsIgnoreCase("30760-08.htm"))
		{
			st.giveItems(G_Let_Martien, 1);
			st.set("cond", "1");
			st.set("Fritz", "1");
			st.set("Lutz", "1");
			st.set("Kurtz", "1");
			st.set("ImpGraveKeeper", "1");
			st.setState(STATE_STARTED);
		}
		else if (event.equalsIgnoreCase("30760-12.htm"))
		{
			st.giveItems(G_Let_Balthazar, 1);
			st.set("cond", "4");
		}
		else if (event.equalsIgnoreCase("30760-16.htm"))
		{
			st.giveItems(G_Let_Rodemai, 1);
			st.set("cond", "7");
		}
		else if (event.equalsIgnoreCase("30760-20.htm"))
			Q503exit(true, st);
		else if (event.equalsIgnoreCase("30760-22.htm"))
			st.set("cond", "12");
		else if (event.equalsIgnoreCase("30760-23.htm"))
			Q503exit(true, st);
		// Events Martien
		else if (event.equalsIgnoreCase("30645-03.htm"))
		{
			st.takeItems(G_Let_Martien, -1);
			st.set("cond", "2");
			suscribe_members(st);
			L2PcInstance[] members = st.getPlayer().getClan().getOnlineMembers();
			for (L2PcInstance i : members)
			{
				if (i.isClanLeader())
					continue;
				QuestState pst = ScriptManager.getInstance().getQuest(qn).newQuestState(st.getPlayer().getClan().getClanMember(i.getName()).getPlayerInstance());
				pst.setState(STATE_STARTED);
			}
		}
		// Events Kurtz
		else if (event.equalsIgnoreCase("30763-03.htm"))
		{
			if (st.getInt("Kurtz") == 1)
			{
				htmltext = "30763-02.htm";
				st.giveItems(Mi_Drake_Eggs, 6);
				st.giveItems(Brooch, 1);
				st.set("Kurtz", "2");
			}
		}
		// Events Lutz
		else if (event.equalsIgnoreCase("30762-03.htm"))
		{
			int lutz = st.getInt("Lutz");
			if (lutz == 1)
			{
				htmltext = "30762-02.htm";
				st.giveItems(Mi_Drake_Eggs, 4);
				st.giveItems(Bl_Wyrm_Eggs, 3);
				st.set("Lutz", "2");
			}
			addSpawn(BlitzWyrm, npc.getX(), npc.getY(), npc.getZ(), 0, true, 120000, false);
			addSpawn(BlitzWyrm, npc.getX(), npc.getY(), npc.getZ(), 0, true, 120000, false);
		}
		// Events Fritz
		else if (event.equalsIgnoreCase("30761-03.htm"))
		{
			int fritz = st.getInt("Fritz");
			if (fritz == 1)
			{
				htmltext = "30761-02.htm";
				st.giveItems(Bl_Wyrm_Eggs, 3);
				st.set("Fritz", "2");
			}
			addSpawn(BlitzWyrm, npc.getX(), npc.getY(), npc.getZ(), 0, true, 120000, false);
			addSpawn(BlitzWyrm, npc.getX(), npc.getY(), npc.getZ(), 0, true, 120000, false);
		}
		// Events Kusto
		else if (event.equalsIgnoreCase("30512-03.htm"))
		{
			if (st.getPlayer().getInventory().hasAtLeastOneItem(Bl_Anvil_Coin))
				return null;
			st.takeItems(Brooch, -1);
			st.giveItems(Bl_Anvil_Coin, 1);
			st.set("Kurtz", "3");
		}
		// Events Balthazar
		else if (event.equalsIgnoreCase("30764-03.htm"))
		{
			st.takeItems(G_Let_Balthazar, -1);
			st.set("cond", "5");
			st.set("Kurtz", "3");
		}
		else if (event.equalsIgnoreCase("30764-05.htm"))
		{
			st.takeItems(G_Let_Balthazar, -1);
			st.set("cond", "5");
		}
		else if (event.equalsIgnoreCase("30764-06.htm"))
		{
			st.takeItems(Bl_Anvil_Coin, -1);
			st.set("Kurtz", "4");
			st.giveItems(Recipe_Power_Stone, 1);
		}
		// Events Rodemai
		else if (event.equalsIgnoreCase("30868-04.htm"))
		{
			st.takeItems(G_Let_Rodemai, -1);
			st.set("cond", "8");
		}
		else if (event.equalsIgnoreCase("30868-06a.htm"))
			st.set("cond", "10");
		else if (event.equalsIgnoreCase("30868-10.htm"))
			st.set("cond", "12");
		// Events Cleo
		else if (event.equalsIgnoreCase("30766-04.htm"))
		{
			st.set("cond", "9");
			spawnedNpc = addSpawn(30766, 160622, 21230, -3710, 0, false, 90000, false);
			npc.broadcastPacket(new NpcSay(spawnedNpc.getObjectId(), 0, spawnedNpc.getNpcId(), "Blood and Honour."));
			spawnedNpc = addSpawn(30759, 160665, 21209, -3710, 0, false, 90000, false);
			npc.broadcastPacket(new NpcSay(spawnedNpc.getObjectId(), 0, spawnedNpc.getNpcId(), "Ambition and Power"));
			spawnedNpc = addSpawn(30758, 160665, 21291, -3710, 0, false, 90000, false);
			npc.broadcastPacket(new NpcSay(spawnedNpc.getObjectId(), 0, spawnedNpc.getNpcId(), "War and Death"));
		}
		else if (event.equalsIgnoreCase("30766-08.htm"))
		{
			st.takeItems(Scepter_Judgement, -1);
			Q503exit(false, st);
		}
		return htmltext;
	}
	
	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		
		int npcId = npc.getNpcId();
		int id = st.getState();
		
		String htmltext = getNoQuestMsg();
		boolean isLeader = st.getPlayer().isClanLeader();
		if (id == STATE_CREATED && npcId == Gustaf)
		{
			if (st.getPlayer().getClan() != null) // has Clan
			{
				if (isLeader) // check if player is clan leader
				{
					int clanLevel = st.getPlayer().getClan().getLevel();
					if (st.getQuestItemsCount(Proof_Aspiration) > 0) // if he has the proof already, tell him what to do now
						htmltext = "30760-03.htm";
					else if (clanLevel > 3) // if clanLevel > 3 you can take this quest, because repeatable
						htmltext = "30760-04.htm";
					else // if clanLevel < 4 you cant take it
						htmltext = "30760-02.htm";
				}
				else // player isnt a leader
					htmltext = "30760-04t.htm";
			}
			else // no Clan
				htmltext = "30760-01.htm";
			return htmltext;
		}
		else if (st.getPlayer().getClan() != null && st.getPlayer().getClan().getLevel() == 5) // player has level 5 clan already
			return getAlreadyCompletedMsg();
		else
		// Leader Area
		if (isLeader)
		{
			if (st.getInt("cond") == 0 && st.getQuestItemsCount(Proof_Aspiration) == 0)
				st.set("cond", "1");
			if (st.get("Kurtz") == null)
				st.set("Kurtz", "1");
			if (st.get("Lutz") == null)
				st.set("Lutz", "1");
			if (st.get("Fritz") == null)
				st.set("Fritz", "1");
			int cond = st.getInt("cond");
			int kurtz = st.getInt("Kurtz");
			int lutz = st.getInt("Lutz");
			int fritz = st.getInt("Fritz");
			
			switch (npcId)
			{
				case Gustaf:
					if (cond == 1)
						htmltext = "30760-09.htm";
					else if (cond == 2)
						htmltext = "30760-10.htm";
					else if (cond == 3)
						htmltext = "30760-11.htm";
					else if (cond == 4)
						htmltext = "30760-13.htm";
					else if (cond == 5)
						htmltext = "30760-14.htm";
					else if (cond == 6)
						htmltext = "30760-15.htm";
					else if (cond == 7)
						htmltext = "30760-17.htm";
					else if (cond == 11)
						htmltext = "30760-19.htm";
					else if (cond == 12)
						htmltext = "30760-24.htm";
					else
						htmltext = "30760-18.htm";
					break;
				case Martien:
					if (cond == 1)
						htmltext = "30645-02.htm";
					else if (cond == 2)
						if (checkEggs(st) && kurtz > 1 && lutz > 1 && fritz > 1)
						{
							htmltext = "30645-05.htm";
							st.set("cond", "3");
							for (int item : EggList)
								st.takeItems(item, -1);
						}
						else
							htmltext = "30645-04.htm";
					else if (cond == 3)
						htmltext = "30645-07.htm";
					else
						htmltext = "30645-08.htm";
					break;
				case Lutz:
					if (cond == 2)
						htmltext = "30762-01.htm";
					break;
				case Kurtz:
					if (cond == 2)
						htmltext = "30763-01.htm";
					break;
				case Fritz:
					if (cond == 2)
						htmltext = "30761-01.htm";
					break;
				case Kusto:
					if (kurtz == 1)
						htmltext = "30512-01.htm";
					else if (kurtz == 2)
						htmltext = "30512-02.htm";
					else
						htmltext = "30512-04.htm";
					break;
				case Balthazar:
					if (cond == 4)
						if (kurtz > 2)
							htmltext = "30764-04.htm";
						else
							htmltext = "30764-02.htm";
					else if (cond == 5)
						if (st.getQuestItemsCount(Power_Stones) > 9 && st.getQuestItemsCount(Nebulite_Crystals) > 9)
						{
							htmltext = "30764-08.htm";
							st.takeItems(Power_Stones, -1);
							st.takeItems(Nebulite_Crystals, -1);
							st.takeItems(Brooch, -1);
							st.set("cond", "6");
						}
						else
							htmltext = "30764-07.htm";
					else if (cond == 6)
						htmltext = "30764-09.htm";
					break;
				case Rodemai:
					if (cond == 7)
						htmltext = "30868-02.htm";
					else if (cond == 8)
						htmltext = "30868-05.htm";
					else if (cond == 9)
						htmltext = "30868-06.htm";
					else if (cond == 10)
						htmltext = "30868-08.htm";
					else if (cond == 11)
						htmltext = "30868-09.htm";
					else if (cond == 12)
						htmltext = "30868-11.htm";
					break;
				case Cleo:
					if (cond == 8)
						htmltext = "30766-02.htm";
					else if (cond == 9)
						htmltext = "30766-05.htm";
					else if (cond == 10)
						htmltext = "30766-06.htm";
					else if (cond == 11 || cond == 12)
						htmltext = "30766-07.htm";
					break;
				case Coffer:
					if (st.getInt("cond") == 10)
					{
						if (st.getQuestItemsCount(Imp_Keys) < 6)
							htmltext = "30765-03a.htm";
						else if (st.getInt("ImpGraveKeeper") == 3)
						{
							htmltext = "30765-02.htm";
							st.set("cond", "11");
							st.takeItems(Imp_Keys, 6);
							st.giveItems(Scepter_Judgement, 1);
						}
						else
							htmltext = "<html><head><body>(You and your Clan didn't kill the Imperial Gravekeeper by your own, do it try again.)</body></html>";
					}
					else
						htmltext = "<html><head><body>(You already have the Scepter of Judgement.)</body></html>";
					break;
				case Kalis:
					htmltext = "30759-01.htm";
					break;
				case Athrea:
					htmltext = "30758-01.htm";
					break;
			}
			return htmltext;
		}
		// Member Area
		else
		{
			int cond = getLeaderVar(st, "cond");
			switch (npcId)
			{
				case Martien:
					if (cond == 1 || cond == 2 || cond == 3)
						htmltext = "30645-01.htm";
					break;
				case Rodemai:
					if (cond == 9 || cond == 10)
						htmltext = "30868-07.htm";
					else if (cond == 7)
						htmltext = "30868-01.htm";
					break;
				case Balthazar:
					if (cond == 4)
						htmltext = "30764-01.htm";
					break;
				case Cleo:
					if (cond == 8)
						htmltext = "30766-01.htm";
					break;
				case Kusto:
					if (cond > 2 && cond < 6)
						htmltext = "30512-01a.htm";
					break;
				case Coffer:
					if (cond == 10)
						htmltext = "30765-01.htm";
					break;
				case Gustaf:
					if (cond == 3)
						htmltext = "30760-11t.htm";
					else if (cond == 4)
						htmltext = "30760-15t.htm";
					else if (cond == 11)
						htmltext = "30760-19t.htm";
					else if (cond == 12)
						htmltext = "30766-24t.htm";
					break;
			}
			return htmltext;
		}
	}
	
	@Override
	public String onAttack(L2Npc npc, L2PcInstance attacker, int damage, boolean isPet, L2Skill skill)
	{
		QuestState st = attacker.getQuestState(qn);
		
		if (npc.getMaxHp() / 2 > npc.getCurrentHp())
			if (Rnd.get(100) < 4)
			{
				int ImpGraveKepperStat = getLeaderVar(st, "ImpGraveKeeper");
				if (ImpGraveKepperStat == 1)
				{
					for (int i = 1; i <= 4; i++)
						addSpawn(27180, npc, false, 120000, false);
					setLeaderVar(st, "ImpGraveKeeper", "2");
				}
				else
				{
					Object[] players = npc.getKnownList().getKnownObjects().toArray();
					if (players.length > 0)
					{
						L2PcInstance player = (L2PcInstance) players[Rnd.get(players.length)];
						if (player != null)
							player.teleToLocation(185462, 20342, -3250, 30);
					}
				}
			}
		return null;
	}
	
	@Override
	public String onKill(L2Npc npc, L2PcInstance killer, boolean isPet)
	{
		QuestState st = killer.getQuestState(qn);
		int npcId = npc.getNpcId();
		int cond = getLeaderVar(st, "cond");
/*
		QuestState st2 = getClanLeaderQuestState(killer, npc);
		L2Clan clan = killer.getClan();
		if (clan != null)
		{
			if (clan.getLeader() != null)
			{
				L2PcInstance leader = clan.getLeader().getPlayerInstance();
				if (leader != null)
					if (killer.isInsideRadius(leader, 1600, true, false))
						st = st2;
			}
		}*/
		switch (cond)
		{
			case 2:
				switch (npcId)
				{
					case ThunderWyrm1:
						if (Rnd.get(100) < 20)
							giveItem(Th_Wyrm_Eggs, 10, st);
						break;
					
					case ThunderWyrm2:
						if (Rnd.get(100) < 15)
							giveItem(Th_Wyrm_Eggs, 10, st);
						break;
					case Drake1:
						if (Rnd.get(100) < 20)
							giveItem(Drake_Eggs, 10, st);
						break;
					case Drake2:
						if (Rnd.get(100) < 25)
							giveItem(Drake_Eggs, 10, st);
						break;
					case BlitzWyrm:
						giveItem(Bl_Wyrm_Eggs, 10, st);
						break;
				}
				break;
			case 5:
				if (getLeaderVar(st, "Kurtz") < 4)
					return null;
				if (npcId == Giant_Soldier)
				{
					if (Rnd.get(100) < 25)
						for (int item : ItemsList)
							giveItem(item, 10, st);
					st.dropItems(Broke_Power_Stone, 1, 0, 250000);
				}
				else if (npcId == Giant_Scouts)
				{
					if (Rnd.get(100) < 35)
						for (int item : ItemsList)
							giveItem(item, 10, st);
					st.dropItems(Broke_Power_Stone, 1, 0, 350000);
				}
				break;
			case 10:
				switch (npcId)
				{
					case GraveGuard:
						if (Rnd.get(100) < 15)
							addSpawn(GraveKeymaster, npc, false, 120000, false);
						break;
					case GraveKeymaster:
						if (Rnd.get(100) < 80)
							giveItem(Imp_Keys, 6, st);
						break;
					case ImperialGravekeeper:
						spawnedNpc = addSpawn(Coffer, npc, false, 120000, false);
						npc.broadcastPacket(new NpcSay(spawnedNpc.getObjectId(), 0, spawnedNpc.getNpcId(), "Curse of the gods on the one that defiles the property of the empire!"));
						setLeaderVar(st, "ImpGraveKeeper", "3");
						break;
				}
				break;
		}
		return null;
	}
}