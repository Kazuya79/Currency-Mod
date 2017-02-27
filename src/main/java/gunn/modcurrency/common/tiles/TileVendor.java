
package gunn.modcurrency.common.tiles;

import gunn.modcurrency.ModCurrency;
import gunn.modcurrency.api.TileBuy;
import gunn.modcurrency.common.containers.itemhandlers.ItemHandlerVendor;
import gunn.modcurrency.common.core.util.INBTInventory;
import gunn.modcurrency.common.items.ItemWallet;
import gunn.modcurrency.common.items.ModItems;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import javax.annotation.Nullable;

/**
 * Distributed with the Currency-Mod for Minecraft.
 * Copyright (C) 2016  Brady Gunn
 *
 * File Created on 2016-10-30.
 */
public class TileVendor extends TileBuy implements ICapabilityProvider, ITickable, INBTInventory{
    private static final int INPUT_SLOT_COUNT = 1;
    private static final int VEND_SLOT_COUNT = 30;
    private static final int BUFFER_SLOT_COUNT = 6;

    private int bank, profit, selectedSlot, face, walletTotal;
    private String owner, selectedName;
    private boolean locked, mode, creative, infinite, gearExtended, walletIn, fuzzy;
    private int[] itemCosts = new int[VEND_SLOT_COUNT];
    private ItemStackHandler inputStackHandler = new ItemStackHandler(INPUT_SLOT_COUNT);
    private ItemHandlerVendor vendStackHandler = new ItemHandlerVendor(VEND_SLOT_COUNT);
    private ItemStackHandler bufferStackHandler = new ItemHandlerVendor(BUFFER_SLOT_COUNT);
    private EntityPlayer playerUsing = null;
    public static Item[] specialSlotItems = new Item[2];



    public TileVendor() {
        bank = 0;
        profit = 0;
        selectedSlot = 37;
        face = 0;
        walletTotal = 0;
        owner = "";
        selectedName = "No Item";
        locked = false;
        mode = false;
        creative = false;
        infinite = false;
        gearExtended = false;
        walletIn = false;
        fuzzy = true;

        for (int i = 0; i < itemCosts.length; i++) itemCosts[i] = 0;

        //Setting items allowed in special slot
        specialSlotItems[0] = ModItems.itemBanknote;
        specialSlotItems[1] = ModItems.itemWallet;
    }

    public void openGui(EntityPlayer player, World world, BlockPos pos) {
        player.openGui(ModCurrency.instance, 30, world, pos.getX(), pos.getY(), pos.getZ());
        playerUsing = player;
    }

    @Override
    public void update() {
        if (!world.isRemote) {
            if (inputStackHandler.getStackInSlot(0) != ItemStack.EMPTY) {
                if (inputStackHandler.getStackInSlot(0).getItem() == ModItems.itemBanknote) {
                    //<editor-fold desc="Dealing with a Banknote">
                    int amount;
                    switch (inputStackHandler.getStackInSlot(0).getItemDamage()) {
                        case 0:
                            amount = 1;
                            break;
                        case 1:
                            amount = 5;
                            break;
                        case 2:
                            amount = 10;
                            break;
                        case 3:
                            amount = 20;
                            break;
                        case 4:
                            amount = 50;
                            break;
                        case 5:
                            amount = 100;
                            break;
                        default:
                            amount = -1;
                            break;
                    }
                    amount = amount * inputStackHandler.getStackInSlot(0).getCount();
                    inputStackHandler.setStackInSlot(0, ItemStack.EMPTY);
                    bank = bank + amount;
                    markDirty();
                    //</editor-fold>
                }else  if (inputStackHandler.getStackInSlot(0).getItem() == ModItems.itemWallet) {      //Wallet
                    walletIn = true;
                    walletTotal = getTotalCash();
                }
            }else if (walletIn == true) walletIn = false;

            if(profit >= 20){
                //<editor-fold desc="Dealing with Buffer Slots">
                Loop:
                for(int i = 0; i < BUFFER_SLOT_COUNT; i++){
                    if(bufferStackHandler.getStackInSlot(i) != ItemStack.EMPTY){
                        if(bufferStackHandler.getStackInSlot(i).getItemDamage() == 3 && bufferStackHandler.getStackInSlot(i).getCount() < bufferStackHandler.getStackInSlot(i).getMaxStackSize()){
                            profit = profit - 20;
                            bufferStackHandler.getStackInSlot(i).grow(1);
                            break Loop;
                        }
                    }else if(bufferStackHandler.getStackInSlot(i) == ItemStack.EMPTY){
                        ItemStack newStack = new ItemStack(ModItems.itemBanknote);
                        newStack.setItemDamage(3);
                        bufferStackHandler.setStackInSlot(i, newStack);
                        profit = profit - 20;
                        break Loop;
                    }
                }
                //</editor-fold>
            }
        }
    }

