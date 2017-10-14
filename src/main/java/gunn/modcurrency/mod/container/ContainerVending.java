package gunn.modcurrency.mod.container;

import gunn.modcurrency.mod.client.gui.util.INBTInventory;
import gunn.modcurrency.mod.container.slot.SlotCustomizable;
import gunn.modcurrency.mod.container.slot.SlotVendor;
import gunn.modcurrency.mod.item.ItemWallet;
import gunn.modcurrency.mod.item.ModItems;
import gunn.modcurrency.mod.network.PacketCheckGhostStacksToClient;
import gunn.modcurrency.mod.network.PacketHandler;
import gunn.modcurrency.mod.network.PacketItemSpawnToServer;
import gunn.modcurrency.mod.tileentity.TileVending;
import gunn.modcurrency.mod.utils.UtilMethods;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/**
 * Distributed with the Currency-Mod for Minecraft
 * Copyright (C) 2017  Brady Gunn
 *
 * File Created on 2017-05-08
 */
public class ContainerVending extends Container implements INBTInventory {
    //Slot Index
    //0-35 = Players Inv
    //36 = Input Slot
    //37-... Vend Slots & Buffer Slots
    public final int HOTBAR_SLOT_COUNT = 9;
    public final int PLAYER_INV_ROW_COUNT = 3;
    public final int PLAYER_INV_COLUMN_COUNT = 9;
    public final int PLAYER_INV_TOTAL_COUNT = PLAYER_INV_COLUMN_COUNT * PLAYER_INV_ROW_COUNT;
    public final int PLAYER_TOTAL_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INV_TOTAL_COUNT;

    public int TE_VEND_COLUMN_COUNT = 3;
    public final int TE_VEND_ROW_COUNT = 5;
    public int TE_VEND_MAIN_TOTAL_COUNT = TE_VEND_COLUMN_COUNT * TE_VEND_ROW_COUNT;
    public final int TE_BUFFER_START = 31;
    public final int TE_BUFFER_COUNT = 4;

    public final int PLAYER_FIRST_SLOT_INDEX = 0;
    public final int TE_MONEY_FIRST_SLOT_INDEX = PLAYER_FIRST_SLOT_INDEX + PLAYER_TOTAL_COUNT;
    public final int TE_VEND_FIRST_SLOT_INDEX = TE_MONEY_FIRST_SLOT_INDEX + 1;
    public int TE_BUFFER_FIRST_SLOT_INDEX = TE_VEND_FIRST_SLOT_INDEX + TE_VEND_MAIN_TOTAL_COUNT;

    private Item[] specialSlotItems = new Item[2];
    private TileVending tile;
    private int[] cachedFields;

    public ContainerVending(InventoryPlayer invPlayer, TileVending te) {
        specialSlotItems[0] = ModItems.itemBanknote;
        specialSlotItems[1] = ModItems.itemWallet;

        tile = te;
        setupPlayerInv(invPlayer);
        setupTeInv();
    }

    private void setupPlayerInv(InventoryPlayer invPlayer) {
        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;
        final int HOTBAR_XPOS = 8;
        final int HOTBAR_YPOS = 211;

        for (int x = 0; x < HOTBAR_SLOT_COUNT; x++)
            addSlotToContainer(new Slot(invPlayer, x, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS));

        final int PLAYER_INV_XPOS = 8;
        final int PLAYER_INV_YPOS = 153;

        for (int y = 0; y < PLAYER_INV_ROW_COUNT; y++) {
            for (int x = 0; x < PLAYER_INV_COLUMN_COUNT; x++) {
                int slotNum = HOTBAR_SLOT_COUNT + y * PLAYER_INV_COLUMN_COUNT + x;
                int xpos = PLAYER_INV_XPOS + x * SLOT_X_SPACING;
                int ypos = PLAYER_INV_YPOS + y * SLOT_Y_SPACING;
                addSlotToContainer(new Slot(invPlayer, slotNum, xpos, ypos));
            }
        }
    }

    private void setupTeInv() {
        IItemHandler itemHandler = this.tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);

        //Input Slot
        addSlotToContainer(new SlotCustomizable(itemHandler, 0, 152, 9, specialSlotItems));

        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;
        final int TE_INV_XPOS = 44;
        int TE_INV_YPOS = 50;

