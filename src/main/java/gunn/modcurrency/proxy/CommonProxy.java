package gunn.modcurrency.proxy;

import gunn.modcurrency.ModCurrency;
import gunn.modcurrency.blocks.ModBlocks;
import gunn.modcurrency.handler.GuiHandler;
import gunn.modcurrency.handler.PacketHandler;
import gunn.modcurrency.items.ModItems;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

/**
 * This class was created by <Brady Gunn>.
 * Distributed with the Currency-Mod for Minecraft.
 *
 * The Currency-Mod is open source and distributed
 * under the General Public License
 *
 * File Created on 2016-10-28.
 */
public class CommonProxy {
    public void preInit(FMLPreInitializationEvent e){
        ModItems.preInit();
        ModBlocks.preInit();

        NetworkRegistry.INSTANCE.registerGuiHandler(ModCurrency.instance, new GuiHandler());
        PacketHandler.registerMessages("modcurrency");

        OBJLoader.INSTANCE.addDomain(ModCurrency.MODID);
    }

    public void Init(FMLInitializationEvent e){
        ModBlocks.addRecipes();
    }

    public void postInit(FMLPostInitializationEvent e){
    }
}