    //Drop Items
    public void dropItems() {
        for (int i = 0; i < vendStackHandler.getSlots(); i++) {
            ItemStack item = vendStackHandler.getStackInSlot(i);
            if (item != ItemStack.EMPTY) {
                world.spawnEntity(new EntityItem(world, getPos().getX(), getPos().getY(), getPos().getZ(), item));
                vendStackHandler.setStackInSlot(i, ItemStack.EMPTY);   //Just in case
            }
        }
        for (int i = 0; i < bufferStackHandler.getSlots(); i++){
            ItemStack item = bufferStackHandler.getStackInSlot(i);
            if (item != ItemStack.EMPTY) {
                world.spawnEntity(new EntityItem(world, getPos().getX(), getPos().getY(), getPos().getZ(), item));
                vendStackHandler.setStackInSlot(i, ItemStack.EMPTY);   //Just in case
            }
        }
    }

    //Player must be in certain range to open GUI
    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return !isInvalid() && player.getDistanceSq(pos.add(0.5D, 0.5D, 0.5D)) <= 64D;
    }

    public void unsucessfulNoise(){
        world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0F, 10.0F, false);
    }

    //<editor-fold desc="Money Methods-------------------------------------------------------------------------------------------------------">
    /**
     * If the wallet is present returns the total amount of cash in it
     * @return TotalCash
     */
    public int getTotalCash(){
        ItemStack item = inputStackHandler.getStackInSlot(0);
        if(item.hasTagCompound()) {
            ItemStackHandler itemStackHandler = readInventoryTag(inputStackHandler.getStackInSlot(0), ItemWallet.WALLET_TOTAL_COUNT);

            int totalCash = 0;
            for (int i = 0; i < itemStackHandler.getSlots(); i++) {
                if (itemStackHandler.getStackInSlot(i) != ItemStack.EMPTY) {
                    switch (itemStackHandler.getStackInSlot(i).getItemDamage()) {
                        case 0:
                            totalCash = totalCash + 1 * itemStackHandler.getStackInSlot(i).getCount();
                            break;
                        case 1:
                            totalCash = totalCash + 5 * itemStackHandler.getStackInSlot(i).getCount();
                            break;
                        case 2:
                            totalCash = totalCash + 10 * itemStackHandler.getStackInSlot(i).getCount();
                            break;
                        case 3:
                            totalCash = totalCash + 20 * itemStackHandler.getStackInSlot(i).getCount();
                            break;
                        case 4:
                            totalCash = totalCash + 50 * itemStackHandler.getStackInSlot(i).getCount();
                            break;
                        case 5:
                            totalCash = totalCash + 100 * itemStackHandler.getStackInSlot(i).getCount();
                            break;
                        default:
                            totalCash = -1;
                            break;
                    }
                }
            }
            return totalCash;
        }
        return 0;
    }

    public boolean canAfford(int slot){
        if(walletIn){
            return getItemCost(slot) >= getTotalCash();
        }
        return bank >= getItemCost(slot);
    }

    //Outputs change in least amount of bills
    public void outChange() {
        int amount = bank;
        if (mode) amount = profit;

        int[] out = new int[6];

        out[5] = Math.round(amount / 100);
        amount = amount - (out[5] * 100);

        out[4] = Math.round(amount / 50);
        amount = amount - (out[4] * 50);

        out[3] = Math.round(amount / 20);
        amount = amount - (out[3] * 20);

        out[2] = Math.round(amount / 10);
        amount = amount - (out[2] * 10);

        out[1] = Math.round(amount / 5);
        amount = amount - (out[1] * 5);

        out[0] = Math.round(amount);

        if (!world.isRemote) {
            for (int i = 0; i < out.length; i++) {
                if (out[i] != 0) {
                    ItemStack item = new ItemStack(ModItems.itemBanknote);
                    item.setItemDamage(i);
                    item.setCount(out[i]);

                    if (mode) {
                        profit = 0;
                    } else {
                        bank = 0;
                    }

                    boolean playerInGui= false;
                    if (playerUsing != null) playerInGui = true;


                    if (playerInGui) {
                        InventoryPlayer inventoryPlayer = playerUsing.inventory;
                        if (inventoryPlayer.getFirstEmptyStack() != -1) {     //If Players Inventory has room
                            inventoryPlayer.setInventorySlotContents(inventoryPlayer.getFirstEmptyStack(), item);
                        } else {
                            playerInGui = false;
                        }
                    }

                    if (!playerInGui) {       //If no room, spawn
                        int x = getPos().getX();
                        int z = getPos().getZ();

                        switch (face) {
                            case 0:
                                z = z - 2; //North
                                break;
                            case 1:
                                x = x + 2; //East
                                break;
                            case 2:
                                z = z + 2; //South
                                break;
                            case 3:
                                x = x - 2;//West
                                break;
                        }
                        world.spawnEntity(new EntityItem(world, x, getPos().up().getY(), z, item));
                    }
                }
            }
        }
    }

    public void outInputSlot(){
        if (inputStackHandler.getStackInSlot(0).getItem() != Item.getItemFromBlock(Blocks.AIR)) {
            if (!world.isRemote) {
                ItemStack item = inputStackHandler.getStackInSlot(0);
                inputStackHandler.setStackInSlot(0, ItemStack.EMPTY);

                int x = getPos().getX();
                int z = getPos().getZ();
                switch (face) {
                    case 0:
                        z = z - 2; //North
                        break;
                    case 1:
                        x = x + 2; //East
                        break;
                    case 2:
                        z = z + 2; //South
                        break;
                    case 3:
                        x = x - 2;//West
                        break;
                }

                world.spawnEntity(new EntityItem(world, x, getPos().up().getY(), z, item));
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="NBT & Packet Stoof--------------------------------------------------------------------------------------------------">
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("items", vendStackHandler.serializeNBT());
        compound.setTag("buffer", bufferStackHandler.serializeNBT());
        compound.setTag("input", inputStackHandler.serializeNBT());
        compound.setInteger("bank", bank);
        compound.setInteger("profit", profit);
        compound.setInteger("face", face);
        compound.setInteger("walletTotal", walletTotal);
        compound.setBoolean("locked", locked);
        compound.setBoolean("mode", mode);
        compound.setBoolean("creative", creative);
        compound.setBoolean("infinite", infinite);
        compound.setBoolean("gearExtended", gearExtended);
        compound.setBoolean("walletIn", walletIn);
        compound.setBoolean("fuzzy", fuzzy);
        compound.setInteger("selectedSlot", selectedSlot);
        compound.setString("selectedName", selectedName);
        compound.setString("owner", owner);

        NBTTagCompound itemCostsNBT = new NBTTagCompound();
        for (int i = 0; i < itemCosts.length; i++) itemCostsNBT.setInteger("cost" + i, itemCosts[i]);
        compound.setTag("itemCosts", itemCostsNBT);

        NBTTagCompound ghostItemNBT = new NBTTagCompound();
        for (int i = 0; i < vendStackHandler.getSlots(); i++) ghostItemNBT.setBoolean("ghost" + i, vendStackHandler.isGhost(i));
        compound.setTag("ghostItems", ghostItemNBT);

        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("items")) vendStackHandler.deserializeNBT((NBTTagCompound) compound.getTag("items"));
        if (compound.hasKey("buffer")) bufferStackHandler.deserializeNBT((NBTTagCompound) compound.getTag("buffer"));
        if (compound.hasKey("input")) inputStackHandler.deserializeNBT((NBTTagCompound) compound.getTag("input"));
        if (compound.hasKey("bank")) bank = compound.getInteger("bank");
        if (compound.hasKey("profit")) profit = compound.getInteger("profit");
        if (compound.hasKey("face")) face = compound.getInteger("face");
        if (compound.hasKey("walletTotal")) walletTotal = compound.getInteger("walletTotal");
        if (compound.hasKey("locked")) locked = compound.getBoolean("locked");
        if (compound.hasKey("mode")) mode = compound.getBoolean("mode");
        if (compound.hasKey("creative")) creative = compound.getBoolean("creative");
        if (compound.hasKey("infinite")) infinite = compound.getBoolean("infinite");
        if (compound.hasKey("gearExtended")) gearExtended = compound.getBoolean("gearExtended");
        if (compound.hasKey("walletIn")) walletIn = compound.getBoolean("walletIn");
        if (compound.hasKey("fuzzy")) fuzzy = compound.getBoolean("fuzzy");
        if (compound.hasKey("selectedSlot")) selectedSlot = compound.getInteger("selectedSlot");
        if (compound.hasKey("selectedName")) selectedName = compound.getString("selectedName");
        if (compound.hasKey("owner")) owner = compound.getString("owner");

        if (compound.hasKey("itemCosts")) {
            NBTTagCompound itemCostsNBT = compound.getCompoundTag("itemCosts");
            for (int i = 0; i < itemCosts.length; i++) itemCosts[i] = itemCostsNBT.getInteger("cost" + i);
        }

        if(compound.hasKey("ghostItems")) {
            NBTTagCompound ghostItemsNBT = compound.getCompoundTag("ghostItems");
            for (int i = 0; i < vendStackHandler.getSlots(); i++) vendStackHandler.setGhost(i, ghostItemsNBT.getBoolean("ghost" + i));
        }
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("bank", bank);
        tag.setInteger("profit", profit);
        tag.setInteger("face", face);
        tag.setInteger("walletTotal", walletTotal);
        tag.setBoolean("locked", locked);
        tag.setBoolean("mode", mode);
        tag.setBoolean("creative", creative);
        tag.setBoolean("infinite", infinite);
        tag.setBoolean("gearExtended", gearExtended);
        tag.setBoolean("walletIn", walletIn);
        tag.setBoolean("fuzzy", fuzzy);
        tag.setInteger("selectedSlot", selectedSlot);
        tag.setString("selectedName", selectedName);
        tag.setString("owner", owner);

        NBTTagCompound itemCostsNBT = new NBTTagCompound();
        for (int i = 0; i < itemCosts.length; i++) itemCostsNBT.setInteger("cost" + i, itemCosts[i]);
        tag.setTag("itemCosts", itemCostsNBT);

        NBTTagCompound ghostItemsNBT = new NBTTagCompound();
        for (int i = 0; i < vendStackHandler.getSlots(); i++) ghostItemsNBT.setBoolean("ghost" + i, vendStackHandler.isGhost(i));
        tag.setTag("ghostItems", ghostItemsNBT);

        return new SPacketUpdateTileEntity(pos, 1, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        bank = pkt.getNbtCompound().getInteger("bank");
        profit = pkt.getNbtCompound().getInteger("profit");
        face = pkt.getNbtCompound().getInteger("face");
        walletTotal = pkt.getNbtCompound().getInteger("walletTotal");
        locked = pkt.getNbtCompound().getBoolean("locked");
        mode = pkt.getNbtCompound().getBoolean("mode");
        creative = pkt.getNbtCompound().getBoolean("creative");
        infinite = pkt.getNbtCompound().getBoolean("infinite");
        gearExtended = pkt.getNbtCompound().getBoolean("gearExtended");
        walletIn = pkt.getNbtCompound().getBoolean("walletIn");
        fuzzy = pkt.getNbtCompound().getBoolean("fuzzy");
        selectedSlot = pkt.getNbtCompound().getInteger("selectedSlot");
        selectedName = pkt.getNbtCompound().getString("selectedName");
        owner = pkt.getNbtCompound().getString("owner");

        NBTTagCompound itemCostsNBT = pkt.getNbtCompound().getCompoundTag("itemCosts");
        for (int i = 0; i < itemCosts.length; i++) itemCosts[i] = itemCostsNBT.getInteger("cost" + i);

        NBTTagCompound ghostItemsNBT = pkt.getNbtCompound().getCompoundTag("ghostItems");
        for (int i = 0; i < vendStackHandler.getSlots(); i ++) vendStackHandler.setGhost(i, ghostItemsNBT.getBoolean("ghost" + 1));

    }
    //</editor-fold>--------------------------------

    //<editor-fold desc="ItemStackHandler Methods--------------------------------------------------------------------------------------------">
    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if(facing == null) return true;
            if(!locked)  return false;
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == null) return (T) new CombinedInvWrapper(inputStackHandler, vendStackHandler, bufferStackHandler); //Inside Itself
            if (facing == EnumFacing.DOWN) return (T) bufferStackHandler;
            if (facing != EnumFacing.DOWN) return (T) vendStackHandler;
        }
        return super.getCapability(capability, facing);
    }
    //</editor-fold>

    //<editor-fold desc="Getter & Setter Methods---------------------------------------------------------------------------------------------">
    @Override
    public int getFieldCount() {
        return 12;
    }

    @Override
    public void setField(int id, int value) {
        switch (id) {
            case 0:
                bank = value;
                break;
            case 1:
                locked = (value == 1);
                break;
            case 2:
                mode = (value == 1);
                break;
            case 3:
                selectedSlot = value;
                break;
            case 4:
                profit = value;
                break;
            case 5:
                creative = (value == 1);
                break;
            case 6:
                infinite = (value == 1);
                break;
            case 7:
                face = value;
                break;
            case 8:
                gearExtended = (value == 1);
                break;
            case 9:
                walletIn = (value == 1);
                break;
            case 10:
                break;
            case 11:
                fuzzy = (value == 1);
                break;
        }
    }

    @Override
    public int getField(int id) {
        switch (id) {
            case 0:
                return bank;
            case 1:
                return (locked) ? 1 : 0;
            case 2:
                world.markBlockRangeForRenderUpdate(pos, pos);
                world.scheduleBlockUpdate(pos,this.getBlockType(),0,0);
                world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
                markDirty();
                return (mode) ? 1 : 0;
            case 3:
                return selectedSlot;
            case 4:
                return profit;
            case 5:
                return (creative) ? 1 : 0;
            case 6:
                return (infinite) ? 1 : 0;
            case 7:
                return face;
            case 8:
                return (gearExtended) ? 1 : 0;
            case 9:
                return (walletIn) ? 1 : 0;
            case 10:
                return walletTotal;
            case 11:
                return (fuzzy) ? 1 : 0;
        }
        return -1;
    }

    @Override
    public String getSelectedName() {
        return selectedName;
    }

    @Override
    public void setSelectedName(String name) {
        selectedName = name;
    }

    @Override
    public int[] getAllItemCosts() {
        return itemCosts.clone();
    }

    @Override
    public void setAllItemCosts(int[] copy) {
        itemCosts = copy.clone();
    }

    @Override
    public int getItemCost(int index) {
        return itemCosts[index];
    }

    @Override
    public void setItemCost(int amount) {
        itemCosts[selectedSlot - 37] = amount;
    }

    @Override
    public ItemStack getStack(int index) {
        return vendStackHandler.getStackInSlot(index);
    }

    @Override
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public ItemStackHandler getInputHandler() {
        return inputStackHandler;
    }

    @Override
    public ItemStackHandler getBufferHandler() {
        return bufferStackHandler;
    }

    @Override
    public ItemStackHandler getVendHandler() {
        return vendStackHandler;
    }

    @Override
    public void setStackHandlers(ItemStackHandler inputCopy, ItemStackHandler buffCopy, ItemStackHandler vendCopy) {
        inputStackHandler = inputCopy;
        vendStackHandler = ((ItemHandlerVendor) vendCopy);
        bufferStackHandler = buffCopy;
    }

    public EntityPlayer getPlayerUsing(){
        return playerUsing;
    }

    public void voidPlayerUsing(){
        playerUsing = null;
    }

    public boolean isItemGhost(int slot){
        return vendStackHandler.isGhost(slot);
    }

    public void setGhost(int slot, boolean bool){
            vendStackHandler.setGhost(slot, bool);
    }

    //</editor-fold>


}