/**
 * This class was created by <Vazkii>. It's distributed as part of the Botania
 * Mod. Get the Source Code in github: https://github.com/Vazkii/Botania
 * 
 * Botania is Open Source and distributed under the Botania License:
 * http://botaniamod.net/license.php
 * 
 * File Created @ [Mar 29, 2015, 10:12:50 PM (GMT)]
 */
package shadows.plants2.item;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import shadows.plants2.client.IHasModel;
import shadows.plants2.data.Constants;
import shadows.plants2.data.IHasRecipe;
import shadows.plants2.init.ModRegistry;
import shadows.plants2.util.RecipeHelper;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.internal.IManaBurst;
import vazkii.botania.api.mana.BurstProperties;
import vazkii.botania.api.mana.ILensEffect;
import vazkii.botania.api.mana.IManaUsingItem;
import vazkii.botania.common.core.handler.ModSounds;
import vazkii.botania.common.core.helper.ItemNBTHelper;
import vazkii.botania.common.core.helper.Vector3;
import vazkii.botania.common.entity.EntityManaBurst;
import vazkii.botania.common.item.ModItems;
import vazkii.botania.common.item.equipment.tool.ToolCommons;

public class ItemExcalibur extends ItemSword implements ILensEffect, IManaUsingItem, IHasModel, IHasRecipe {

	private static final String TAG_ATTACKER_USERNAME = "attackerUsername";
	private static final String TAG_HOME_ID = "homeID";

	public static ToolMaterial toolMaterial = EnumHelper.addToolMaterial("EXCALIBUR", 3, -1, 6.2F, 6F, 40);

	public ItemExcalibur() {
		super(toolMaterial);
		setCreativeTab(ModRegistry.TAB);
		setUnlocalizedName(Constants.MODID + ".excalibur");
		setRegistryName("excalibur");
		ModRegistry.ITEMS.add(this);
	}

	@Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		if (entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) entity;
			PotionEffect haste = player.getActivePotionEffect(Potion.getPotionById(3));
			float check = haste == null ? 0.16666667F : haste.getAmplifier() == 1 ? 0.5F : 0.4F;

