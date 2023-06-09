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
package net.sf.l2j.loginserver;

import java.io.File;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.commons.random.Rnd;
import net.sf.l2j.gameserver.xmlfactory.XMLDocumentFactory;
import net.sf.l2j.loginserver.network.gameserverpackets.ServerStatus;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class GameServerTable
{
	private static final Logger _log = Logger.getLogger(GameServerTable.class.getName());
	
	private static final int KEYS_SIZE = 10;
	
	private final Map<Integer, String> _serverNames = new HashMap<>();
	private final Map<Integer, GameServerInfo> _gameServerTable = new ConcurrentHashMap<>();
	
	private KeyPair[] _keyPairs;
	
	protected GameServerTable()
	{
		loadServerNames();
		_log.info("Loaded " + _serverNames.size() + " server names.");
		
		loadRegisteredGameServers();
		_log.info("Loaded " + _gameServerTable.size() + " registered gameserver(s).");
		
		initRSAKeys();
		_log.info("Cached " + _keyPairs.length + " RSA keys for gameserver communication.");
	}
	
	private void initRSAKeys()
	{
		try
		{
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(new RSAKeyGenParameterSpec(512, RSAKeyGenParameterSpec.F4));
			
			_keyPairs = new KeyPair[KEYS_SIZE];
			for (int i = 0; i < KEYS_SIZE; i++)
				_keyPairs[i] = keyGen.genKeyPair();
		}
		catch (Exception e)
		{
			_log.severe("GameServerTable: Error loading RSA keys for Game Server communication!");
		}
	}
	
	private void loadServerNames()
	{
		try
		{
			File f = new File("servername.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);
			
			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("server"))
				{
					NamedNodeMap attrs = d.getAttributes();
					
					int id = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
					String name = attrs.getNamedItem("name").getNodeValue();
					
					_serverNames.put(id, name);
				}
			}
		}
		catch (Exception e)
		{
			_log.warning("GameServerTable: servername.xml could not be loaded.");
		}
	}
	
	private void loadRegisteredGameServers()
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT * FROM gameservers");
			ResultSet rset = statement.executeQuery();
			
			while (rset.next())
			{
				final int id = rset.getInt("server_id");
				
				_gameServerTable.put(id, new GameServerInfo(id, stringToHex(rset.getString("hexid"))));
			}
			rset.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.severe("GameServerTable: Error loading registered game servers!");
		}
	}
	
	public Map<Integer, GameServerInfo> getRegisteredGameServers()
	{
		return _gameServerTable;
	}
	
	public boolean registerWithFirstAvailableId(GameServerInfo gsi)
	{
		for (int id : _serverNames.keySet())
		{
			if (!_gameServerTable.containsKey(id))
			{
				_gameServerTable.put(id, gsi);
				gsi.setId(id);
				return true;
			}
		}
		return false;
	}
	
	public boolean register(int id, GameServerInfo gsi)
	{
		if (!_gameServerTable.containsKey(id))
		{
			_gameServerTable.put(id, gsi);
			gsi.setId(id);
			return true;
		}
		return false;
	}
	
	public void registerServerOnDB(GameServerInfo gsi)
	{
		registerServerOnDB(gsi.getHexId(), gsi.getId(), gsi.getExternalHost());
	}
	
	public void registerServerOnDB(byte[] hexId, int id, String externalHost)
	{
		try (Connection con = L2DatabaseFactory.getInstance().getConnection())
		{
			PreparedStatement statement = con.prepareStatement("INSERT INTO gameservers (hexid,server_id,host) values (?,?,?)");
			statement.setString(1, hexToString(hexId));
			statement.setInt(2, id);
			statement.setString(3, externalHost);
			statement.executeUpdate();
			statement.close();
		}
		catch (SQLException e)
		{
			_log.warning("GameServerTable: SQL error while saving gameserver: " + e);
		}
	}
	
	public Map<Integer, String> getServerNames()
	{
		return _serverNames;
	}
	
	public KeyPair getKeyPair()
	{
		return _keyPairs[Rnd.get(10)];
	}
	
	private static byte[] stringToHex(String string)
	{
		return new BigInteger(string, 16).toByteArray();
	}
	
	private static String hexToString(byte[] hex)
	{
		return (hex == null) ? "null" : new BigInteger(hex).toString(16);
	}
	
	public static class GameServerInfo
	{
		// auth
		private int _id;
		private final byte[] _hexId;
		private boolean _isAuthed;
		
		// status
		private GameServerThread _gst;
		private int _status;
		
		// network
		private String _internalIp;
		private String _externalIp;
		private String _externalHost;
		private int _port;
		
		// config
		private final boolean _isPvp = true;
		private boolean _isTestServer;
		private boolean _isShowingClock;
		private boolean _isShowingBrackets;
		private int _maxPlayers;
		
		public GameServerInfo(int id, byte[] hexId, GameServerThread gst)
		{
			_id = id;
			_hexId = hexId;
			_gst = gst;
			_status = ServerStatus.STATUS_DOWN;
		}
		
		public GameServerInfo(int id, byte[] hexId)
		{
			this(id, hexId, null);
		}
		
		public void setId(int id)
		{
			_id = id;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public byte[] getHexId()
		{
			return _hexId;
		}
		
		public void setAuthed(boolean isAuthed)
		{
			_isAuthed = isAuthed;
		}
		
		public boolean isAuthed()
		{
			return _isAuthed;
		}
		
		public void setGameServerThread(GameServerThread gst)
		{
			_gst = gst;
		}
		
		public GameServerThread getGameServerThread()
		{
			return _gst;
		}
		
		public void setStatus(int status)
		{
			_status = status;
		}
		
		public int getStatus()
		{
			return _status;
		}
		
		public int getCurrentPlayerCount()
		{
			if (_gst == null)
				return 0;
			return _gst.getPlayerCount();
		}
		
		public void setInternalIp(String internalIp)
		{
			_internalIp = internalIp;
		}
		
		public String getInternalHost()
		{
			return _internalIp;
		}
		
		public void setExternalIp(String externalIp)
		{
			_externalIp = externalIp;
		}
		
		public String getExternalIp()
		{
			return _externalIp;
		}
		
		public void setExternalHost(String externalHost)
		{
			_externalHost = externalHost;
		}
		
		public String getExternalHost()
		{
			return _externalHost;
		}
		
		public int getPort()
		{
			return _port;
		}
		
		public void setPort(int port)
		{
			_port = port;
		}
		
		public void setMaxPlayers(int maxPlayers)
		{
			_maxPlayers = maxPlayers;
		}
		
		public int getMaxPlayers()
		{
			return _maxPlayers;
		}
		
		public boolean isPvp()
		{
			return _isPvp;
		}
		
		public void setTestServer(boolean val)
		{
			_isTestServer = val;
		}
		
		public boolean isTestServer()
		{
			return _isTestServer;
		}
		
		public void setShowingClock(boolean clock)
		{
			_isShowingClock = clock;
		}
		
		public boolean isShowingClock()
		{
			return _isShowingClock;
		}
		
		public void setShowingBrackets(boolean val)
		{
			_isShowingBrackets = val;
		}
		
		public boolean isShowingBrackets()
		{
			return _isShowingBrackets;
		}
		
		public void setDown()
		{
			setAuthed(false);
			setPort(0);
			setGameServerThread(null);
			setStatus(ServerStatus.STATUS_DOWN);
		}
	}
	
	/**
	 * Gets the single instance of GameServerTable.
	 * @return single instance of GameServerTable
	 */
	public static GameServerTable getInstance()
	{
		return SingletonHolder._instance;
	}
	
	/**
	 * The Class SingletonHolder.
	 */
	private static class SingletonHolder
	{
		protected static final GameServerTable _instance = new GameServerTable();
	}
}