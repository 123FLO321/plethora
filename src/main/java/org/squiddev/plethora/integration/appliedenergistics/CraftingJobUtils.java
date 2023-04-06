package org.squiddev.plethora.integration.appliedenergistics;

import appeng.api.AEApi;
import appeng.api.networking.crafting.CraftingItemList;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.crafting.CraftingJob;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.CraftingTreeProcess;
import appeng.util.item.AEItemStack;
import dan200.computercraft.api.lua.LuaException;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import org.squiddev.plethora.integration.vanilla.meta.MetaItemBasic;
import appeng.me.cluster.implementations.CraftingCPUCluster;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class CraftingJobUtils {

	static class TaskProgress {
		public long value;

		TaskProgress(long value) {
			this.value = value;
		}
	}

	static class CraftingTree {
		public final ItemStack item;
		public final long amount;
		public final long missing;
		public final long active;
		public final long pending;

		public final ArrayList<CraftingTreeProcess> children = new ArrayList<>();

		public CraftingTree(ItemStack item, long amount, long missing, long active, long pending) {
			this.item = item;
			this.amount = amount;
			this.missing = missing;
			this.active = active;
			this.pending = pending;
		}

		public void addChild(CraftingTreeProcess child) {
			children.add(child);
		}

		static class CraftingTreeProcess {
			public final long amount;
			public final long pending;
			public final ArrayList<CraftingTree> children = new ArrayList<>();

			public CraftingTreeProcess(long amount, long pending) {
				this.amount = amount;
				this.pending = pending;
			}

			public void addChild(CraftingTree child) {
				children.add(child);
			}
		}
	}

	@Nullable
	private static CraftingTree buildNodeTree(CraftingJob job, CraftingTreeNode parent, IItemList<IAEItemStack> activeList, IItemList<IAEItemStack> pendingList, Map<ICraftingPatternDetails, TaskProgress> tasks) throws IllegalAccessException, NoSuchFieldException {
		Field nodesField = CraftingTreeNode.class.getDeclaredField("nodes");
		nodesField.setAccessible(true);
		ArrayList<CraftingTreeProcess> nodes = (ArrayList<CraftingTreeProcess>) nodesField.get(parent);

		// TODO: maybe load all outputs and not only the required ones
		Field whatField = CraftingTreeNode.class.getDeclaredField("what");
		whatField.setAccessible(true);
		AEItemStack what = (AEItemStack) ((AEItemStack) whatField.get(parent)).copy();

		if (!nodes.isEmpty()) what.setStackSize(0);
		for (CraftingTreeProcess node : nodes) {
			Field defailsField = CraftingTreeProcess.class.getDeclaredField("details");
			defailsField.setAccessible(true);
			ICraftingPatternDetails details = (ICraftingPatternDetails) defailsField.get(node);

			Field craftsField = CraftingTreeProcess.class.getDeclaredField("crafts");
			craftsField.setAccessible(true);
			long crafts = (long) craftsField.get(node);

			for (IAEItemStack output : details.getOutputs()) {
				if (output.equals(what)) what.incStackSize(output.getStackSize() * crafts);
			}
		}

		Field missingField = CraftingTreeNode.class.getDeclaredField("missing");
		missingField.setAccessible(true);
		long missing = (long) missingField.get(parent);
		what.incStackSize(missing);

		if (what.getStackSize() == 0) return null;

		IAEItemStack activeStack = activeList.findPrecise(what);
		long active = Math.min(activeStack == null ? 0 : activeStack.getStackSize(), what.getStackSize() - missing);
		if (activeStack != null) activeStack.decStackSize(active);

		IAEItemStack pendingStack = pendingList.findPrecise(what);
		long pending = Math.min(pendingStack == null ? 0 : pendingStack.getStackSize(), what.getStackSize() - missing);
		if (pendingStack != null) pendingStack.decStackSize(pending);

		CraftingTree tree = new CraftingTree(what.createItemStack(), what.getStackSize(), missing, active, pending);

		for (CraftingTreeProcess node : nodes) {
			CraftingTreeNode[] children = getTreeNodes(node);
			if (children.length > 0) {
				CraftingTree.CraftingTreeProcess processTree = buildProcessTree(job, node, children, activeList, pendingList, tasks);
				if (processTree != null) tree.addChild(processTree);
			}
		}

		return tree;
	}

	@Nullable
	private static CraftingTree.CraftingTreeProcess buildProcessTree(CraftingJob job, CraftingTreeProcess node, CraftingTreeNode[] children, IItemList<IAEItemStack> activeList, IItemList<IAEItemStack> pendingList, Map<ICraftingPatternDetails, TaskProgress> tasks) throws NoSuchFieldException, IllegalAccessException {
		Field craftsField = CraftingTreeProcess.class.getDeclaredField("crafts");
		craftsField.setAccessible(true);
		long crafts = (long) craftsField.get(node);

		if (crafts == 0) return null;

		Field detailsField = CraftingTreeProcess.class.getDeclaredField("details");
		detailsField.setAccessible(true);
		ICraftingPatternDetails details = (ICraftingPatternDetails) detailsField.get(node);

		TaskProgress progress = tasks.get(details);
		long pending = Math.min(progress == null ? 0 : progress.value, crafts);
		if (progress != null) progress.value -= pending;

		CraftingTree.CraftingTreeProcess processTree = new CraftingTree.CraftingTreeProcess(crafts, pending);

		for (CraftingTreeNode child : children) {
			CraftingTree childTree = buildNodeTree(job, child, activeList, pendingList, tasks);
			if (childTree != null) processTree.addChild(childTree);
		}
		return processTree;
	}

	private static CraftingTreeNode[] getTreeNodes(CraftingTreeProcess parent) throws NoSuchFieldException, IllegalAccessException {
		Field nodesField = CraftingTreeProcess.class.getDeclaredField("nodes");
		nodesField.setAccessible(true);
		Object2LongArrayMap<CraftingTreeNode> nodes = (Object2LongArrayMap<CraftingTreeNode>) nodesField.get(parent);
		return nodes.keySet().toArray(new CraftingTreeNode[0]);
	}

	private static CraftingTree buildFlatTree(CraftingCPUCluster cluster, IItemList<IAEItemStack> activeList, IItemList<IAEItemStack> pendingList) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		CraftingTree tree;
		Method getFinalOutputMethod = CraftingCPUCluster.class.getDeclaredMethod("getFinalOutput");
		getFinalOutputMethod.setAccessible(true);
		IAEItemStack finalOutput = (IAEItemStack) getFinalOutputMethod.invoke(cluster);

		Map<IAEItemStack, Tuple<Long, Long>> combinedList = new HashMap<>();
		for (IAEItemStack stack : activeList) {
			combinedList.put(stack, new Tuple<>(stack.getStackSize(), 0L));
		}
		for (IAEItemStack stack : pendingList) {
			Tuple<Long, Long> tuple = combinedList.get(stack);
			if (tuple == null) {
				combinedList.put(stack, new Tuple<>(0L, stack.getStackSize()));
			} else {
				combinedList.put(stack, new Tuple<>(tuple.getFirst(), tuple.getSecond() + stack.getStackSize()));
			}
		}

		IAEItemStack outputActive = activeList.findPrecise(finalOutput);
		IAEItemStack outputPending = pendingList.findPrecise(finalOutput);
		tree = new CraftingTree(finalOutput.createItemStack(), finalOutput.getStackSize(), 0, outputActive == null ? 0 : outputActive.getStackSize(), outputPending == null ? 0 : outputPending.getStackSize());
		CraftingTree.CraftingTreeProcess processTree = new CraftingTree.CraftingTreeProcess(1, 1);
		for (IAEItemStack stack : combinedList.keySet()) {
			if (stack.equals(finalOutput)) continue;
			Tuple<Long, Long> tuple = combinedList.get(stack);
			CraftingTree nodeTree = new CraftingTree(stack.createItemStack(), tuple.getFirst() + tuple.getSecond(), 0, tuple.getFirst(), tuple.getSecond());
			processTree.addChild(nodeTree);
		}
		if (processTree.children.size() > 0) tree.addChild(processTree);
		return tree;
	}

	private static Map<String, Object> mapNodeTree(CraftingTree tree) {
		Map<String, Object> map = new HashMap<>();
		Object[] children = new Object[tree.children.size()];
		for (int i = 0; i < children.length; i++) {
			children[i] = mapProcessTree(tree.children.get(i));
		}
		if (children.length > 0) map.put("children", children);

		Map<String, Object> item = new HashMap<>();
		item.put("name", tree.item.getItem().getRegistryName().toString());
		item.put("damage", tree.item.getItemDamage());
		item.put("nbtHash", MetaItemBasic.getNBTHash(tree.item));
		map.put("item", item);

		map.put("amount", tree.amount);
		map.put("missing", tree.missing);
		map.put("active", tree.active);
		map.put("pending", tree.pending);
		return map;
	}

	private static Map<String, Object> mapProcessTree(CraftingTree.CraftingTreeProcess tree) {
		Map<String, Object> map = new HashMap<>();
		Object[] children = new Object[tree.children.size()];
		for (int i = 0; i < children.length; i++) {
			children[i] = mapNodeTree(tree.children.get(i));
		}
		if (children.length > 0) map.put("children", children);

		map.put("amount", tree.amount);
		map.put("pending", tree.pending);
		return map;
	}

	public static Map<String, Object> mapJob(@Nullable CraftingJob job, @Nullable CraftingCPUCluster cluster) throws LuaException {
		try {
			IItemList<IAEItemStack> activeList = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();
			IItemList<IAEItemStack> pendingList = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();
			Map<ICraftingPatternDetails, TaskProgress> tasks = new HashMap<>();

			if (cluster != null) {
				Field tasksField = CraftingCPUCluster.class.getDeclaredField("tasks");
				tasksField.setAccessible(true);
				tasks = ((Map<ICraftingPatternDetails, Object>) tasksField.get(cluster)).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
					try {
						Field valueField = entry.getValue().getClass().getDeclaredField("value");
						valueField.setAccessible(true);
						long value = (long) valueField.get(entry.getValue());
						return new TaskProgress(value);
					} catch (NoSuchFieldException | IllegalAccessException e) {
						e.printStackTrace();
						return new TaskProgress(0);
					}
				}));

				IItemList<IAEItemStack> tempActiveList = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();
				IItemList<IAEItemStack> tempPendingList = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();

				cluster.getListOfItem(tempActiveList, CraftingItemList.ACTIVE);
				cluster.getListOfItem(tempPendingList, CraftingItemList.PENDING);

				tempActiveList.forEach(stack -> activeList.add(stack.copy()));
				tempPendingList.forEach(stack -> pendingList.add(stack.copy()));
			}

			CraftingTree tree;
			if (job == null && cluster != null) {
				tree = buildFlatTree(cluster, activeList, pendingList);
			} else if (job != null) {
				tree = buildNodeTree(job, job.getTree(), activeList, pendingList, tasks);
			} else {
				throw new LuaException("Either job or cluster must be provided");
			}
			return tree != null ? mapNodeTree(tree) : null;
		} catch (Exception e) {
			e.printStackTrace();
			throw new LuaException("Failed to get job");
		}
	}

}