			if (player.getHeldItemMainhand() == stack && player.swingProgress == check && !world.isRemote) {
				EntityManaBurst burst = getBurst(player, stack);
				world.spawnEntity(burst);
				world.playSound(null, player.posX, player.posY, player.posZ, ModSounds.terraBlade, SoundCategory.PLAYERS, 0.4F, 1.4F);
			}
		}
	}

	@Override
	public boolean usesMana(ItemStack stack) {
		return false;
	}

	@Override
	public boolean hasEffect(ItemStack stack) {
		return true;
	}

	@Override
	public int getEntityLifespan(ItemStack itemStack, World world) {
		return Integer.MAX_VALUE;
	}

	@Override
	public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
		Multimap<String, AttributeModifier> multimap = HashMultimap.create();
		if (slot == EntityEquipmentSlot.MAINHAND) {
			multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", 10, 0));
			multimap.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", 0.3, 1));
		}
		return multimap;
	}

	public EntityManaBurst getBurst(EntityPlayer player, ItemStack stack) {
		EntityManaBurst burst = new EntityManaBurst(player);

		float motionModifier = 7F;

		burst.setColor(0xFFFF20);
		burst.setMana(1);
		burst.setStartingMana(1);
		burst.setMinManaLoss(200);
		burst.setManaLossPerTick(1F);
		burst.setGravity(0F);
		burst.setMotion(burst.motionX * motionModifier, burst.motionY * motionModifier, burst.motionZ * motionModifier);

		ItemStack lens = stack.copy();
		ItemNBTHelper.setString(lens, TAG_ATTACKER_USERNAME, player.getCommandSenderEntity().getName());
		burst.setSourceLens(lens);
		return burst;
	}

	@Override
	public void apply(ItemStack stack, BurstProperties props) { // NO-OP 

	}

	@Override
	public boolean collideBurst(IManaBurst burst, RayTraceResult pos, boolean isManaBlock, boolean dead, ItemStack stack) {
		return dead;
	}

	@Override
	public void updateBurst(IManaBurst iManaBurst, ItemStack stack) {
		EntityThrowable burst = (EntityThrowable) iManaBurst;
		AxisAlignedBB axis = new AxisAlignedBB(burst.posX, burst.posY, burst.posZ, burst.lastTickPosX, burst.lastTickPosY, burst.lastTickPosZ).grow(1, 1, 1);

		String attacker = ItemNBTHelper.getString(iManaBurst.getSourceLens(), TAG_ATTACKER_USERNAME, "");
		int homeID = ItemNBTHelper.getInt(stack, TAG_HOME_ID, -1);
		if (homeID == -1) {
			AxisAlignedBB axis1 = new AxisAlignedBB(burst.posX, burst.posY, burst.posZ, burst.lastTickPosX, burst.lastTickPosY, burst.lastTickPosZ).expand(5, 5, 5);
			List<EntityLivingBase> entities = burst.world.getEntitiesWithinAABB(EntityLivingBase.class, axis1);
			for (EntityLivingBase living : entities) {
				if (living instanceof EntityPlayer || living instanceof EntityDragon || living instanceof EntityWither || !(living instanceof IMob) || living.hurtTime != 0)
					continue;

				homeID = living.getEntityId();
				ItemNBTHelper.setInt(stack, TAG_HOME_ID, homeID);
				break;
			}
		}

		List<EntityLivingBase> entities = burst.world.getEntitiesWithinAABB(EntityLivingBase.class, axis);

		if (homeID != -1) {
			Entity home = burst.world.getEntityByID(homeID);
			if (home != null) {
				Vector3 vecEntity = Vector3.fromEntityCenter(home);
				Vector3 vecThis = Vector3.fromEntityCenter(burst);
				Vector3 vecMotion = vecEntity.subtract(vecThis);
				Vector3 vecCurrentMotion = new Vector3(burst.motionX, burst.motionY, burst.motionZ);

				vecMotion.normalize().multiply(vecCurrentMotion.mag());
				iManaBurst.setMotion(vecMotion.x, vecMotion.y, vecMotion.z);
			}
		}

		for (EntityLivingBase living : entities) {
			if (living instanceof EntityPlayer && (((EntityPlayer) living).getCommandSenderEntity().getName().equals(attacker) || living.world.getMinecraftServer() != null && living.world.getMinecraftServer().isPVPEnabled()))
				continue;

			if (living.hurtTime == 0) {
				int cost = 1;
				int mana = iManaBurst.getMana();
				if (mana >= cost) {
					iManaBurst.setMana(mana - cost);
					float damage = 4F + toolMaterial.getDamageVsEntity();
					if (!iManaBurst.isFake() && !burst.world.isRemote) {
						EntityPlayer player = living.world.getPlayerEntityByName(attacker);
						living.attackEntityFrom(player == null ? DamageSource.MAGIC : DamageSource.causePlayerDamage(player), damage);
						burst.setDead();
						break;
					}
				}
			}
		}
	}

	@Override
	public boolean doParticles(IManaBurst burst, ItemStack stack) {
		return true;
	}

	@Override
	public boolean hitEntity(ItemStack par1ItemStack, EntityLivingBase par2EntityLivingBase,

			@Nonnull EntityLivingBase par3EntityLivingBase) {
		if (usesMana(par1ItemStack))
			ToolCommons.damageItem(par1ItemStack, 1, par3EntityLivingBase, getManaPerDamage());
		return true;
	}

	@Override
	public boolean onBlockDestroyed(@Nonnull ItemStack stack, @Nonnull World world, IBlockState state,

			@Nonnull BlockPos pos, @Nonnull EntityLivingBase entity) {
		if (usesMana(stack) && state.getBlockHardness(world, pos) != 0F)
			ToolCommons.damageItem(stack, 1, entity, getManaPerDamage());

		return true;
	}

	public int getManaPerDamage() {
		return 60;
	}

	@Override
	public boolean getIsRepairable(ItemStack par1ItemStack, @Nonnull ItemStack par2ItemStack) {
		return par2ItemStack.getItem() == ModItems.manaResource && par2ItemStack.getItemDamage() == 0 ? true : super.getIsRepairable(par1ItemStack, par2ItemStack);
	}

	@Nonnull

	@Override
	public EnumRarity getRarity(ItemStack stack) {
		return BotaniaAPI.rarityRelic;
	}

	@Override
	public void initRecipes(Register<IRecipe> e) {
		RecipeHelper.addShapeless(this, ForgeRegistries.ITEMS.getValue(new ResourceLocation(Constants.BOTANIA_ID, "kingkey")));
	}
}