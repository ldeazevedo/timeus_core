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
package net.sf.l2j.gameserver.model.vehicles;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.l2j.gameserver.ThreadPoolManager;
import net.sf.l2j.gameserver.instancemanager.BoatManager;
import net.sf.l2j.gameserver.model.Location;
import net.sf.l2j.gameserver.model.VehiclePathPoint;
import net.sf.l2j.gameserver.model.actor.L2Vehicle;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.clientpackets.Say2;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.PlaySound;

public class BoatRunePrimeval implements Runnable
{
	private static final Logger _log = Logger.getLogger(BoatRunePrimeval.class.getName());
	
	private static final Location OUST_LOC_1 = new Location(34513, -38009, -3640);
	private static final Location OUST_LOC_2 = new Location(10447, -24982, -3664);
	
	// Time: 239s
	private static final VehiclePathPoint[] RUNE_TO_PRIMEVAL =
	{
		new VehiclePathPoint(32750, -39300, -3610, 180, 800),
		new VehiclePathPoint(27440, -39328, -3610, 250, 1000),
		new VehiclePathPoint(19616, -39360, -3610, 270, 1000),
		new VehiclePathPoint(3840, -38528, -3610, 270, 1000),
		new VehiclePathPoint(1664, -37120, -3610, 270, 1000),
		new VehiclePathPoint(896, -34560, -3610, 180, 1800),
		new VehiclePathPoint(832, -31104, -3610, 180, 180),
		new VehiclePathPoint(2240, -29132, -3610, 150, 1800),
		new VehiclePathPoint(4160, -27828, -3610, 150, 1800),
		new VehiclePathPoint(5888, -27279, -3610, 150, 1800),
		new VehiclePathPoint(7000, -27279, -3610, 150, 1800),
		new VehiclePathPoint(10342, -27279, -3610, 150, 1800)
	};
	
	// Time: 221s
	private static final VehiclePathPoint[] PRIMEVAL_TO_RUNE =
	{
		new VehiclePathPoint(15528, -27279, -3610, 180, 800),
		new VehiclePathPoint(22304, -29664, -3610, 290, 800),
		new VehiclePathPoint(33824, -26880, -3610, 290, 800),
		new VehiclePathPoint(38848, -21792, -3610, 240, 1200),
		new VehiclePathPoint(43424, -22080, -3610, 180, 1800),
		new VehiclePathPoint(44320, -25152, -3610, 180, 1800),
		new VehiclePathPoint(40576, -31616, -3610, 250, 800),
		new VehiclePathPoint(36819, -35315, -3610, 220, 800)
	};
	
	private static final VehiclePathPoint[] RUNE_DOCK =
	{
		new VehiclePathPoint(34381, -37680, -3610, 220, 800)
	};
	
	private static final VehiclePathPoint PRIMEVAL_DOCK = RUNE_TO_PRIMEVAL[RUNE_TO_PRIMEVAL.length - 1];
	
	private final L2Vehicle _boat;
	private int _cycle = 0;
	private int _shoutCount = 0;
	
	private final CreatureSay ARRIVED_AT_RUNE;
	private final CreatureSay ARRIVED_AT_RUNE_2;
	private final CreatureSay LEAVING_RUNE;
	private final CreatureSay ARRIVED_AT_PRIMEVAL;
	private final CreatureSay ARRIVED_AT_PRIMEVAL_2;
	private final CreatureSay LEAVING_PRIMEVAL;
	private final CreatureSay BUSY_RUNE;
	
	private final PlaySound RUNE_SOUND;
	private final PlaySound PRIMEVAL_SOUND;
	
