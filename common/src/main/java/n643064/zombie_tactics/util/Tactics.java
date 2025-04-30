package n643064.zombie_tactics.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.Rotation;

public class Tactics {
    public static final BlockPos UNIT_FRONT = new BlockPos(0, 0, 1);

    public static Rotation getRelativeRotation(Mob mob) {
        Vec3i norm = mob.getNearestViewDirection().getUnitVec3i();
        int x = norm.getX(), z = norm.getZ();
        if(x == 0 && z == 1) return Rotation.NONE;
        else if(x == 0 && z == -1) return Rotation.CLOCKWISE_180;
        else if(x == -1 && z == 0) return Rotation.CLOCKWISE_90;
        else return Rotation.COUNTERCLOCKWISE_90; // x = 1, z = 0
    }

    public static boolean isSword(ItemStack item) {
        return item.is(ItemTags.SWORDS);
    }

    public static class Armor {
        public static boolean isArmor(ItemStack item) {
            return item.is(ItemTags.CHEST_ARMOR) || item.is(ItemTags.LEG_ARMOR) || item.is(ItemTags.FOOT_ARMOR) || item.is(ItemTags.HEAD_ARMOR);
        }

        public static ArmorType getArmorType(ItemStack item) {
            if(item.is(ItemTags.CHEST_ARMOR)) return ArmorType.CHESTPLATE;
            if(item.is(ItemTags.LEG_ARMOR)) return ArmorType.LEGGINGS;
            if(item.is(ItemTags.FOOT_ARMOR)) return ArmorType.BOOTS;
            if(item.is(ItemTags.HEAD_ARMOR)) return ArmorType.HELMET;
            return null;
        }

        public static ArmorMaterial getArmorMaterial(ItemStack item) {
            if(item.is(Items.LEATHER_BOOTS) || item.is(Items.LEATHER_CHESTPLATE)
                    || item.is(Items.LEATHER_HELMET) || item.is(Items.LEATHER_LEGGINGS)) return ArmorMaterials.LEATHER;
            if(item.is(Items.CHAINMAIL_BOOTS) || item.is(Items.CHAINMAIL_CHESTPLATE)
                    || item.is(Items.CHAINMAIL_HELMET) || item.is(Items.CHAINMAIL_LEGGINGS)) return ArmorMaterials.CHAINMAIL;
            if(item.is(Items.GOLDEN_BOOTS) || item.is(Items.GOLDEN_CHESTPLATE)
                    || item.is(Items.GOLDEN_HELMET) || item.is(Items.GOLDEN_LEGGINGS)) return ArmorMaterials.GOLD;
            if(item.is(Items.IRON_BOOTS) || item.is(Items.IRON_CHESTPLATE)
                    || item.is(Items.IRON_HELMET) || item.is(Items.IRON_LEGGINGS)) return ArmorMaterials.IRON;
            if(item.is(Items.DIAMOND_BOOTS) || item.is(Items.DIAMOND_CHESTPLATE)
                    || item.is(Items.DIAMOND_HELMET) || item.is(Items.DIAMOND_LEGGINGS)) return ArmorMaterials.DIAMOND;
            if(item.is(Items.NETHERITE_BOOTS) || item.is(Items.NETHERITE_CHESTPLATE)
                    || item.is(Items.NETHERITE_HELMET) || item.is(Items.NETHERITE_LEGGINGS)) return ArmorMaterials.NETHERITE;
            return null;
        }
    }
}
