package com.iscas.JavaExP.model.analyzeModel;

import com.iscas.JavaExP.model.sootAnalysisModel.NestableObj;
import com.iscas.JavaExP.utils.PrintUtils;
import com.iscas.JavaExP.utils.ValueObtainer;
import org.jetbrains.annotations.NotNull;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.*;

import java.util.*;

public class ConditionWithValueSet implements  Cloneable {
	private Unit conditionUnit;
	private List<RefinedCondition> refinedConditions = new ArrayList<>();
	private SootMethod sootMethod;
	private Map<Value,String> value2StringMap = new HashMap<>();

	@Override
	public ConditionWithValueSet clone() throws CloneNotSupportedException {
		ConditionWithValueSet conditionWithValueSetClone = new ConditionWithValueSet(sootMethod, conditionUnit);
		for(RefinedCondition refinedCondition: refinedConditions) {
			RefinedCondition refinedConditionClone = refinedCondition.clone();
			refinedConditionClone.setConditionWithValueSet(conditionWithValueSetClone);
			conditionWithValueSetClone.getRefinedConditions().add(refinedConditionClone);
		}

		return conditionWithValueSetClone;
	}

	public ConditionWithValueSet(SootMethod sootMethod, Unit conditionUnit){
		this.sootMethod = sootMethod;
		this.conditionUnit = conditionUnit;
	}


	public Unit getConditionUnit() {
		return conditionUnit;
	}

	public void setConditionUnit(Unit conditionUnit) {
		this.conditionUnit = conditionUnit;
	}

	public List<RefinedCondition> getRefinedConditions() {
		return refinedConditions;
	}

	public void addRefinedCondition(RefinedCondition refinedCondition) {
		for(RefinedCondition old: refinedConditions){
			if(old.toString().equals(refinedCondition.toString()))
				return;
		}
		if(refinedCondition!=null) {
			if((refinedCondition.getLeftStr()!=null || refinedCondition.getLeftStr().length()>0)
				&& (refinedCondition.getRightStr()!=null ||refinedCondition.getRightStr().length()>0))
			this.refinedConditions.add(refinedCondition);
		}
	}

	@Override
	public String toString() {
		return PrintUtils.printList(refinedConditions, "\n") ;
	}

	public void optimizeConditionConservative() {
		List<RefinedCondition> list = new ArrayList<>();
		list = new ArrayList(refinedConditions);
		RefinedCondition keyCond = list.get(0);

		String leftStr, rightStr;
		leftStr = optimizeRefinedCondition(keyCond.getLeftVar());
		if(keyCond.getLeftVar().getUseBoxes().size()>0) {
			leftStr = keyCond.getLeftStr();
			for (ValueBox vb : keyCond.getLeftVar().getUseBoxes()) {
				leftStr = leftStr.replace(vb.getValue().toString(), optimizeRefinedCondition(vb.getValue()));
			}
		}
		rightStr = optimizeRefinedCondition(keyCond.getRightValue());
		if(keyCond.getRightValue().getUseBoxes().size()>0) {
			rightStr = keyCond.getRightStr();
			for (ValueBox vb : keyCond.getRightValue().getUseBoxes()) {
				rightStr = rightStr.replace(vb.getValue().toString(), optimizeRefinedCondition(vb.getValue()));
			}
		}
//		System.out.println(leftStr+" "+ keyCond.getOperator()+" "+ rightStr);
		refinedConditions.clear();
		keyCond.setLeftStr(leftStr);
		keyCond.setRightStr(rightStr);
		refinedConditions.add(keyCond);
	}

	private String optimizeRefinedCondition(Value value) {
		if (value2StringMap.containsKey(value)) return value2StringMap.get(value);
			for (int i = 1; i < refinedConditions.size(); i++) {
			RefinedCondition temp = refinedConditions.get(i);
			if (temp.getLeftVar() == value) {
				if (temp.getOperator() == IROperator.denoteOP || temp.getOperator() == IROperator.phiReplaceOp
						|| temp.getOperator() == IROperator.isOP || temp.getOperator() == IROperator.isiInvokeOP
						|| temp.getOperator() == IROperator.equalsOp) {
					String str = optimizeRefinedCondition(temp.getRightValue());
					if(temp.getRightValue().getUseBoxes().size()>0) {
						str = temp.getRightStr();
						for (ValueBox vb : temp.getRightValue().getUseBoxes()) {
							str = str.replace(vb.getValue().toString(), optimizeRefinedCondition(vb.getValue()));
						}
					}
					value2StringMap.put(value, str);
					return formatOutput(str);
				}
			}
		}
		if(value instanceof ParameterRef)
			return "parameter" + ((ParameterRef)value).getIndex();
		return formatOutput(value.toString());
	}