	public BoatRunePrimeval(L2Vehicle boat)
	{
		_boat = boat;
		
		ARRIVED_AT_RUNE = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.ARRIVED_AT_RUNE);
		ARRIVED_AT_RUNE_2 = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.FERRY_LEAVING_FOR_PRIMEVAL_3_MINUTES);
		LEAVING_RUNE = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.FERRY_LEAVING_RUNE_FOR_PRIMEVAL_NOW);
		ARRIVED_AT_PRIMEVAL = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.FERRY_ARRIVED_AT_PRIMEVAL);
		ARRIVED_AT_PRIMEVAL_2 = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.FERRY_LEAVING_FOR_RUNE_3_MINUTES);
		LEAVING_PRIMEVAL = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.FERRY_LEAVING_PRIMEVAL_FOR_RUNE_NOW);
		BUSY_RUNE = new CreatureSay(0, Say2.BOAT, 801, SystemMessageId.FERRY_FROM_PRIMEVAL_TO_RUNE_DELAYED);
		
		RUNE_SOUND = new PlaySound(0, "itemsound.ship_arrival_departure", 1, _boat.getObjectId(), RUNE_DOCK[0].x, RUNE_DOCK[0].y, RUNE_DOCK[0].z);
		PRIMEVAL_SOUND = new PlaySound(0, "itemsound.ship_arrival_departure", 1, _boat.getObjectId(), PRIMEVAL_DOCK.x, PRIMEVAL_DOCK.y, PRIMEVAL_DOCK.z);
	}
	
	@Override
	public void run()
	{
		try
		{
			switch (_cycle)
			{
				case 0:
					BoatManager.getInstance().dockShip(BoatManager.RUNE_HARBOR, false);
					BoatManager.getInstance().broadcastPackets(RUNE_DOCK[0], PRIMEVAL_DOCK, LEAVING_RUNE, RUNE_SOUND);
					_boat.payForRide(8925, 1, OUST_LOC_1);
					_boat.executePath(RUNE_TO_PRIMEVAL);
					break;
				case 1:
					BoatManager.getInstance().broadcastPackets(PRIMEVAL_DOCK, RUNE_DOCK[0], ARRIVED_AT_PRIMEVAL, ARRIVED_AT_PRIMEVAL_2, PRIMEVAL_SOUND);
					ThreadPoolManager.getInstance().scheduleGeneral(this, 180000);
					break;
				case 2:
					BoatManager.getInstance().broadcastPackets(PRIMEVAL_DOCK, RUNE_DOCK[0], LEAVING_PRIMEVAL, PRIMEVAL_SOUND);
					_boat.payForRide(8924, 1, OUST_LOC_2);
					_boat.executePath(PRIMEVAL_TO_RUNE);
					break;
				case 3:
					if (BoatManager.getInstance().dockBusy(BoatManager.RUNE_HARBOR))
					{
						if (_shoutCount == 0)
							BoatManager.getInstance().broadcastPacket(RUNE_DOCK[0], PRIMEVAL_DOCK, BUSY_RUNE);
						
						_shoutCount++;
						if (_shoutCount > 35)
							_shoutCount = 0;
						
						ThreadPoolManager.getInstance().scheduleGeneral(this, 5000);
						return;
					}
					BoatManager.getInstance().dockShip(BoatManager.RUNE_HARBOR, true);
					_boat.executePath(RUNE_DOCK);
					break;
				case 4:
					BoatManager.getInstance().broadcastPackets(RUNE_DOCK[0], PRIMEVAL_DOCK, ARRIVED_AT_RUNE, ARRIVED_AT_RUNE_2, RUNE_SOUND);
					ThreadPoolManager.getInstance().scheduleGeneral(this, 180000);
					break;
			}
			_shoutCount = 0;
			_cycle++;
			if (_cycle > 4)
				_cycle = 0;
		}
		catch (Exception e)
		{
			_log.log(Level.WARNING, e.getMessage());
		}
	}
	
	public static void load()
	{
		final L2Vehicle boat = BoatManager.getInstance().getNewBoat(5, 34381, -37680, -3610, 40785);
		if (boat != null)
		{
			boat.registerEngine(new BoatRunePrimeval(boat));
			boat.runEngine(180000);
			BoatManager.getInstance().dockShip(BoatManager.RUNE_HARBOR, true);
		}
	}
}