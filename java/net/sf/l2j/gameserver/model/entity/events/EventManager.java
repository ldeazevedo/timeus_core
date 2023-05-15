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
package net.sf.l2j.gameserver.model.entity.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import net.sf.l2j.Config;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.datatables.GmListTable;
import net.sf.l2j.gameserver.model.L2World;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.olympiad.OlympiadManager;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage.SMPOS;
import net.sf.l2j.gameserver.network.serverpackets.StopMove;
import net.sf.l2j.gameserver.scripting.ScriptManager;
import net.sf.l2j.gameserver.util.Broadcast;

public class EventManager
{
	protected static final Logger _log = Logger.getLogger(EventManager.class.getName());
	
	public Vector<L2PcInstance> players = new Vector<>();
	public State state = State.INACTIVE;
	public Events event = Events.NULL;
	
	int StateEvent = 0;
	
	public L2PcInstance _activeChar;
	
	enum State
	{
		INACTIVE,
		REGISTER,
		LOADING,
		FIGHT,
		ENDING
	}
	
	enum Events
	{
		NULL,
		SURVIVAL,
		RF,
		DM
	}
	
	private String[] ignorePlayers =
	{
		"MiniBlaster",
		"Panthominum"
	};
	
	protected EventManager()
	{
	}
	
	public void removePlayer(L2PcInstance player)
	{
		synchronized (players)
		{
			players.remove(player);
		}
	}
	
	public boolean addPlayer(L2PcInstance player)
	{
		if (player == null)
			return false;
		
		synchronized (players)
		{
			players.add(player);
		}
		return true;
	}
	
	public boolean containsPlayer(L2PcInstance player)
	{
		synchronized (players)
		{
			return players.contains(player);
		}
	}
	
	public static void getBuffs(L2PcInstance killer)
	{
		killer.getSkill(1204, 2); // Wind Walk
		if (killer.isMageClass())
			killer.getSkill(1085, 3);// Acumen
		else
			killer.getSkill(1086, 2); // haste
	}
	
	public class Msg implements Runnable
	{
		private String _Msg;
		private int _Time;
		
		public Msg(String msg, int time)
		{
			_Msg = msg;
			_Time = time;
		}
		
		@Override
		public void run()
		{
			sendMsg(_Msg, _Time);
		}
		
		void sendMsg(String msg, int time)
		{
			for (L2PcInstance player : L2World.getInstance().getPlayers())
				player.sendPacket(new ExShowScreenMessage(msg, time, SMPOS.TOP_CENTER, false));
		}
	}
	
	public boolean isInEvent(L2PcInstance pc)
	{
		return state == State.FIGHT && containsPlayer(pc);
	}
	
	public void announce(String msg)
	{
		Broadcast.announceToOnlinePlayers(msg);
	}
	
	public boolean isInProgress()
	{
		return state != State.INACTIVE;
	}
	