	private String formatOutput(String str) {
		if(str.startsWith("lengthof "))
			return str.replace("lengthof ","")+".length";
		return str;
	}


	public void optimizeConditionInConservative() {
		List<RefinedCondition> list = new ArrayList<>();
		boolean optimize = true;
		if (optimize) {
			int id = 1, signature = 12345;
			while(list.hashCode()!=signature && id++<5) {
				//optimize, transport denoted variables to each use point
				list = new ArrayList(refinedConditions);
				optimizePhiVariable(list);

				//optimize, model some judgement statements like equals
				list = new ArrayList(refinedConditions);
				optimizeInvocation(list);

				//optimize, transport denoted variables to each use point
				list = new ArrayList(refinedConditions);
				optimizeDenotedVariable(list);

				//optimize, transport denoted variables to each use point
				list = new ArrayList(refinedConditions);
				optimizeDenotedVariable2(list);

				//optimize, transport is invoke variables to each use point
				list = new ArrayList(refinedConditions);
				optimizeIsInvokeVariable(list);

				//optimize, transport is variables to each use point
				list = new ArrayList(refinedConditions);
				optimizeIsVariable(list);

				//optimize, model some judgement statements like equals
				list = new ArrayList(refinedConditions);
				optimizeCmpInst(list);
				signature = list.hashCode();
			}

			//optimize, remove redundant variables
			list = new ArrayList(refinedConditions);
			optimizeRedundant(list);

			//update the output format
			list = new ArrayList(refinedConditions);
			optimizePrintFormat(list);
		}
	}


	/**
	 * trasfer phi to unphi stmt
	 * @param list
	 */
	private void optimizePhiVariable(List<RefinedCondition> list) {
		Set<RefinedCondition> toBeDel = new HashSet<>();
		for(RefinedCondition refinedCondition: list) {
			if (refinedCondition.getOperator().equals(IROperator.phiReplaceOp)) {
				for (RefinedCondition temp : list) {
					if (temp == refinedCondition || temp.getOperator().equals(IROperator.phiReplaceOp)) continue;
					if (temp.getLeftStr().equals(refinedCondition.getLeftStr())) {
						temp.setLeftStr(refinedCondition.getRightStr());
						temp.setLeftVar(refinedCondition.getRightValue());
					}

					if(temp.getRightValue() instanceof  InvokeExpr){
						int i =0;
						for(Value value: ((InvokeExpr)temp.getRightValue()).getArgs()){
							if(value.toString().equals(refinedCondition.getLeftStr())){
								InvokeExpr rightInvoke = (InvokeExpr) (temp.getRightValue()).clone();
								try {
									rightInvoke.setArg(i, refinedCondition.getRightValue());
								}catch (Exception e){
								}
								RefinedCondition refinedCondition2 = new RefinedCondition(this,
										temp.getLeftVar(), temp.getOperator() , rightInvoke, temp.getUnit(), temp.isSatisfied());
								refinedCondition2.setLeftStr(temp.getLeftStr());
								addRefinedCondition(refinedCondition2);
								toBeDel.add(temp);
							}
							i++;
						}
					}
				}
			}
		}
		for(RefinedCondition temp: toBeDel){
			refinedConditions.remove(temp);
		}
	}

