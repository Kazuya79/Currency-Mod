package beardlessbrady.modcurrency2.proxy;

import beardlessbrady.modcurrency2.block.economyblocks.ModBlockColors;
import beardlessbrady.modcurrency2.item.ModItems;
import beardlessbrady.modcurrency2.item.playercurrency.ItemColorCurrency;
import beardlessbrady.modcurrency2.network.PacketHandler;
import beardlessbrady.modcurrency2.handler.BakedHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.input.Keyboard;

/**
 * This class was created by BeardlessBrady. It is distributed as
 * part of The Currency-Mod. Source Code located on github:
 * https://github.com/BeardlessBrady/Currency-Mod
 * -
 * Copyright (C) All Rights Reserved
 * File Created 2019-02-07
 */

public class ClientProxy extends CommonProxy{
    public static KeyBinding[] keyBindings;

    public void preInit(FMLPreInitializationEvent e){
        super.preInit(e);
        PacketHandler.registerClientMessages("modcurrency");
        MinecraftForge.EVENT_BUS.register(new BakedHandler());
    }

    public void Init(FMLInitializationEvent e) {
        super.Init(e);
        registerKeyBindings();
        ModBlockColors.registerBlockColors();
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler(new ItemColorCurrency(), ModItems.itemPlayerCurrency);
    }

    public void postInit(FMLPostInitializationEvent e){
        super.postInit(e);
    }

    public void registerKeyBindings(){
        keyBindings = new KeyBinding[2];

        keyBindings[0] = new KeyBinding("key.shift.desc", Keyboard.KEY_LSHIFT, "key.goodolcurrency.category");
        keyBindings[1] = new KeyBinding("key.control.desc", Keyboard.KEY_LCONTROL, "key.goodolcurrency.category");

        for (int i = 0; i < keyBindings.length; i++)
            ClientRegistry.registerKeyBinding(keyBindings[i]);
    }


}
