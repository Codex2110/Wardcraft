package com.codex.wards.tiles;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import scala.actors.threadpool.Arrays;
import scala.collection.mutable.LinkedList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import com.codex.wards.blocks.wards.MajorWard;
import com.codex.wards.blocks.wards.RangeMultiplierWard;
import com.codex.wards.helpers.WardNetHelper;
import com.codex.wards.particles.LanguageMovingParticle;

public class KeyTileEntity extends TileEntity implements IUpdatePlayerListBox {

	private BlockPos corner1, corner2, corner3, corner4;
	private Iterator<BlockPos> blocks1, blocks2, blocks3, blocks4;
	private int range, ticks, power, maxPower;

	public KeyTileEntity() {
	}

	public int getRange() {

		range = 0;

		BlockPos bottomCorner = this.pos.add(-1, 0, -1);
		BlockPos topCorer = this.pos.add(1, 0, 1);

		Iterator<BlockPos> blockIterator = BlockPos.getAllInBox(topCorer,
				bottomCorner).iterator();

		while (blockIterator.hasNext()) {
			if (worldObj.getBlockState(blockIterator.next()).getBlock() instanceof RangeMultiplierWard) {
				range++;
			}
		}
		return (int) Math.pow(2, range - 1);
	}

	@Override
	public void update() {
		ticks++;

		if (ticks > 20) {
			int newRange = getRange();
			corner1 = this.pos.add(newRange, 0, newRange);
			corner2 = this.pos.add(-newRange, 0, newRange);
			corner3 = this.pos.add(-newRange, 0, -newRange);
			corner4 = this.pos.add(newRange, 0, -newRange);

			if (worldObj.isRemote) {
				blocks1 = BlockPos.getAllInBox(corner1, corner2).iterator();
				blocks2 = BlockPos.getAllInBox(corner2, corner3).iterator();
				blocks3 = BlockPos.getAllInBox(corner3, corner4).iterator();
				blocks4 = BlockPos.getAllInBox(corner4, corner1).iterator();
			}

			if (allCornersAreWardBlocks()) {
				WardNetHelper.decideAction(this);
				System.out.println(power);
			}

			ticks = 0;
		}

		if (blocks1 != null && corner1 != null) {
			spawnParticlesFromIterator(blocks1, corner1, corner2);
			spawnParticlesFromIterator(blocks2, corner2, corner3);
			spawnParticlesFromIterator(blocks3, corner3, corner4);
			spawnParticlesFromIterator(blocks4, corner4, corner1);
		}

		markDirty();

	}

	private void spawnParticlesFromIterator(Iterator<BlockPos> blocks,
			BlockPos start, BlockPos end) {
		while (blocks.hasNext()) {
			BlockPos pos = blocks.next();
			spawnParticles(worldObj, pos, start, end);
		}
	}

	private void spawnParticles(World world, BlockPos pos, BlockPos start,
			BlockPos end) {
		Random random = world.rand;
		EntityFX letters = new LanguageMovingParticle(world, pos.getX()
				+ random.nextFloat(), pos.getY() + random.nextFloat(),
				pos.getZ() + random.nextFloat(), 0, 0, 0, start, end);
		Minecraft.getMinecraft().effectRenderer.addEffect(letters);
	}

	private boolean allCornersAreWardBlocks() {
		boolean allCornersAreWardBlocks = true;

		if (!(worldObj.getBlockState(corner1).getBlock() instanceof MajorWard)) {
			allCornersAreWardBlocks = false;
		}
		if (!(worldObj.getBlockState(corner2).getBlock() instanceof MajorWard)) {
			allCornersAreWardBlocks = false;
		}
		if (!(worldObj.getBlockState(corner3).getBlock() instanceof MajorWard)) {
			allCornersAreWardBlocks = false;
		}
		if (!(worldObj.getBlockState(corner4).getBlock() instanceof MajorWard)) {
			allCornersAreWardBlocks = false;
		}
		return allCornersAreWardBlocks;
	}

	public List<BlockPos> getCorners() {
		return Arrays.asList(new BlockPos[] { corner1, corner2, corner3,
				corner4 });
	}

	public List<BlockPos> getAllBlocks() {
		List<BlockPos> pos = new java.util.LinkedList<BlockPos>();
		Iterator<BlockPos> blockIt1 = BlockPos.getAllInBox(corner1, corner2)
				.iterator();
		Iterator<BlockPos> blockIt2 = BlockPos.getAllInBox(corner2, corner3)
				.iterator();
		Iterator<BlockPos> blockIt3 = BlockPos.getAllInBox(corner3, corner4)
				.iterator();
		Iterator<BlockPos> blockIt4 = BlockPos.getAllInBox(corner4, corner1)
				.iterator();

		while (blockIt1.hasNext()) {
			pos.add(blockIt1.next());
		}
		while (blockIt2.hasNext()) {
			pos.add(blockIt2.next());
		}
		while (blockIt3.hasNext()) {
			pos.add(blockIt3.next());
		}
		while (blockIt4.hasNext()) {
			pos.add(blockIt3.next());
		}

		return pos;
	}

	public boolean decreasePower(int cost) {
		if (power >= cost) {
			power -= cost;
			worldObj.markBlockForUpdate(pos);
			markDirty();
			return true;
		}
		return false;
	}

	public boolean increasePower(int amount) {
		if (power + amount <= getMaxPower()) {
			power += amount;
			worldObj.markBlockForUpdate(pos);
			markDirty();
			return true;
		} else {
			power = maxPower;
			worldObj.markBlockForUpdate(pos);
			markDirty();
			return false;
		}
	}

	private int getMaxPower() {
		return getRange() * 100;
	}

	@Override
	public void writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		compound.setInteger("power", power);
	}

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		super.readFromNBT(compound);
		this.power = compound.getInteger("power");
	}

	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound tag = new NBTTagCompound();
		writeToNBT(tag);

		return new S35PacketUpdateTileEntity(this.pos, 1, tag);
	}
	
	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
		readFromNBT(pkt.getNbtCompound());
	}
}
