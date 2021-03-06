package squeek.applecore.asm.module;

import org.objectweb.asm.tree.*;
import squeek.applecore.asm.ASMConstants;
import squeek.applecore.asm.IClassTransformerModule;
import squeek.asmhelper.applecore.ASMHelper;
import squeek.asmhelper.applecore.InsnComparator;
import squeek.asmhelper.applecore.ObfHelper;

import static org.objectweb.asm.Opcodes.*;

public class ModuleBlockFood implements IClassTransformerModule
{
	@Override
	public String[] getClassesToTransform()
	{
		return new String[]{ASMConstants.CAKE};
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		ClassNode classNode = ASMHelper.readClassFromBytes(basicClass);

		MethodNode methodNode = ASMHelper.findMethodNodeOfClass(classNode, "func_180682_b", "eatCake", ASMHelper.toMethodDescriptor("V", ASMConstants.WORLD, ASMConstants.BLOCK_POS, ASMConstants.IBLOCKSTATE, ASMConstants.PLAYER));

		if (methodNode != null)
		{
			addOnBlockFoodEatenHook(classNode, methodNode);
			return ASMHelper.writeClassToBytes(classNode);
		}
		else
			throw new RuntimeException("BlockCake: eatCake (func_180682_b) method not found");
	}

	private void addOnBlockFoodEatenHook(ClassNode classNode, MethodNode method)
	{
		AbstractInsnNode firstUniqueInsnInsideIf = ASMHelper.find(method.instructions, new FieldInsnNode(GETSTATIC, ASMHelper.toInternalClassName(ASMConstants.STAT_LIST), InsnComparator.WILDCARD,ASMHelper.toDescriptor(ASMConstants.STAT_BASE)));

		if (firstUniqueInsnInsideIf == null)
			throw new RuntimeException("Target instruction not found in " + classNode.name + "." + method.name);

		AbstractInsnNode returnInsn = ASMHelper.findPreviousInstructionWithOpcode(method.instructions.getLast(), RETURN);
		AbstractInsnNode prevLabel = ASMHelper.findPreviousLabelOrLineNumber(returnInsn);
		while (prevLabel != null && prevLabel.getType() != AbstractInsnNode.LABEL)
			prevLabel = ASMHelper.findPreviousLabelOrLineNumber(prevLabel);

		LabelNode ifEndLabel = (LabelNode) prevLabel;

		/*
		 * Modify food values
		 */
		InsnList toInject = new InsnList();
		AbstractInsnNode targetNode = firstUniqueInsnInsideIf.getPrevious();

		// create modifiedFoodValues variable
		LabelNode modifiedFoodValuesStart = new LabelNode();
		LocalVariableNode modifiedFoodValues = new LocalVariableNode("modifiedFoodValues", ASMHelper.toDescriptor(ASMConstants.FOOD_VALUES), null, modifiedFoodValuesStart, ifEndLabel, method.maxLocals);
		method.maxLocals += 1;
		method.localVariables.add(modifiedFoodValues);

		// FoodValues modifiedFoodValues = Hooks.onBlockFoodEaten(this, p_150036_1_, p_150036_5_);
		toInject.add(new VarInsnNode(ALOAD, 0));
		toInject.add(new VarInsnNode(ALOAD, 1));
		toInject.add(new VarInsnNode(ALOAD, 4));
		toInject.add(new MethodInsnNode(INVOKESTATIC, ASMConstants.HOOKS_INTERNAL_CLASS, "onBlockFoodEaten",  ASMHelper.toMethodDescriptor(ASMConstants.FOOD_VALUES, ASMConstants.BLOCK, ASMConstants.WORLD, ASMConstants.PLAYER), false));
		toInject.add(new VarInsnNode(ASTORE, modifiedFoodValues.index));
		toInject.add(modifiedFoodValuesStart);

		// create prevFoodLevel variable
		LabelNode prevFoodLevelStart = new LabelNode();
		LocalVariableNode prevFoodLevel = new LocalVariableNode("prevFoodLevel", "I", null, prevFoodLevelStart, ifEndLabel, method.maxLocals);
		method.maxLocals += 1;
		method.localVariables.add(prevFoodLevel);

		// int prevFoodLevel = p_150036_5_.getFoodStats().getFoodLevel();
		toInject.add(new VarInsnNode(ALOAD, 4));
		toInject.add(new MethodInsnNode(INVOKEVIRTUAL, ObfHelper.getInternalClassName(ASMConstants.PLAYER), ObfHelper.isObfuscated() ? "func_71024_bL" : "getFoodStats", ASMHelper.toMethodDescriptor(ASMConstants.FOOD_STATS), false));
		toInject.add(new MethodInsnNode(INVOKEVIRTUAL, ObfHelper.getInternalClassName(ASMConstants.FOOD_STATS), ObfHelper.isObfuscated() ? "func_75116_a" : "getFoodLevel", ASMHelper.toMethodDescriptor("I"), false));
		toInject.add(new VarInsnNode(ISTORE, prevFoodLevel.index));
		toInject.add(prevFoodLevelStart);

		// create prevSaturationLevel variable
		LabelNode prevSaturationLevelStart = new LabelNode();
		LocalVariableNode prevSaturationLevel = new LocalVariableNode("prevSaturationLevel", "F", null, prevSaturationLevelStart, ifEndLabel, method.maxLocals);
		method.maxLocals += 1;
		method.localVariables.add(prevSaturationLevel);

		// float prevSaturationLevel = p_150036_5_.getFoodStats().getSaturationLevel();
		toInject.add(new VarInsnNode(ALOAD, 4));
		toInject.add(new MethodInsnNode(INVOKEVIRTUAL, ObfHelper.getInternalClassName(ASMConstants.PLAYER), ObfHelper.isObfuscated() ? "func_71024_bL" : "getFoodStats", ASMHelper.toMethodDescriptor(ASMConstants.FOOD_STATS), false));
		toInject.add(new MethodInsnNode(INVOKEVIRTUAL, ObfHelper.getInternalClassName(ASMConstants.FOOD_STATS), ObfHelper.isObfuscated() ? "func_75115_e" : "getSaturationLevel", ASMHelper.toMethodDescriptor("F"), false));
		toInject.add(new VarInsnNode(FSTORE, prevSaturationLevel.index));
		toInject.add(prevSaturationLevelStart);

		method.instructions.insertBefore(targetNode, toInject);

		/*
		 * Replace 2/0.1F with the modified values
		 */
		InsnList hungerNeedle = new InsnList();
		hungerNeedle.add(new InsnNode(ICONST_2));

		InsnList hungerReplacement = new InsnList();
		hungerReplacement.add(new VarInsnNode(ALOAD, modifiedFoodValues.index));
		hungerReplacement.add(new FieldInsnNode(GETFIELD, ASMHelper.toInternalClassName(ASMConstants.FOOD_VALUES), "hunger", "I"));

		InsnList saturationNeedle = new InsnList();
		saturationNeedle.add(new LdcInsnNode(0.1f));

		InsnList saturationReplacement = new InsnList();
		saturationReplacement.add(new VarInsnNode(ALOAD, modifiedFoodValues.index));
		saturationReplacement.add(new FieldInsnNode(GETFIELD, ASMHelper.toInternalClassName(ASMConstants.FOOD_VALUES), "saturationModifier", "F"));

		ASMHelper.findAndReplace(method.instructions, hungerNeedle, hungerReplacement, targetNode);
		ASMHelper.findAndReplace(method.instructions, saturationNeedle, saturationReplacement, targetNode);

		/*
		 * onPostBlockFoodEaten
		 */
		AbstractInsnNode targetNodeAfter = ASMHelper.find(targetNode, new MethodInsnNode(INVOKEVIRTUAL, ASMHelper.toInternalClassName(ASMConstants.FOOD_STATS), InsnComparator.WILDCARD, ASMHelper.toMethodDescriptor("V", "I", "F"), false));
		InsnList toInjectAfter = new InsnList();

		// Hooks.onPostBlockFoodEaten(this, modifiedFoodValues, prevFoodLevel, prevSaturationLevel, p_150036_5_);
		toInjectAfter.add(new VarInsnNode(ALOAD, 0));
		toInjectAfter.add(new VarInsnNode(ALOAD, modifiedFoodValues.index));
		toInjectAfter.add(new VarInsnNode(ILOAD, prevFoodLevel.index));
		toInjectAfter.add(new VarInsnNode(FLOAD, prevSaturationLevel.index));
		toInjectAfter.add(new VarInsnNode(ALOAD, 4));
		toInjectAfter.add(new MethodInsnNode(INVOKESTATIC, ASMHelper.toInternalClassName(ASMConstants.HOOKS), "onPostBlockFoodEaten", ASMHelper.toMethodDescriptor("V", ASMConstants.BLOCK, ASMConstants.FOOD_VALUES, "I", "F", ASMConstants.PLAYER), false));

		method.instructions.insert(targetNodeAfter, toInjectAfter);
	}
}