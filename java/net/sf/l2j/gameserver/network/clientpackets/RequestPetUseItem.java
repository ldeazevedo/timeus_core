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
package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.handler.IItemHandler;
import net.sf.l2j.gameserver.handler.ItemHandler;
import net.sf.l2j.gameserver.model.actor.instance.L2PcInstance;
import net.sf.l2j.gameserver.model.actor.instance.L2PetInstance;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.PetItemList;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;
import net.sf.l2j.gameserver.util.Util;

public final class RequestPetUseItem extends L2GameClientPacket
{
	private static final int[] PET_FOOD_IDS =
	{
		2515,
		4038,
		5168,
		5169,
		6316,
		7582
	};
	
	private int _objectId;
	
	@Override
	protected void readImpl()
	{
		_objectId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null || !activeChar.hasPet())
			return;
		
		final L2PetInstance pet = (L2PetInstance) activeChar.getPet();
		
		final ItemInstance item = pet.getInventory().getItemByObjectId(_objectId);
		if (item == null)
			return;
		
		if (activeChar.isAlikeDead() || pet.isDead())
		{
			activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_CANNOT_BE_USED).addItemName(item));
			return;
		}
		
		if (!item.isEquipped() && !item.getItem().checkCondition(pet, pet, true))
			return;
		
		// Check if item is pet armor or pet weapon
		if (item.isPetItem())
		{
			// Verify if the pet can wear that item
			if (!pet.canWear(item.getItem()))
			{
				activeChar.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
				return;
			}
			
			if (item.isEquipped())
			{
				pet.getInventory().unEquipItemInSlot(item.getLocationSlot());
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_TOOK_OFF_S1).addItemName(item));
			}
			else
			{
				pet.getInventory().equipPetItem(item);
				activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.PET_PUT_ON_S1).addItemName(item));
			}
			
			activeChar.sendPacket(new PetItemList(pet));
			pet.updateAndBroadcastStatus(1);
			return;
		}
		
		if (Util.contains(PET_FOOD_IDS, item.getItemId()) && !pet.getTemplate().canEatFood(item.getItemId()))
		{
			activeChar.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
			return;
		}
		
		// If pet food check is successful or if the item got an handler, use that item.
		final IItemHandler handler = ItemHandler.getInstance().getItemHandler(item.getEtcItem());
		if (handler != null)
		{
			handler.useItem(pet, item, false);
			pet.updateAndBroadcastStatus(1);
		}
		else
			activeChar.sendPacket(SystemMessageId.PET_CANNOT_USE_ITEM);
		
		return;
	}
}