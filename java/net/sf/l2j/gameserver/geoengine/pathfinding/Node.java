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
package net.sf.l2j.gameserver.geoengine.pathfinding;

import net.sf.l2j.gameserver.geoengine.geodata.GeoLocation;

/**
 * @author Hasha
 */
public class Node
{
	// node coords and nswe flag
	private GeoLocation _loc;
	
	// node parent (for reverse path construction)
	private Node _parent;
	// node child (for moving over nodes during iteration)
	private Node _child;
	
	// node G cost (movement cost = parent movement cost + current movement cost)
	private double _cost = -1000;
	
	public void setLoc(int x, int y, int z)
	{
		_loc = new GeoLocation(x, y, z);
	}
	
	public GeoLocation getLoc()
	{
		return _loc;
	}
	
	public void setParent(Node parent)
	{
		_parent = parent;
	}
	
	public Node getParent()
	{
		return _parent;
	}
	
	public void setChild(Node child)
	{
		_child = child;
	}
	
	public Node getChild()
	{
		return _child;
	}
	
	public void setCost(double cost)
	{
		_cost = cost;
	}
	
	public double getCost()
	{
		return _cost;
	}
	
	public void free()
	{
		// reset node location
		_loc = null;
		
		// reset node parent, child and cost
		_parent = null;
		_child = null;
		_cost = -1000;
	}
}