        if (tile.getField(tile.FIELD_TWOBLOCK) == 1) {
            TE_INV_YPOS = 32;
            TE_VEND_COLUMN_COUNT = 6;
            TE_VEND_MAIN_TOTAL_COUNT = TE_VEND_COLUMN_COUNT * TE_VEND_ROW_COUNT;
            TE_BUFFER_FIRST_SLOT_INDEX = TE_VEND_FIRST_SLOT_INDEX + TE_VEND_MAIN_TOTAL_COUNT;
        }

        //Main Slots
        for (int y = 0; y < TE_VEND_COLUMN_COUNT; y++) {
            for (int x = 0; x < TE_VEND_ROW_COUNT; x++) {
                int slotNum = 1 + y * TE_VEND_ROW_COUNT + x;
                int xpos = TE_INV_XPOS + x * SLOT_X_SPACING;
                int ypos = TE_INV_YPOS + y * SLOT_Y_SPACING;
                addSlotToContainer(new SlotVendor(itemHandler, slotNum, xpos, ypos));
            }
        }

        //Buffer Slots
        int yshift = 0;
        if (tile.getField(tile.FIELD_TWOBLOCK) == 1) yshift = 8;

        for (int i = 0; i < TE_BUFFER_COUNT; i++) {
            addSlotToContainer(new SlotCustomizable(itemHandler, TE_BUFFER_START + i, 13, 42 + yshift + i * SLOT_Y_SPACING, ModItems.itemBanknote));
        }
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        checkGhostStacks();
        tile.voidPlayerUsing();
        tile.setField(tile.FIELD_GEAREXT, 0);
        tile.setField(tile.FIELD_CREATIVE, 0);
        tile.setField(tile.FIELD_MODE, 0);
        tile.outInputSlot();
        super.onContainerClosed(playerIn);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return tile.canInteractWith(playerIn);
    }

    @Nullable
    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
        //Allows drag clicking
        if (slotId == -999) return super.slotClick(slotId, dragType, clickTypeIn, player);

        //Ensures Pickup_All works without duplicating blocks
        if (clickTypeIn == ClickType.PICKUP_ALL && slotId >= 0 && slotId <= PLAYER_TOTAL_COUNT) {
            Slot slot = this.inventorySlots.get(slotId);
            ItemStack itemstack1 = player.inventory.getItemStack();

            if (!itemstack1.isEmpty() && (slot == null || !slot.getHasStack() || !slot.canTakeStack(player))) {
                int i = dragType == 0 ? 0 : this.inventorySlots.size() - 1;
                int j = dragType == 0 ? 1 : -1;

                for (int k = 0; k < 2; ++k) {
                    for (int l = i; l >= 0 && l <= PLAYER_TOTAL_COUNT && itemstack1.getCount() < itemstack1.getMaxStackSize(); l += j) {
                        Slot slot1 = this.inventorySlots.get(l);

                        if (slot1.getHasStack() && canAddItemToSlot(slot1, itemstack1, true) && slot1.canTakeStack(player) && this.canMergeSlot(itemstack1, slot1)) {
                            ItemStack itemstack2 = slot1.getStack();

                            if (k != 0 || itemstack2.getCount() != itemstack2.getMaxStackSize()) {
                                int i1 = Math.min(itemstack1.getMaxStackSize() - itemstack1.getCount(), itemstack2.getCount());
                                ItemStack itemstack3 = slot1.decrStackSize(i1);
                                itemstack1.grow(i1);

                                if (itemstack3.isEmpty()) {
                                    slot1.putStack(ItemStack.EMPTY);
                                }

                                slot1.onTake(player, itemstack3);
                            }
                        }
                    }
                }
            }
            this.detectAndSendChanges();
            return ItemStack.EMPTY;
        } else if (clickTypeIn == ClickType.PICKUP_ALL && slotId > PLAYER_TOTAL_COUNT) {
            return ItemStack.EMPTY;
        }

        if (tile.getField(tile.FIELD_MODE) == 1) {               //EDIT MODE
            if (slotId >= TE_VEND_FIRST_SLOT_INDEX && slotId < (TE_VEND_FIRST_SLOT_INDEX+ TE_VEND_MAIN_TOTAL_COUNT) && tile.getField(tile.FIELD_GEAREXT) == 1 && clickTypeIn == ClickType.PICKUP && dragType == 0) {
                if (getSlot(slotId).getHasStack()) {
                    tile.setSelectedName(getSlot(slotId).getStack().getDisplayName());
                } else tile.setSelectedName("No Item");
                tile.getWorld().notifyBlockUpdate(tile.getPos(), tile.getBlockType().getDefaultState(), tile.getBlockType().getDefaultState(), 3);
                if (!(tile.getField(tile.FIELD_SELECTSLOT) == slotId)) {
                    tile.setField(tile.FIELD_SELECTSLOT, slotId);
                    return ItemStack.EMPTY;
                }
            } else if (slotId >= TE_VEND_FIRST_SLOT_INDEX && slotId < (TE_VEND_FIRST_SLOT_INDEX + TE_VEND_MAIN_TOTAL_COUNT) && tile.getField(tile.FIELD_GEAREXT) == 0) {
                //If an item is a ghost and clicked on with an item
                if (tile.isGhostSlot(slotId - PLAYER_TOTAL_COUNT - 1)) {
                    this.tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).getStackInSlot(slotId - PLAYER_TOTAL_COUNT).shrink(1);
                    tile.setGhostSlot(slotId - PLAYER_TOTAL_COUNT - 1, false);
                }
            }
            return super.slotClick(slotId, dragType, clickTypeIn, player);

        } else {  //Sell Mode
            if (slotId >= 0 && slotId <= PLAYER_TOTAL_COUNT) {           //Is Players Inv or Input Slot
                return super.slotClick(slotId, dragType, clickTypeIn, player);
            } else if (slotId >= TE_VEND_FIRST_SLOT_INDEX && slotId < (TE_VEND_FIRST_SLOT_INDEX + TE_VEND_MAIN_TOTAL_COUNT)) {  //Is TE Inv
                if (!tile.isGhostSlot(slotId - PLAYER_TOTAL_COUNT - 1)) {
                    if (clickTypeIn == ClickType.PICKUP && dragType == 0) {   //Left Click = 1 item
                        return checkAfford(slotId, 1, player);
                    } else if (clickTypeIn == ClickType.PICKUP && dragType == 1) {   //Right Click = 10 item


                        return checkAfford(slotId, 10, player);
                    } else if (clickTypeIn == ClickType.QUICK_MOVE) {
                        return checkAfford(slotId, 64, player);
                    } else {
                        return ItemStack.EMPTY;
                    }
                } else return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack checkAfford(int slotId, int amnt, EntityPlayer player) {
        IItemHandler itemHandler = this.tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        ItemStack playStack = player.inventory.getItemStack();
        ItemStack slotStack = itemHandler.getStackInSlot(slotId - PLAYER_TOTAL_COUNT);
        ItemStack playBuyStack;

        boolean wallet = false;
        double bank = tile.getDouble(tile.DOUBLE_BANK);

        if (itemHandler.getStackInSlot(0) != ItemStack.EMPTY) {
            if (itemHandler.getStackInSlot(0).getItem() == ModItems.itemWallet) {
                wallet = true;
                bank = tile.getDouble(tile.DOUBLE_WALLETTOTAL);
            }
        }
        int cost = tile.getItemCost(slotId - PLAYER_TOTAL_COUNT - 1);

        if (slotStack != ItemStack.EMPTY) {
            if (playStack.getItem() != Item.getItemFromBlock(Blocks.AIR)) {
                if (!UtilMethods.equalStacks(playStack, slotStack)) {
                    return ItemStack.EMPTY; //Checks if player is holding stack, if its different then one being clicked do nothing
                }
            }
            if (tile.getField(tile.FIELD_INFINITE) == 0)
                if (slotStack.getCount() < amnt && slotStack.getCount() != 0) amnt = slotStack.getCount();

            //If cant afford amount requested lowers amount till can afford or till amount is 1
            lower:
            while ((cost * amnt) > bank) {
                if (amnt == 1) break lower;
                amnt--;
            }

            if ((bank >= (cost * amnt))) {   //If has enough money, buy it
                if (slotStack.getCount() >= amnt || tile.getField(tile.FIELD_INFINITE) == 1) {
                    playBuyStack = slotStack.copy();
                    playBuyStack.setCount(amnt);

                    if (!player.inventory.getItemStack().isEmpty()) {       //Holding Item
                        playBuyStack.setCount(amnt + playStack.getCount());
                    }
                    player.inventory.setItemStack(playBuyStack);

                    if (tile.getField(tile.FIELD_INFINITE) == 0) {
                        if (slotStack.getCount() - amnt == 0) {
                            tile.setGhostSlot(slotId - PLAYER_TOTAL_COUNT - 1, true);
                            slotStack.setCount(1);
                        } else slotStack.splitStack(amnt);
                    }

                    if (wallet) {
                        sellToWallet(itemHandler.getStackInSlot(0), cost * amnt);
                    } else {
                        tile.setDouble(tile.DOUBLE_BANK, bank - (cost * amnt));
                    }
                    tile.setDouble(tile.DOUBLE_PROFIT, tile.getField(tile.DOUBLE_PROFIT) + cost * amnt);
                }
            } else {
                System.out.println("CANT");
                tile.unsucessfulNoise();
            }
            return slotStack;
        }
        return ItemStack.EMPTY;
    }


    @Nullable
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        ItemStack sourceStack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack copyStack = slot.getStack();
            sourceStack = copyStack.copy();

            if (index < PLAYER_TOTAL_COUNT) {        //Player Inventory Slots
                if (inventorySlots.get(index).getStack().getItem() == ModItems.itemBanknote || inventorySlots.get(index).getStack().getItem() == ModItems.itemWallet) {
                    if (tile.getField(tile.FIELD_MODE) == 0) {
                        if (!this.mergeItemStack(copyStack, TE_MONEY_FIRST_SLOT_INDEX, TE_MONEY_FIRST_SLOT_INDEX + 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (tile.getField(tile.FIELD_MODE) == 1) {     //Only allow shift clicking from player inv in edit mode
                        if (!this.mergeItemStack(copyStack, TE_VEND_FIRST_SLOT_INDEX, TE_VEND_FIRST_SLOT_INDEX + TE_VEND_MAIN_TOTAL_COUNT, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (index == TE_MONEY_FIRST_SLOT_INDEX) {
                if (tile.getField(tile.FIELD_MODE) == 0) {
                    if (index == 36) {
                        if (!this.mergeItemStack(copyStack, PLAYER_FIRST_SLOT_INDEX, PLAYER_FIRST_SLOT_INDEX + PLAYER_TOTAL_COUNT, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            } else if (index >= TE_VEND_FIRST_SLOT_INDEX && index < TE_VEND_FIRST_SLOT_INDEX + TE_VEND_MAIN_TOTAL_COUNT + 4) {  //TE Inventory
                if (!this.mergeItemStack(copyStack, 0, PLAYER_FIRST_SLOT_INDEX + PLAYER_TOTAL_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (copyStack.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }
        return sourceStack;
    }

    @Override
    public void detectAndSendChanges() {
        checkGhostStacks();
        if(!tile.getWorld().isRemote && tile.getPlayerUsing() != null && PacketHandler.INSTANCE != null){
            PacketCheckGhostStacksToClient pack = new PacketCheckGhostStacksToClient();
            pack.setData(tile.getPos());
            PacketHandler.INSTANCE.sendTo(pack, (EntityPlayerMP) tile.getPlayerUsing());
        }

        super.detectAndSendChanges();
        boolean fieldChanged[] = new boolean[tile.getFieldCount()];

        if (cachedFields == null) cachedFields = new int[tile.getFieldCount()];

        for (int i = 0; i < cachedFields.length; i++) {
            if (cachedFields[i] != tile.getField(i)) {
                cachedFields[i] = tile.getField(i);
                fieldChanged[i] = true;
            }
        }

        for (IContainerListener listener : this.listeners) {
            for (int field = 0; field < tile.getFieldCount(); ++field) {
                if (fieldChanged[field]) {
                    listener.sendWindowProperty(this, field, cachedFields[field]);
                }
            }
        }
    }

    @Override
    public void updateProgressBar(int id, int data) {
        tile.setField(id, data);
    }

    private void sellToWallet(ItemStack wallet, int amountRemovable) {
        int amount = amountRemovable;

        int one = getTotalOfBill(wallet, 0);
        int five = getTotalOfBill(wallet, 1);
        int ten = getTotalOfBill(wallet, 2);
        int twenty = getTotalOfBill(wallet, 3);
        int fifty = getTotalOfBill(wallet, 4);
        int hundo = getTotalOfBill(wallet, 5);

        int[] out = new int[6];

        out[5] = Math.round(amount / 100);
        while (out[5] > hundo) out[5]--;
        amount = amount - (out[5] * 100);

        out[4] = Math.round(amount / 50);
        while (out[4] > fifty) out[4]--;
        amount = amount - (out[4] * 50);

        out[3] = Math.round(amount / 20);
        while (out[3] > twenty) out[3]--;
        amount = amount - (out[3] * 20);

        out[2] = Math.round(amount / 10);
        while (out[2] > ten) out[2]--;
        amount = amount - (out[2] * 10);

        out[1] = Math.round(amount / 5);
        while (out[1] > five) out[1]--;
        amount = amount - (out[1] * 5);

        out[0] = Math.round(amount);
        while (out[0] > one) out[0]--;
        amount = amount - (out[0]);


        ItemStackHandler itemHandler = readInventoryTag(wallet, ItemWallet.WALLET_TOTAL_COUNT);

        for (int i = 0; i < 6; i++) {
            searchLoop:
            for (int j = 0; j < itemHandler.getSlots(); j++) {
                ItemStack stack = itemHandler.getStackInSlot(j);

                if (stack != ItemStack.EMPTY) {
                    if (stack.getItemDamage() == i) {
                        if (stack.getCount() >= out[i]) {  //If Stack size can handle all amount of bill
                            itemHandler.getStackInSlot(j).shrink(out[i]);
                            out[0] = 0;

                            break searchLoop;
                        } else {  //If Stack size is smaller then amount of bills
                            out[i] = out[i] - stack.getCount();
                            itemHandler.setStackInSlot(j, ItemStack.EMPTY);
                        }
                    }

                    if (stack.getCount() == 0) itemHandler.setStackInSlot(j, ItemStack.EMPTY);  //Removes stack is 0
                }
            }
        }
        /*If there is still an amount that needs to be paid for,
        will pick the closed highest bill in the wallet, remove it
        and send the 'change' to the bank variable of the vendor then output it with outChange
         */
        if (amount != 0) {
            int billDamage = 0;
            searchLoop:
            //Searches wallet for the first highest bill that can handle rest of amount
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                if (itemHandler.getStackInSlot(i) != ItemStack.EMPTY) {
                    int billWorth = getBillWorth(itemHandler.getStackInSlot(i).getItemDamage(), itemHandler.getStackInSlot(i).getCount());
                    if (billWorth > amount) {
                        billDamage = itemHandler.getStackInSlot(i).getItemDamage();
                        if (itemHandler.getStackInSlot(i).getCount() == 1) {
                            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                        } else {
                            itemHandler.getStackInSlot(i).shrink(-1);
                        }
                        break searchLoop;
                    }
                }
            }

            //Calculates change by deducting the bills worth with the amount deductible that was left
            int change = 0;
            switch (billDamage) {
                case 0:
                    change = 0;
                    break;
                case 1:
                    change = 5 - amount;
                    break;
                case 2:
                    change = 10 - amount;
                    break;
                case 3:
                    change = 20 - amount;
                    break;
                case 4:
                    change = 50 - amount;
                    break;
                case 5:
                    change = 100 - amount;
                    break;
            }

            //Adds change to the bank variable in vendor
            tile.setDouble(tile.DOUBLE_BANK, tile.getField(tile.DOUBLE_BANK) + change);
        }

        writeInventoryTag(wallet, itemHandler);
    }

    private int getTotalOfBill(ItemStack stack, int billDamage) {
        ItemStackHandler itemStackHandler = readInventoryTag(stack, ItemWallet.WALLET_TOTAL_COUNT);

        int totalOfBill = 0;
        for (int i = 0; i < itemStackHandler.getSlots(); i++) {
            if (itemStackHandler.getStackInSlot(i) != ItemStack.EMPTY) {
                if (itemStackHandler.getStackInSlot(i).getItemDamage() == billDamage) {
                    totalOfBill = totalOfBill + itemStackHandler.getStackInSlot(i).getCount();
                }
            }
        }
        return totalOfBill;
    }

    private int getBillWorth(int itemDamage, int stackSize) {
        int cash = 0;
        switch (itemDamage) {
            case 0:
                cash = 1;
                break;
            case 1:
                cash = 5;
                break;
            case 2:
                cash = 10;
                break;
            case 3:
                cash = 20;
                break;
            case 4:
                cash = 50;
                break;
            case 5:
                cash = 100;
                break;
        }

        return cash * stackSize;
    }

    private void checkGhostStacks() {
        IItemHandler itemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        for (int i = 0; i < TE_VEND_MAIN_TOTAL_COUNT; i++) {
            if (tile.isGhostSlot(i) && itemHandler.getStackInSlot(i + 1).getCount() > 1) {
                System.out.println("GHOOSST");
                itemHandler.getStackInSlot(i + 1).shrink(1);
                tile.setGhostSlot(i, false);
            }
        }
    }
}
