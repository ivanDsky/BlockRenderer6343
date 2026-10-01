package blockrenderer6343;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.StatCollector;

/**
 * The multiblock structure "recipe" result: renders as the structure controller but is not it, so bookmarks and
 * autocraft don't consider the multiblock done once the controller is crafted. Display only - hidden from NEI. Its NBT
 * is just the serialized controller (id/Count/Damage) plus a "display" tag with name and lore. Registered on both
 * sides; the client-only rendering lives in MultiblockPreviewRenderer.
 */
public class ItemMultiblockPreview extends Item {

    public static final String REGISTRY_NAME = "multiblock_preview";
    public static final ItemMultiblockPreview INSTANCE = new ItemMultiblockPreview();

    private ItemMultiblockPreview() {
        setUnlocalizedName(BlockRenderer6343.MOD_ID + "." + REGISTRY_NAME);
        setCreativeTab(null);
    }

    public static ItemStack of(ItemStack controller, List<String> lore) {
        ItemStack stack = new ItemStack(INSTANCE);
        stack.setTagCompound(new NBTTagCompound());
        controller.writeToNBT(stack.getTagCompound());
        stack.setStackDisplayName(
                controller.getDisplayName() + " ("
                        + StatCollector.translateToLocal("blockrenderer6343.multiblock.structure")
                        + ")");
        NBTTagList loreTags = new NBTTagList();
        for (String line : lore) loreTags.appendTag(new NBTTagString(line));
        stack.getTagCompound().getCompoundTag("display").setTag("Lore", loreTags);
        return stack;
    }

    /** @return the controller stack this preview represents, or null */
    public static ItemStack getControllerStack(ItemStack stack) {
        return stack.hasTagCompound() ? ItemStack.loadItemStackFromNBT(stack.getTagCompound()) : null;
    }
}