	public void checkTimeusEvents(String text, L2PcInstance player)
	{
		if (!isInProgress() || player == null || player.isInOlympiadMode() || player.isFestivalParticipant() || player.isInSiege() || player.isInJail() || player.isFestivalParticipant() || player.isCursedWeaponEquipped() || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(player.getObjectId()) || player.getKarma() > 0)
			return;
		
		if (OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendMessage("No puedes participar ni ver el evento mientras estas registrado en oly.");
			return;
		}
		
		if (text.equalsIgnoreCase(".salir") && isInProgress())
		{
			if (!containsPlayer(player) || state != State.FIGHT)
				return;
			if (player.isDead())
			{
				removePlayer(player);
				revertPlayer(player);
			}
			return;
		}
		
		if (text.equalsIgnoreCase(".ver"))
		{
			if (containsPlayer(player) || player.inObserverMode())
				return;
			
			if (event == Events.RF)
				player.enterObserverMode(179747, 54696, -2805);
			else if (event == Events.SURVIVAL)
				player.enterObserverMode(85574, 256964, -11674);
			return;
		}
		
		if (text.equalsIgnoreCase(".register"))
		{
			if (player.isDead())
				return;
			if (player._active_boxes >= 1)
			{
				List<String> boxes = player.active_boxes_characters;
				
				if (boxes != null && boxes.size() > 1)
					for (String name : boxes)
					{
						L2PcInstance pc = L2World.getInstance().getPlayer(name);
						if (pc == null || pc.isGM())
							continue;
						
						for (String ignore : ignorePlayers)
							if (ignore.equalsIgnoreCase(ignore))
								if (player != pc && pc.getName().equalsIgnoreCase(ignore) && containsPlayer(L2World.getInstance().getPlayer(ignore)))
									continue;
						
						if (containsPlayer(pc))
						{
							player.sendMessage("Ya estas parcitipando con otro personaje!");
							return;
						}
					}
			}
			if (player.inObserverMode())
			{
				player.sendMessage("No te podes anotar si estas mirando el evento.");
				return;
			}
			if (containsPlayer(player))
			{
				player.sendMessage("Ya estas registrado en el evento.");
				return;
			}
			if (state != State.REGISTER)
			{
				player.sendMessage("El evento ya comenzo.");
				return;
			}
			addPlayer(player);
			player.sendMessage("Te registraste al evento!");
			return;
		}
		if (text.equalsIgnoreCase(".unregister"))
		{
			if (!containsPlayer(player))
			{
				player.sendMessage("No te registraste al evento.");
				return;
			}
			if (state != State.REGISTER)
			{
				player.sendMessage("El evento ya comenzo.");
				return;
			}
			removePlayer(player);
			player.sendMessage("Saliste del evento.");
			return;
		}
	}
	
	public void revertPlayer(L2PcInstance player)
	{
		if (player.atEvent)
			player.atEvent = false;
		if (player.isInSurvival)
			player.isInSurvival = false;
		if (player.isDead())
			player.doRevive();
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentMp(player.getMaxMp());
		player.broadcastUserInfo();
		
		if (player.getLastLocation() != null)
			player.teleToLocation(player.getLastLocation(), 0);
		else
			player.teleToLocation(82698, 148638, -3473, 0);
		
		if (player.getKarma() > 0)
			player.setKarma(0);
		
		player.setPvpFlag(0);
		player.setTeam(0);
	}
	
	private class RevertTask implements Runnable
	{
		RevertTask()
		{
		}
		
		@Override
		public void run()
		{
			if (!players.isEmpty())
				for (L2PcInstance p : players)
				{
					if (p == null)
						continue;

					if (state == State.FIGHT || state == State.ENDING)
					{
						revertPlayer(p);
						if (StateEvent == 3)
							for (L2PcInstance player : players)
							{
								player.stopAbnormalEffect(0x0200);
								player.setIsImmobilized(false);
								player.setIsInvul(false);
								setCurrentHpMpCp(player);
							}
					}
				}
			clean();
		}
	}
	
