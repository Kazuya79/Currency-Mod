package gunn.modcurrency.client.guis;

import gunn.modcurrency.client.containers.ContainerVendor;
import gunn.modcurrency.tiles.TileVendor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;

import java.awt.*;

/**
 * This class was created by <Brady Gunn>.
 * Distributed with the Currency Mod for Minecraft.
 *
 * The Currency Mod is open source and distributed
 * under the General Public License
 *
 * File Created on 2016-11-02.
 */
public class GuiVendor extends GuiContainer{

    private static final ResourceLocation texture = new ResourceLocation("modcurrency", "textures/gui/vendor_gui.png");
    private TileVendor tilevendor;

    public GuiVendor(InventoryPlayer invPlayer, TileVendor tilevendor){
        super(new ContainerVendor(invPlayer, tilevendor));
        this.tilevendor = tilevendor;
        xSize = 176;
        ySize = 235;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);

        fontRendererObj.drawString(I18n.format("container.vendor.name"),5,5, Color.darkGray.getRGB());
        fontRendererObj.drawString(I18n.format("container.vendor_dollarAmount.name") + ": $" + tilevendor.getField(0),5,14, Color.darkGray.getRGB());
        fontRendererObj.drawString(I18n.format("container.vendor_playerInv.name"),4,142, Color.darkGray.getRGB());
    }
}