	/**
	 * set satisfactory for value condition, not path condition!
	 * @param list
	 */
	private void optimizeInvocation(@NotNull List<RefinedCondition> list) {
		Set<RefinedCondition> toBeDel = new HashSet<>();
		for(RefinedCondition refinedCondition: list) {
			boolean find = false;
			if (refinedCondition.getRightValue() == null) continue;
			if (refinedCondition.getRightValue() instanceof InstanceInvokeExpr) {
				boolean expressionIsTrue = false;
				for (RefinedCondition temp : list) {
					if(temp == refinedCondition) continue;
					// map invoke unit and invoke result
					if (refinedCondition.getLeftStr().equals(temp.getLeftStr())) {
						if (temp.getOperator().equals(IROperator.isNotOP) && temp.getRightStr().equals("0"))
							expressionIsTrue = true;
						else if (temp.getOperator().equals(IROperator.isOP) && temp.getRightStr().equals("1"))
							expressionIsTrue = true;
						else if (temp.getOperator().equals(IROperator.isNotOP) && temp.getRightStr().equals("1"))
							expressionIsTrue = false;
						else if (temp.getOperator().equals(IROperator.isOP) && temp.getRightStr().equals("0"))
							expressionIsTrue = false;
						find = true;
						break;
					}
				}
				if (!find) continue;
				RefinedCondition refinedCondition2;
				String expression = refinedCondition.getRightStr();
				InstanceInvokeExpr exp = (InstanceInvokeExpr) refinedCondition.getRightValue();
				if(exp.getArgs().size()>0) {
					ValueObtainer vo = new ValueObtainer(sootMethod.getSignature());
					NestableObj obj = vo.getValueofVar(exp.getArg(0), refinedCondition.getUnit(), 0);
					List<Value> rightValues = new ArrayList<>();
					if (obj.getValues().size() > 0) {
						for (int i = 0; i < obj.getValues().size(); i++) {
							rightValues.add(StringConstant.v(obj.getValues().get(i)));
						}
					}
					// add the variable conservatively
					else{
						rightValues.add(exp.getArg(0));
					}
					if (expression.contains("equals") || expression.contains("contentEquals") || expression.contains("equalsIgnoreCase")) {
						for(Value rightValue : rightValues) {
							refinedCondition2 = new RefinedCondition(this, exp.getBase(),  IROperator.equalsOp, rightValue,
									refinedCondition.getUnit(),refinedCondition.isSatisfied());
							if(!expressionIsTrue) refinedCondition2.changeSatisfied();
							addRefinedCondition(refinedCondition2);
						}
						toBeDel.add(refinedCondition);
					} else if (expression.contains("contains")) {
						for(Value rightValue : rightValues){
							refinedCondition2 = new RefinedCondition(this, exp.getBase(),  IROperator.containsOP, rightValue,
									refinedCondition.getUnit(),refinedCondition.isSatisfied());
							if(!expressionIsTrue) refinedCondition2.changeSatisfied();
							addRefinedCondition(refinedCondition2);
						}
						toBeDel.add(refinedCondition);
					} else if (expression.contains("startsWith")) {
						for(Value rightValue : rightValues){
							refinedCondition2 = new RefinedCondition(this, exp.getBase(),  IROperator.startsWithOP, rightValue,
									refinedCondition.getUnit(),refinedCondition.isSatisfied());
							if(!expressionIsTrue) refinedCondition2.changeSatisfied();
							addRefinedCondition(refinedCondition2);
						}
						toBeDel.add(refinedCondition);
					} else if (expression.contains("endsWith")) {
						for(Value rightValue : rightValues){
							refinedCondition2 = new RefinedCondition(this, exp.getBase(),  IROperator.endsWithOP, rightValue,
									refinedCondition.getUnit(),refinedCondition.isSatisfied());
							if(!expressionIsTrue) refinedCondition2.changeSatisfied();
							addRefinedCondition(refinedCondition2);
						}
						toBeDel.add(refinedCondition);
					}else{
						for(Value rightValue : rightValues){
							refinedCondition2 = new RefinedCondition(this, exp.getBase(),  exp.getMethod().getName(), rightValue,
									refinedCondition.getUnit(),refinedCondition.isSatisfied());
							if(!expressionIsTrue) refinedCondition2.changeSatisfied();
							addRefinedCondition(refinedCondition2);
						}
						toBeDel.add(refinedCondition);
					}
				}
				if (expression.contains("isEmpty")) {
					refinedCondition2 = new RefinedCondition(this, exp.getBase(), IROperator.equalsOp, StringConstant.v(""),
							refinedCondition.getUnit(),refinedCondition.isSatisfied());
					if(!expressionIsTrue) refinedCondition2.changeSatisfied();
					addRefinedCondition(refinedCondition2);
					toBeDel.add(refinedCondition);
				}

			}
		}
		for(RefinedCondition temp: toBeDel){
			refinedConditions.remove(temp);
		}
	}

