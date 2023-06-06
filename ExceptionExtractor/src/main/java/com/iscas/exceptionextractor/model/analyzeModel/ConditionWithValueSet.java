package com.iscas.exceptionextractor.model.analyzeModel;

import com.iscas.exceptionextractor.model.sootAnalysisModel.NestableObj;
import com.iscas.exceptionextractor.utils.PrintUtils;
import com.iscas.exceptionextractor.utils.ValueObtainer;
import org.jetbrains.annotations.NotNull;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConditionWithValueSet implements  Cloneable {
	private Unit conditionUnit;
	private List<RefinedCondition> refinedConditions = new ArrayList<>();
	private SootMethod sootMethod;

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
		if(refinedCondition!=null && refinedCondition.toString().length()>0) {
			this.refinedConditions.add(refinedCondition);
		}
	}

	@Override
	public String toString() {
		return PrintUtils.printList(refinedConditions, "\n") ;
	}

	public void optimizeCondition() {
		List<RefinedCondition> list = new ArrayList<>();
		boolean optimize = true;
		if (optimize) {
			int id = 1, signature = 12345;
			while(list.hashCode()!=signature && id++<5) {
				//optimize, transport denoted variables to each use point
				signature = list.hashCode();
				list = new ArrayList(refinedConditions);
				optimizeBeforePhiVariable(list);

				//optimize, transport denoted variables to each use point
				signature = list.hashCode();
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
	 * trasfer phi to phi stmt
	 * @param list
	 */
	private void optimizeBeforePhiVariable(List<RefinedCondition> list) {

//		for(RefinedCondition refinedCondition: list) {
//			if (refinedCondition.getOperator().equals(IROperator.phiReplaceOp) &&
//					(!refinedCondition.getRightStr().startsWith("$") &&!refinedCondition.getRightStr().startsWith("i"))) {
//				for (RefinedCondition temp : list) {
//					if (temp == refinedCondition) continue;
//					if (temp.getRightStr().equals(refinedCondition.getLeftStr())) {
//						RefinedCondition refinedCondition2 = new RefinedCondition(this,
//								temp.getLeftVar(), temp.getOperator() , refinedCondition.getRightValue(), temp.getUnit(), temp.isSatisfied());
//						refinedCondition2.setRightStr(temp.getRightStr());
//						addRefinedCondition(refinedCondition2);
//					}
//				}
//			}
//		}
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
								rightInvoke.setArg(i, refinedCondition.getRightValue());
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
//			System.out.println("1 " +temp);
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
//			System.out.println("2 " +temp);
			refinedConditions.remove(temp);
		}
	}

	private void optimizeDenotedVariable(List<RefinedCondition> list) {
		//optimize ($i1 is 0 + $i1 denote r0.<myThrow.ThrowTest: int outVar>) to (r0.<myThrow.ThrowTest: int outVar> equals 0)
		for(RefinedCondition refinedCondition: list) {
			if (refinedCondition.getOperator().equals(IROperator.denoteOP)) {
				for (RefinedCondition temp : list) {
					if (temp == refinedCondition) continue;
					if (temp.getLeftStr().equals(refinedCondition.getLeftStr())) {
						temp.setLeftVar(refinedCondition.getRightValue());
					}
					if (temp.getRightStr().equals(refinedCondition.getLeftStr())) {
						temp.setRightValue(refinedCondition.getRightValue());
					}
					if (temp.getRightStr().equals("lengthof "+refinedCondition.getLeftStr())) {
						temp.setRightStr(refinedCondition.getRightStr()+".length");
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
								refinedConditions.add(tempClone);
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
								refinedConditions.add(tempClone);
								toBeDel.add(temp);
							} catch (CloneNotSupportedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		for(RefinedCondition temp: toBeDel){
//			System.out.println("3 " +temp);
			refinedConditions.remove(temp);
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
//			System.out.println("4 " +temp);
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
						temp.setRightStr(refinedCondition.getRightStr());
						temp.setRightValue(refinedCondition.getRightValue());
					}
					if (temp.getLeftStr().equals(refinedCondition.getLeftStr())) {
						temp.setLeftStr(refinedCondition.getRightStr());
						temp.setLeftVar(refinedCondition.getRightValue());
					}
				}
			}
		}

		for(RefinedCondition temp: toBeDel){
//			System.out.println("5 " +temp);
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
			if (!refinedCondition.getLeftStr().contains("this") && !refinedCondition.getLeftStr().contains("parameter")
					&& !refinedCondition.getRightStr().contains("parameter")) {
				toBeDel.add(refinedCondition);
			}
		}
		for(RefinedCondition temp: toBeDel){
//			System.out.println("6 " +temp);
			refinedConditions.remove(temp);
		}
	}
}