	public boolean onKill(L2PcInstance pc, L2PcInstance pk)
	{
		boolean isInEvent = false;
		if (isInProgress() && state == State.FIGHT)
		{
			if (event == Events.SURVIVAL && StateEvent == 4)
			{
				if (pc != null)
				{
					if (pc != pk)
						pk.addAncientAdena("Survival", 25000, pk, true);
					pc.sendPacket(new ExShowScreenMessage("Para regresar escribir .salir o esperar a que termine el evento", 5000, SMPOS.MIDDLE_RIGHT, false));
					pc.isInSurvival = false;
				}
				boolean allDead = true;
				synchronized (players)
				{
					for (L2PcInstance player : players)
					{
						if (pk.equals(player))
							continue;
						if (player.isInSurvival)
							allDead = false;
					}
				}
				
				if (allDead && state != State.ENDING)
				{
					state = State.ENDING;
					if (pk != null)
					{
						pk.sendMessage("Sos el ganador!");
						announce("Resultado Survival: " + pk.getName() + " es el ganador.");
						announce("Evento finalizado");
						pk.addItem("", Config.RANDOM_FIGHT_REWARD_ID, 2, null, true);
						//pk.addAncientAdena("Survival", 100000, pk, true);
					}
					ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 15000);
				}
				isInEvent = true;
			}
			if (event == Events.RF && state != State.ENDING && StateEvent == 5)
			{
				for (L2PcInstance player : players)
				{
					if (player.isDead())
						pc = player;
					if (!player.isDead())
						pk = player;
				}
				state = State.ENDING;
				if (pk != null)
				{
					pk.sendMessage("Sos el ganador!");
					announce("Resultado Random Fight: " + pk.getName() + " es el ganador.");
					announce("Evento finalizado");
					pk.addItem("", Config.RANDOM_FIGHT_REWARD_ID, Config.RANDOM_FIGHT_REWARD_COUNT, null, true);
					
					// Guardar en la base de datos
					try (Connection con = L2DatabaseFactory.getInstance().getConnection())
					{
						PreparedStatement statement = con.prepareStatement("select * from rf where char_name=?");
						statement.setString(1, pk.getName());
						PreparedStatement statement2 = con.prepareStatement(statement.executeQuery().first() ? "update rf set count=count+1 where char_name=?" : "insert rf set count=1,char_name=?");
						statement2.setString(1, pk.getName());
						statement2.execute();
						statement2.close();
						statement.close();
					}
					catch (Exception e)
					{
						_log.warning("Error en RF Ranking: " + e);
					}
				}
				ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 15000);
				
				isInEvent = true;
			}
		}
		return isInEvent;
	}
	
	public void onLogout(L2PcInstance pc)
	{
		L2PcInstance pk = null;
		int alive = 0;
		if (pc != null)
		{
			if (containsPlayer(pc) || pc.atEvent || pc.isInSurvival)
			{
				Location loc = pc.getLastLocation();
				if (loc != null)
					pc.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
				else
					pc.setXYZInvisible(82698, 148638, -3473);
			}
			if (containsPlayer(pc))
				removePlayer(pc);
			
			pc.atEvent = false;
			pc.isInSurvival = false;
		}
		synchronized (players) // cuando un player se desconecta se redirecciona a onkill si solo queda un pj vivo en el evento
		{
			for (L2PcInstance player : players)
				if (!player.isDead() || player.isInSurvival)
				{
					alive++;
					pk = player;
				}
			
			if (alive == 1)
				onKill(null, pk);
			else
				alive = 0;
		}
	}
	
	public boolean reqPlayers()
	{
		return players.isEmpty() || players.size() < 2;
	}
	
	public void clean()
	{
		if (state == State.FIGHT)
			for (L2PcInstance p : players)
				p.setTeam(0);
		
		for (L2PcInstance pc : L2World.getInstance().getPlayers())
		{
			pc.isInSurvival = false;
			pc.atEvent = false;
		}
		
		players.clear();
		state = State.INACTIVE;
		event = Events.NULL;

		StateEvent = 0;
		ScriptManager.getInstance().getQuest("EventsTask").startQuestTimer("cancelQuestTimers", 1000, null, null, false);
	}
	
	public void clear()
	{
		clean();
		ScriptManager.getInstance().getQuest("EventsTask").startQuestTimer("clear", 1000, null, null, false);
	}
	
	public void setSurvival(int stage)
	{
		if (TvTEvent.isInProgress() || event == Events.RF || event == Events.DM)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 15000);
			return;
		}
		switch (stage)
		{
			case 0:
				if (state == State.INACTIVE && event == Events.NULL)
				{
					event = Events.SURVIVAL;
					state = State.REGISTER;
					StateEvent = 1;
					for (L2PcInstance player : L2World.getInstance().getPlayers())
						player.sendPacket(new ExShowScreenMessage("Evento Survival empezara en 1 minuto", 5000, SMPOS.TOP_CENTER, false));
					ThreadPoolManager.getInstance().scheduleGeneral(new Msg("Para registrarte escribi .register", 5000), 5000);
					ThreadPoolManager.getInstance().scheduleGeneral(new Msg("Para ver la pela escribi .ver", 5000), 10000);
					announce("Evento Survival empezara en 1 minuto");
					announce("Para registrarte, escribi .register");
					announce("Para mirar la pelea, escribi .ver");
				}
				break;
			case 1:
				if (state == State.REGISTER && event == Events.SURVIVAL && StateEvent == 1)
				{
					state = State.LOADING;
					Vector<L2PcInstance> newPlayers = new Vector<>();
					for (L2PcInstance p : players)
						newPlayers.add(p);
					for (L2PcInstance p : players)
						if (p.isInOlympiadMode() || p.inObserverMode() || OlympiadManager.getInstance().isRegistered(p) && p.getKarma() > 0 || p.isCursedWeaponEquipped() || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(p.getObjectId()))
						{
							newPlayers.remove(p);
							p.sendMessage("no cumples los requisitos para participar en el evento.");
						}
					players = newPlayers;
					if (reqPlayers())
					{
						announce("Survival no comenzara por que faltan participantes.");
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 1000);
						return;
					}
					
					announce("Cantidad de registrados: " + players.size());
					announce("Los personajes seran teleportados en 15 segundos.");
					StateEvent = 2;
				}
				break;
			case 2:
				if (state == State.LOADING && event == Events.SURVIVAL && StateEvent == 2)
				{
					if (reqPlayers())
					{
						announce("Survival no comenzara por que faltan participantes.");
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 1000);
						return;
					}
					
					for (L2PcInstance player : players)
						setPcPrepare(player);
					StateEvent = 3;
				}
				break;
			case 3:
				if (state == State.LOADING && event == Events.SURVIVAL && StateEvent == 3)
				{
					if (reqPlayers())
					{
						announce("Uno de los personajes no esta Online, se cancela el evento.");
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 1000);
						return;
					}
					state = State.FIGHT;
					for (L2PcInstance player : players)
					{
						player.sendMessage("Pelea! Tenes 5 minutos para ganar!");
						player.stopAbnormalEffect(0x0200);
						player.setIsImmobilized(false);
						player.setIsInvul(false);
						player.isInSurvival = true;
						setCurrentHpMpCp(player);
					}
					StateEvent = 4;
				}
				break;
			case 4:
				if (state == State.FIGHT && event == Events.SURVIVAL && StateEvent == 4)
				{
					if (reqPlayers())
					{
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 15000);
						return;
					}
					
					int alive = 0;
					for (L2PcInstance player : players)
						if (player.isInSurvival)
							alive++;
					
					if (alive >= 2)
					{
						state = State.ENDING;
						announce("[Survival] No hubo ganador, no hay premio!");
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 15000);
					}
				}
				break;
		}
	}
	
	public void setRandomFight(int _status)
	{
		if (TvTEvent.isInProgress() || event == Events.SURVIVAL || event == Events.DM)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 15000);
			return;
		}
		switch (_status)
		{
			case 0:
				if (state == State.INACTIVE && event == Events.NULL && StateEvent == 0)
				{
					StateEvent = 1;
					event = Events.RF;
					state = State.REGISTER;
					for (L2PcInstance player : L2World.getInstance().getPlayers())
						player.sendPacket(new ExShowScreenMessage("Evento Random Fight empezara en 1 minuto", 5000, SMPOS.TOP_CENTER, false));
					ThreadPoolManager.getInstance().scheduleGeneral(new Msg("Para registrarte escribi .register", 5000), 5000);
					ThreadPoolManager.getInstance().scheduleGeneral(new Msg("Para ver la pela escribi .ver", 5000), 10000);
					announce("Evento Random Fight empezara en 1 minuto");
					announce("Para registrarte, escribi .register");
					announce("Para mirar la pelea, escribi .ver");
				}
				break;
			case 1:
				if (state == State.REGISTER && event == Events.RF && StateEvent == 1)
				{
					state = State.LOADING;
					
					if (reqPlayers())
					{
						announce("Random Fight no comenzara por que faltan participantes.");
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 1000);
						return;
					}
					
					announce("Cantidad de registrados: " + players.size());
					announce("2 personajes al azar seran elegidos en 10 segundos!");
					StateEvent = 2;
				}
				break;
			case 2:
				if (state == State.LOADING && event == Events.RF && StateEvent == 2)
				{
					try
					{
						if (reqPlayers())
						{
							announce("Random Fight no comenzara por que faltan participantes.");
							ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 1000);
							return;
						}
						
						Vector<L2PcInstance> newPlayers = new Vector<>();
						for (L2PcInstance p : players)
							newPlayers.add(p);
						for (L2PcInstance p : players)
							if (p.isInOlympiadMode() || p.inObserverMode() || OlympiadManager.getInstance().isRegistered(p) && p.getKarma() > 0 || p.isCursedWeaponEquipped() || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(p.getObjectId()))
							{
								newPlayers.remove(p);
								p.sendMessage("no cumples los requisitos para participar en el evento.");
							}
						players = newPlayers;
						
						int rnd1 = Rnd.get(players.size());
						int rnd2 = Rnd.get(players.size());
						
						while (rnd2 == rnd1)
							rnd2 = Rnd.get(players.size());
						
						Vector<L2PcInstance> players2 = new Vector<>();
						for (L2PcInstance player : players)
							if (player != players.get(rnd1) && player != players.get(rnd2))
								players2.add(player);
						for (L2PcInstance player : players2)
							if (players.contains(player))
								players.remove(player);
						
						announce("Personajes elegidos: " + players.firstElement().getName() + " || " + players.lastElement().getName());
						announce("Los personajes seran teleportados en 15 segundos.");
						StateEvent = 3;
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
				break;
			case 3:
				if (state == State.LOADING && event == Events.RF && StateEvent == 3)
				{
					if (reqPlayers())
					{
						announce("Random Fight no comenzara por que faltan participantes.");
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 1000);
						return;
					}
					
					L2PcInstance player1 = players.firstElement();
					L2PcInstance player2 = players.lastElement();
					
					setPcPrepare(player1);
					setPcPrepare(player2);
					
					// Arriba de GC
					player1.teleToLocation(179621, 54371, -3093, 0);
					player2.teleToLocation(178167, 54851, -3093, 0);
					player1.setTeam(1);
					player2.setTeam(2);
					
					state = State.FIGHT;
					StateEvent = 4;
				}
				break;
			case 4:
				if (state == State.FIGHT && event == Events.RF && StateEvent == 4)
				{
					if (reqPlayers())
					{
						announce("Uno de los personajes no esta Online, se cancela el evento.");
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 15000);
						return;
					}

					for (L2PcInstance player : players)
					{
						player.sendMessage("Pelea!");
						player.stopAbnormalEffect(0x0200);
						player.setIsImmobilized(false);
						player.setIsInvul(false);
						setCurrentHpMpCp(player);
					}
					StateEvent = 5;
				}
				break;
			case 5:
				if (state == State.FIGHT && event == Events.RF && StateEvent == 5)
				{
					if (reqPlayers())
					{
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 15000);
						return;
					}
					
					int alive = 0;
					for (L2PcInstance player : players)
						if (!player.isDead())
							alive++;
					
					if (alive == 2)
					{
						state = State.ENDING;
						_log.info("ENDING");
						announce("[RandomFight] Termino en empate!");
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 15000);
					}
				}
				break;
		}
	}
	
	public static EventManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final EventManager _instance = new EventManager();
	}
	
	public boolean blockAllChat = false;
	private ScheduledFuture<?> _chatTask = null;

	public void setBlock()
	{
		if (!blockAllChat)
		{
			restartChatTask();
			blockAllChat = true;
		}
		else
			clearChat();
		GmListTable.broadcastMessageToGMs(blockAllChat ? "chats desactivados" : "chats activados");
	}
	
	public void restartChatTask()
	{
		stopChatTask();
		_chatTask = ThreadPoolManager.getInstance().scheduleGeneral(new ChatTask(), 60000*10);
		announce("Los chats quedan desactivados temporalmente por el administrador");
	}
	
	public void stopChatTask()
	{
		if (_chatTask != null)
		{
			_chatTask.cancel(false);
			_chatTask = null;
		}
	}
	
	protected class ChatTask implements Runnable
	{
		@Override
		public void run()
		{
			clearChat();
		}
	}
	
	public void clearChat()
	{
		stopChatTask();
		blockAllChat = false;
		announce("Los chats estan activados");
	}
	
	private void setPcPrepare(L2PcInstance player)
	{
		player.setLastLocation(new Location(player.getX(), player.getY(), player.getZ()));
		player.atEvent = true;
		if (state == State.LOADING && event == Events.SURVIVAL && StateEvent == 2)
		{
			player.setIsInvul(true);
			player.teleToLocation(85574, 256964, -11674, Rnd.get(200, 1800));
		}
		player.stopAllEffectsExceptThoseThatLastThroughDeath();
		if (player.getPet() != null)
			player.getPet().stopAllEffectsExceptThoseThatLastThroughDeath();
		player.startAbnormalEffect(0x0200);
		player.setIsImmobilized(true);
		player.broadcastPacket(new StopMove(player));
		getBuffs(player);
		setCurrentHpMpCp(player);
		String message = "La pelea comenzara en 30 segundos!";
		player.sendMessage(message);
		player.sendPacket(new ExShowScreenMessage(message, 3500, SMPOS.MIDDLE_RIGHT, false));
	}
	
	public static void setCurrentHpMpCp(L2PcInstance player)
	{
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
	}
	
	public void setDM(int stage)
	{
		if (TvTEvent.isInProgress() || event == Events.RF || event == Events.SURVIVAL)
		{
			ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 15000);
			return;
		}
		switch (stage)
		{
			case 0:
				if (state == State.INACTIVE && event == Events.NULL)
				{
					event = Events.DM;
					state = State.REGISTER;
					StateEvent = 1;
					for (L2PcInstance player : L2World.getInstance().getPlayers())
						player.sendPacket(new ExShowScreenMessage("Evento Death Match empezara en 1 minuto", 5000, SMPOS.TOP_CENTER, false));
					ThreadPoolManager.getInstance().scheduleGeneral(new Msg("Para registrarte escribi .register", 5000), 5000);
					ThreadPoolManager.getInstance().scheduleGeneral(new Msg("Para ver la pela escribi .ver", 5000), 10000);
					announce("Evento Death Match empezara en 1 minuto");
					announce("Para registrarte, escribi .register");
					announce("Para mirar la pelea, escribi .ver");
				}
				break;
			case 1:
				if (state == State.REGISTER && event == Events.DM && StateEvent == 1)
				{
					state = State.LOADING;
					Vector<L2PcInstance> newPlayers = new Vector<>();
					for (L2PcInstance p : players)
						newPlayers.add(p);
					for (L2PcInstance p : players)
						if (p.isInOlympiadMode() || p.inObserverMode() || OlympiadManager.getInstance().isRegistered(p) && p.getKarma() > 0 || p.isCursedWeaponEquipped() || TvTEvent.isInProgress() && TvTEvent.isPlayerParticipant(p.getObjectId()))
						{
							newPlayers.remove(p);
							p.sendMessage("no cumples los requisitos para participar en el evento.");
						}
					players = newPlayers;
					if (reqPlayers())
					{
						announce("Death Match no comenzara por que faltan participantes.");
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 1000);
						return;
					}
					
					announce("Cantidad de registrados: " + players.size());
					announce("Los personajes seran teleportados en 15 segundos.");
					StateEvent = 2;
				}
				break;
			case 2:
				if (state == State.LOADING && event == Events.DM && StateEvent == 2)
				{
					if (reqPlayers())
					{
						announce("Death Match no comenzara por que faltan participantes.");
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 1000);
						return;
					}
					
					for (L2PcInstance player : players)
						setPcPrepare(player);
					StateEvent = 3;
				}
				break;
			case 3:
				if (state == State.LOADING && event == Events.DM && StateEvent == 3)
				{
					if (reqPlayers())
					{
						announce("Death Match no comenzara por que faltan participantes.");
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 1000);
						return;
					}
					state = State.FIGHT;
					for (L2PcInstance player : players)
					{
						player.sendMessage("Pelea! Tenes 5 minutos para matar!");
						player.stopAbnormalEffect(0x0200);
						player.setIsImmobilized(false);
						player.setIsInvul(false);
						player.isInSurvival = true;
						setCurrentHpMpCp(player);
					}
					StateEvent = 4;
				}
				break;
			case 4:
				if (state == State.FIGHT && event == Events.DM && StateEvent == 4)
				{
					if (reqPlayers())
					{
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 15000);
						return;
					}
					
					int alive = 0;
					for (L2PcInstance player : players)
						if (player.isInSurvival)
							alive++;
					
					if (alive >= 2)
					{
						state = State.ENDING;
						announce("Death Match No hubo ganador, no hay premio!");
						ThreadPoolManager.getInstance().scheduleGeneral(new RevertTask(), 15000);
					}
				}
				break;
		}
	}
	
}