	private void optimizeDenotedVariable(List<RefinedCondition> list) {
		//optimize ($i1 is 0 + $i1 denote r0.<myThrow.ThrowTest: int outVar>) to (r0.<myThrow.ThrowTest: int outVar> equals 0)
		for(RefinedCondition refinedCondition: list) {
			if (refinedCondition.getOperator().equals(IROperator.denoteOP)) {
				if(refinedCondition.getRightStr().contains("<android.content.pm.ApplicationInfo: int targetSdkVersion>")){
					refinedCondition.setRightStr("targetSdkVersion");
				}
				for (RefinedCondition temp : list) {
					if (temp == refinedCondition) continue;
					if (temp.getLeftStr().equals(refinedCondition.getLeftStr())) {
						temp.setLeftVar(refinedCondition.getRightValue());
						if(refinedCondition.getRightValue() == null)
							temp.setLeftStr(refinedCondition.getRightStr());
					}
					if (temp.getRightStr().equals(refinedCondition.getLeftStr())) {
						temp.setRightValue(refinedCondition.getRightValue());
						if(refinedCondition.getRightValue() == null)
							temp.setLeftStr(refinedCondition.getRightStr());
					}
					if (temp.getRightStr().equals("lengthof "+refinedCondition.getLeftStr())) {
						temp.setRightStr(refinedCondition.getRightStr()+".length");
					}
					if (temp.getOperator().equals("equals") && temp.getLeftVar() == refinedCondition.getLeftVar()) {
						try {
							RefinedCondition refinedConditionClone = refinedCondition.clone(); //r0.<myThrow.ThrowTest: int outVar> is 0
							refinedConditionClone.setLeftVar(temp.getRightValue());
							addRefinedCondition(refinedConditionClone);

						} catch (CloneNotSupportedException e) {
							e.printStackTrace();
						}
					}else if (temp.getOperator().equals("equals") && temp.getRightValue() == refinedCondition.getLeftVar()) {
						try {
							RefinedCondition refinedConditionClone = refinedCondition.clone(); //r0.<myThrow.ThrowTest: int outVar> is 0
							refinedConditionClone.setLeftVar(temp.getLeftVar());
							addRefinedCondition(refinedConditionClone);
						} catch (CloneNotSupportedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	private void optimizeDenotedVariable2(List<RefinedCondition> list) {
		//optimize (r0 denote @this: myThrow.ThrowTest + r0.<myThrow.ThrowTest: int outVar> is 0
		// to (@this: myThrow.ThrowTest.<myThrow.ThrowTest: int outVar> equals 0)
		Set<RefinedCondition> toBeDel = new HashSet<>();
		for(RefinedCondition refinedCondition: list) {
			if (refinedCondition.getOperator().equals(IROperator.denoteOP)) {  //r0 denote @this: myThrow.ThrowTest
				for (RefinedCondition temp : list) {
					if (temp == refinedCondition) continue;
					if (temp.getLeftVar() instanceof InstanceFieldRef) { //r0.<myThrow.ThrowTest: int outVar> is 0
						InstanceFieldRef tempLeft = (InstanceFieldRef) temp.getLeftVar();
						if(tempLeft.getBase().toString().equals(refinedCondition.getLeftStr())){
							try {
								RefinedCondition tempClone = temp.clone(); //r0.<myThrow.ThrowTest: int outVar> is 0
								tempClone.setLeftStr(tempClone.getLeftStr().replace(tempLeft.getBase().toString()+".",refinedCondition.getRightStr()+"."));
								addRefinedCondition(tempClone);
								toBeDel.add(temp);
							} catch (CloneNotSupportedException e) {
								e.printStackTrace();
							}
						}
					}
					if (temp.getRightValue() instanceof InstanceInvokeExpr) {
						InstanceInvokeExpr tempRight = (InstanceInvokeExpr) temp.getRightValue();
						if(tempRight.getBase().toString().equals(refinedCondition.getLeftStr())){
							try {
								RefinedCondition tempClone = temp.clone();
								tempClone.setRightStr(tempClone.getRightStr().replace(tempRight.getBase().toString()+".",refinedCondition.getRightStr()+"."));
								addRefinedCondition(tempClone);
								toBeDel.add(temp);
							} catch (CloneNotSupportedException e) {
								e.printStackTrace();
							}
						}
					}
					if(temp.getLeftStr().startsWith(refinedCondition.getLeftStr() +" ") || temp.getLeftStr().startsWith(refinedCondition.getLeftStr() +".")){
						try {
							RefinedCondition tempClone = temp.clone(); //r0.<myThrow.ThrowTest: int outVar> is 0
							tempClone.setLeftStr(tempClone.getLeftStr().replace(refinedCondition.getLeftStr()+".",refinedCondition.getLeftStr()+"."));
							addRefinedCondition(tempClone);
							toBeDel.add(temp);
						} catch (CloneNotSupportedException e) {
							e.printStackTrace();
						}
					}else if(temp.getRightStr().startsWith(refinedCondition.getLeftStr() +" ") || temp.getRightStr().startsWith(refinedCondition.getLeftStr() +".")){
						try {
							RefinedCondition tempClone = temp.clone(); //r0.<myThrow.ThrowTest: int outVar> is 0
							tempClone.setRightStr(tempClone.getRightStr().replace(refinedCondition.getLeftStr()+".",refinedCondition.getRightStr())+".");
							addRefinedCondition(tempClone);
							toBeDel.add(temp);
						} catch (CloneNotSupportedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		for(RefinedCondition temp: toBeDel){
			refinedConditions.remove(temp);
		}
	}
	private void optimizeCmpInst(List<RefinedCondition> list) {
		for(RefinedCondition refinedCondition: list) {
			if(refinedCondition.getLeftStr().contains(" cmp ")){
				String left = refinedCondition.getLeftStr().split(" cmp ")[0];
				for(RefinedCondition leftDef: list) {
					if(leftDef.getLeftStr().equals(left) && (leftDef.getOperator().equals(IROperator.isOP)
							|| leftDef.getOperator().equals(IROperator.isiInvokeOP) || leftDef.getOperator().equals(IROperator.denoteOP))){
						left = leftDef.getRightStr();
					}
				}
				String right = refinedCondition.getLeftStr().split(" cmp ")[1];
				for(RefinedCondition rightDef: list) {
					if(rightDef.getLeftStr().equals(right) && (rightDef.getOperator().equals(IROperator.isOP)
							|| rightDef.getOperator().equals(IROperator.isiInvokeOP) || rightDef.getOperator().equals(IROperator.denoteOP))){
						right = rightDef.getRightStr();
					}
				}
				if(refinedCondition.getRightStr().equals("0")) {
					refinedCondition.setLeftStr(left);
					refinedCondition.setRightStr(right);
					if (refinedCondition.getOperator().equals(IROperator.isOP)) {
						refinedCondition.setOperator(IROperator.equalsOp);
					} else if (refinedCondition.getOperator().equals(IROperator.isNotOP)) {
						refinedCondition.setOperator(IROperator.notEqualsOp);
					}
				}
			}
		}
	}

	private void optimizeIsInvokeVariable(List<RefinedCondition> list) {
		Set<RefinedCondition> toBeDel = new HashSet<>();
		for(RefinedCondition refinedCondition: list) {
			if (refinedCondition.getOperator().equals(IROperator.isiInvokeOP)) {
				for (RefinedCondition temp : list) {
					if (temp == refinedCondition) continue;
					if (temp.getRightStr().equals(refinedCondition.getLeftStr())) {
						temp.setRightStr(refinedCondition.getRightStr());
						toBeDel.add(refinedCondition);
					}
					else if (temp.getLeftStr().equals(refinedCondition.getLeftStr())) {
						temp.setLeftStr(refinedCondition.getRightStr());
						toBeDel.add(refinedCondition);
					}
				}
			}
		}
		for(RefinedCondition temp: toBeDel){
			refinedConditions.remove(temp);
		}
	}

	private void optimizeIsVariable(List<RefinedCondition> list) {
		Set<RefinedCondition> toBeDel = new HashSet<>();
		for(RefinedCondition refinedCondition: list) {
			if (refinedCondition.getOperator().equals(IROperator.isOP) && refinedCondition.getLeftStr().startsWith("$")) {
				for (RefinedCondition temp : list) {
					if (temp == refinedCondition) continue;
					if (temp.getRightStr().equals(refinedCondition.getLeftStr())) {
						temp.setRightStr(refinedCondition.getRightStr());
						toBeDel.add(refinedCondition);
					}
					else if (temp.getLeftStr().equals(refinedCondition.getLeftStr())) {
						temp.setLeftStr(refinedCondition.getRightStr());
						toBeDel.add(refinedCondition);
					}
				}
			}
		}
		//i2 equals 0
		//parameter0 charAt i2 is zero
		for(RefinedCondition refinedCondition: list) {
			if (refinedCondition.getOperator().equals(IROperator.equalsOp) &&
					(refinedCondition.getLeftStr().startsWith("$") || refinedCondition.getLeftStr().startsWith("i"))) {
				for (RefinedCondition temp : list) {
					if (temp == refinedCondition) continue;
					if (temp.getRightStr().equals(refinedCondition.getLeftStr())) {
						temp.setRightValue(refinedCondition.getRightValue());
						temp.setRightStr(refinedCondition.getRightStr());
					}
					if (temp.getLeftStr().equals(refinedCondition.getLeftStr())) {
						temp.setLeftVar(refinedCondition.getRightValue());
						temp.setLeftStr(refinedCondition.getRightStr());
					}
				}
			}
		}

		for(RefinedCondition temp: toBeDel){
			refinedConditions.remove(temp);
		}
	}


	private void optimizePrintFormat(List<RefinedCondition> list) {
		for(RefinedCondition refinedCondition: list) {
			if(refinedCondition.getOperator().equals(IROperator.equalsOp) || refinedCondition.getOperator().equals(IROperator.notEqualsOp)) {
				if (refinedCondition.getRightStr().contains("parameter") && !refinedCondition.getLeftStr().contains("parameter")) {
					String temp = refinedCondition.getRightStr();
					refinedCondition.setRightStr(refinedCondition.getLeftStr());
					refinedCondition.setLeftStr(temp);
				}
			}
		}
		for(RefinedCondition refinedCondition: list) {
			String str = refinedCondition.getRightStr();
			if(str.contains("virtualinvoke") || str.contains("specialinvoke")|| str.contains("staticinvoke")
					|| str.contains("dynamicinvoke")|| str.contains("instanceinvoke")) {
				String[] ss = str.split(" ");
				refinedCondition.setRightStr(str.replace(ss[0]+" ", ""));
			}
			str = refinedCondition.getLeftStr();
			if(str.contains("virtualinvoke") || str.contains("specialinvoke")|| str.contains("staticinvoke")
					|| str.contains("dynamicinvoke")|| str.contains("instanceinvoke")) {
				String[] ss = str.split(" ");
				refinedCondition.setLeftStr(str.replace(ss[0]+" ", ""));
			}
		}
		for(RefinedCondition refinedCondition: list) {
			if(refinedCondition.getLeftStr().contains("<java.io.InputStream: int read()>()") && refinedCondition.getRightStr().equals("-1")) {
				refinedCondition.setRightStr("EOF");
			}
			if(refinedCondition.getRightStr().contains("<java.io.InputStream: int read()>()") && refinedCondition.getLeftStr().equals("-1")) {
				refinedCondition.setLeftStr("EOF");
			}

		}
	}
	private void optimizeRedundant(List<RefinedCondition> list) {
		Set<RefinedCondition> toBeDel = new HashSet<>();
		for(RefinedCondition refinedCondition: list) {
			if (refinedCondition.getOperator()!=null && refinedCondition.getOperator().equals(IROperator.denoteOP)) {
				toBeDel.add(refinedCondition);
			}
			else if (refinedCondition.getOperator()!=null && refinedCondition.getOperator().equals(IROperator.phiReplaceOp)) {
				toBeDel.add(refinedCondition);
			}
		}
		for(RefinedCondition refinedCondition: list) {
			if (refinedCondition.getLeftStr().startsWith("$") || refinedCondition.getLeftStr().startsWith("i")){
				toBeDel.add(refinedCondition);
			}
			else if (refinedCondition.getRightStr().startsWith("$") || refinedCondition.getRightStr().startsWith("i")){
				toBeDel.add(refinedCondition);
			}
//			if (!refinedCondition.getLeftStr().contains("this") && !refinedCondition.getLeftStr().contains("parameter")
//					&& !refinedCondition.getRightStr().contains("parameter")) {
//				toBeDel.add(refinedCondition);
//			}
		}
		for(RefinedCondition temp: toBeDel){
			refinedConditions.remove(temp);
		}
	}
}
