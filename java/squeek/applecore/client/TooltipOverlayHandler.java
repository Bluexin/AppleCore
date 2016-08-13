package squeek.applecore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import squeek.applecore.ModConfig;
import squeek.applecore.ModInfo;
import squeek.applecore.api.AppleCoreAPI;
import squeek.applecore.api.food.FoodValues;
import squeek.applecore.helpers.KeyHelper;

@SideOnly(Side.CLIENT)
public class TooltipOverlayHandler
{
	private static ResourceLocation modIcons = new ResourceLocation(ModInfo.MODID_LOWER, "textures/icons.png");
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_BOTTOM = 3;
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_TOP = -4;
	public static final int TOOLTIP_REAL_WIDTH_OFFSET_RIGHT = 3;

	public static void init()
	{
		MinecraftForge.EVENT_BUS.register(new TooltipOverlayHandler());
	}

	@SubscribeEvent
	public void onRenderTooltip(RenderTooltipEvent.PostText event)
	{
		ItemStack hoveredStack = event.getStack() != null ? event.getStack() : TooltipJEIHelper.getHoveredStack();
		if (hoveredStack == null)
			return;

		boolean shouldShowTooltip = (ModConfig.SHOW_FOOD_VALUES_IN_TOOLTIP && KeyHelper.isShiftKeyDown()) || ModConfig.ALWAYS_SHOW_FOOD_VALUES_TOOLTIP;
		if (!shouldShowTooltip)
			return;

		Minecraft mc = Minecraft.getMinecraft();
		GuiScreen gui = mc.currentScreen;

		if (gui == null)
			return;

		if (!AppleCoreAPI.accessor.isFood(hoveredStack))
			return;

		EntityPlayer player = mc.thePlayer;
		ScaledResolution scale = new ScaledResolution(mc);
		int toolTipY = event.getY();
		int toolTipX = event.getX();
		int toolTipW = event.getWidth();
		int toolTipH = event.getHeight();

		FoodValues defaultFoodValues = FoodValues.get(hoveredStack);
		FoodValues modifiedFoodValues = FoodValues.get(hoveredStack, player);

		if (defaultFoodValues.equals(modifiedFoodValues) && defaultFoodValues.hunger == 0 && defaultFoodValues.saturationModifier == 0)
			return;

		int biggestHunger = Math.max(defaultFoodValues.hunger, modifiedFoodValues.hunger);
		float biggestSaturationIncrement = Math.max(defaultFoodValues.getSaturationIncrement(), modifiedFoodValues.getSaturationIncrement());

		int barsNeeded = (int) Math.ceil(Math.abs(biggestHunger) / 2f);
		int saturationBarsNeeded = (int) Math.max(1, Math.ceil(Math.abs(biggestSaturationIncrement) / 2f));
		boolean saturationOverflow = saturationBarsNeeded > 10;
		String saturationText = saturationOverflow ? ((defaultFoodValues.saturationModifier < 0 ? -1 : 1) * saturationBarsNeeded) + "x " : null;
		if (saturationOverflow)
			saturationBarsNeeded = 1;

		int toolTipBottomY = toolTipY + toolTipH + 1 + TOOLTIP_REAL_HEIGHT_OFFSET_BOTTOM;
		int toolTipRightX = toolTipX + toolTipW + 1 + TOOLTIP_REAL_WIDTH_OFFSET_RIGHT;

		boolean shouldDrawBelow = toolTipBottomY + 20 < scale.getScaledHeight() - 3;

		int rightX = toolTipRightX - 3;
		int leftX = rightX - (Math.max(barsNeeded * 9, saturationBarsNeeded * 6 + (int) (mc.fontRendererObj.getStringWidth(saturationText) * 0.75f))) - 4;
		int topY = (shouldDrawBelow ? toolTipBottomY : toolTipY - 20 + TOOLTIP_REAL_HEIGHT_OFFSET_TOP);
		int bottomY = topY + 20;

		GlStateManager.disableLighting();
		GlStateManager.disableDepth();

		// bg
		Gui.drawRect(leftX - 1, topY, rightX + 1, bottomY, 0xF0100010);
		Gui.drawRect(leftX, (shouldDrawBelow ? bottomY : topY - 1), rightX, (shouldDrawBelow ? bottomY + 1 : topY), 0xF0100010);
		Gui.drawRect(leftX, topY, rightX, bottomY, 0x66FFFFFF);

		// drawRect disables blending and modifies color, so reset them
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		int x = rightX - 2;
		int startX = x;
		int y = bottomY - 19;

		mc.getTextureManager().bindTexture(Gui.ICONS);

		for (int i = 0; i < barsNeeded * 2; i += 2)
		{
			x -= 9;

			if (modifiedFoodValues.hunger < 0)
				gui.drawTexturedModalRect(x, y, 34, 27, 9, 9);
			else if (modifiedFoodValues.hunger > defaultFoodValues.hunger && defaultFoodValues.hunger <= i)
				gui.drawTexturedModalRect(x, y, 133, 27, 9, 9);
			else if (modifiedFoodValues.hunger > i + 1 || defaultFoodValues.hunger == modifiedFoodValues.hunger)
				gui.drawTexturedModalRect(x, y, 16, 27, 9, 9);
			else if (modifiedFoodValues.hunger == i + 1)
				gui.drawTexturedModalRect(x, y, 124, 27, 9, 9);
			else
				gui.drawTexturedModalRect(x, y, 34, 27, 9, 9);

			GlStateManager.color(1.0F, 1.0F, 1.0F, .25F);
			gui.drawTexturedModalRect(x, y, defaultFoodValues.hunger - 1 == i ? 115 : 106, 27, 9, 9);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

			if (modifiedFoodValues.hunger > i)
				gui.drawTexturedModalRect(x, y, modifiedFoodValues.hunger - 1 == i ? 61 : 52, 27, 9, 9);
		}

		y += 11;
		x = startX;
		float modifiedSaturationIncrement = modifiedFoodValues.getSaturationIncrement();
		float absModifiedSaturationIncrement = Math.abs(modifiedSaturationIncrement);

		GlStateManager.pushMatrix();
		GlStateManager.scale(0.75F, 0.75F, 0.75F);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		for (int i = 0; i < saturationBarsNeeded * 2; i += 2)
		{
			float effectiveSaturationOfBar = (absModifiedSaturationIncrement - i) / 2f;

			x -= 6;

			boolean shouldBeFaded = absModifiedSaturationIncrement <= i;
			if (shouldBeFaded)
				GlStateManager.color(1.0F, 1.0F, 1.0F, .5F);

			mc.getTextureManager().bindTexture(Gui.ICONS);
			gui.drawTexturedModalRect(x * 4 / 3, y * 4 / 3, 16, 27, 9, 9);

			mc.getTextureManager().bindTexture(modIcons);
			gui.drawTexturedModalRect(x * 4 / 3, y * 4 / 3, effectiveSaturationOfBar >= 1 ? 27 : effectiveSaturationOfBar > 0.5 ? 18 : effectiveSaturationOfBar > 0.25 ? 9 : effectiveSaturationOfBar > 0 ? 0 : 36, modifiedSaturationIncrement >= 0 ? 0 : 9, 9, 9);

			if (shouldBeFaded)
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		}
		if (saturationText != null)
		{
			mc.fontRendererObj.drawStringWithShadow(saturationText, x * 4 / 3 - mc.fontRendererObj.getStringWidth(saturationText) + 2, y * 4 / 3 + 1, 0xFFFF0000);
		}
		GlStateManager.popMatrix();

		GlStateManager.disableBlend();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		// reset to drawHoveringText state
		GlStateManager.disableRescaleNormal();
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
	}
}
