package com.iscas.exceptionextractor.model.analyzeModel;

import com.iscas.exceptionextractor.model.sootAnalysisModel.NestableObj;
import com.iscas.exceptionextractor.utils.PrintUtils;
import com.iscas.exceptionextractor.utils.ValueObtainer;
import org.jetbrains.annotations.NotNull;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.StringConstant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConditionWithValueSet implements  Cloneable {
	private boolean isSatisfy = true;
	private Unit conditionUnit;
	private List<RefinedCondition> refinedConditions = new ArrayList<>();
	private SootMethod sootMethod;

	@Override
	public ConditionWithValueSet clone() throws CloneNotSupportedException {
		ConditionWithValueSet conditionWithValueSetClone = new ConditionWithValueSet(sootMethod, conditionUnit, isSatisfy);
		for(RefinedCondition refinedCondition: refinedConditions) {
			RefinedCondition refinedConditionClone = refinedCondition.clone();
			refinedConditionClone.setConditionWithValueSet(conditionWithValueSetClone);
			conditionWithValueSetClone.getRefinedConditions().add(refinedConditionClone);
		}

		return conditionWithValueSetClone;
	}

	public ConditionWithValueSet(SootMethod sootMethod, Unit conditionUnit, boolean isSatisfy){
		this.sootMethod = sootMethod;
		this.conditionUnit = conditionUnit;
		this.isSatisfy = isSatisfy;
	}


	public boolean getIsSatisfy() {
		return isSatisfy;
	}

	public void setIsSatisfy(boolean isSatisfy) {
		this.isSatisfy = isSatisfy;
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
		if(refinedCondition!=null) {
			this.refinedConditions.add(refinedCondition);
		}
	}

	@Override
	public String toString() {
		return PrintUtils.printList(refinedConditions, "\n") ;
	}

	public void optimizeCondition() {
		List<RefinedCondition> list;

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
		optimizeInInvokeVariable(list);

		//optimize, transport redundant variables to each use point
		list = new ArrayList(refinedConditions);
		optimizeRedundant(list);

		//optimize, remove variable not this and not parameter constraints
		list = new ArrayList(refinedConditions);
		optimizeOtherVariable(list);

	}


	private void optimizePhiVariable(List<RefinedCondition> list) {
		//optimize (RefinedCondition: r3_2 phi replace r3, $z0 is invoke virtualinvoke r0.<java.lang.String: boolean equals(java.lang.Object)>(r3_2))
		// to ($z0 is invoke virtualinvoke r0.<java.lang.String: boolean equals(java.lang.Object)>(r3))
		Set<RefinedCondition> toBeDel = new HashSet<>();
		for(RefinedCondition refinedCondition: list) {
			if (refinedCondition.getOperator().equals("phi replace")) {
				for (RefinedCondition temp : list) {
					if (temp == refinedCondition) continue;
					if(temp.getRightValue() instanceof  InvokeExpr){
						int i =0;
						for(Value value: ((InvokeExpr)temp.getRightValue()).getArgs()){
							if(value.toString().equals(refinedCondition.getLeftStr())){
								InvokeExpr rightInvoke = (InvokeExpr) (temp.getRightValue()).clone();
								rightInvoke.setArg(i, refinedCondition.getRightValue());
								RefinedCondition refinedCondition2 = new RefinedCondition(this,
										temp.getLeftVar(), temp.getOperator() , rightInvoke, temp.getUnit());
								addRefinedCondition(refinedCondition2);
								toBeDel.add(temp);
							}
							i++;
						}
					}
				}
			}
		}
		for(RefinedCondition refinedCondition: toBeDel){
			refinedConditions.remove(refinedCondition);
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
						if (temp.getOperator().equals("is not") && temp.getRightStr().equals("0"))
							expressionIsTrue = true;
						else if (temp.getOperator().equals("is") && temp.getRightStr().equals("1"))
							expressionIsTrue = true;
						else if (temp.getOperator().equals("is not") && temp.getRightStr().equals("1"))
							expressionIsTrue = false;
						else if (temp.getOperator().equals("is") && temp.getRightStr().equals("0"))
							expressionIsTrue = false;
						toBeDel.add(temp);
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
							refinedCondition2 = new RefinedCondition(this, exp.getBase(),  "equals", rightValue, refinedCondition.getUnit());
							if(!expressionIsTrue) refinedCondition2.changeSatisfied();
							addRefinedCondition(refinedCondition2);
						}
					} else if (expression.contains("contains")) {
						for(Value rightValue : rightValues){
							refinedCondition2 = new RefinedCondition(this, exp.getBase(),  "contains", rightValue, refinedCondition.getUnit());
							if(!expressionIsTrue) refinedCondition2.changeSatisfied();
							addRefinedCondition(refinedCondition2);
						}
					} else if (expression.contains("startsWith")) {
						for(Value rightValue : rightValues){
							refinedCondition2 = new RefinedCondition(this, exp.getBase(),  "startsWith", rightValue, refinedCondition.getUnit());
							if(!expressionIsTrue) refinedCondition2.changeSatisfied();
							addRefinedCondition(refinedCondition2);
						}
					} else if (expression.contains("endsWith")) {
						for(Value rightValue : rightValues){
							refinedCondition2 = new RefinedCondition(this, exp.getBase(),  "endsWith", rightValue, refinedCondition.getUnit());
							if(!expressionIsTrue) refinedCondition2.changeSatisfied();
							addRefinedCondition(refinedCondition2);
						}
					} else if (expression.contains("contains")) {
						for(Value rightValue : rightValues){
							refinedCondition2 = new RefinedCondition(this, exp.getBase(),  "contains", rightValue, refinedCondition.getUnit());
							if(!expressionIsTrue) refinedCondition2.changeSatisfied();
							addRefinedCondition(refinedCondition2);
						}
					}
				}
				if (expression.contains("isEmpty")) {
					refinedCondition2 = new RefinedCondition(this, exp.getBase(), "equals", StringConstant.v(""), refinedCondition.getUnit());
					if(!expressionIsTrue) refinedCondition2.changeSatisfied();
					addRefinedCondition(refinedCondition2);
				}
				toBeDel.add(refinedCondition);
			}
		}
		for(RefinedCondition refinedCondition: toBeDel){
			refinedConditions.remove(refinedCondition);
		}
	}

	private void optimizeDenotedVariable(List<RefinedCondition> list) {
		//optimize ($i1 is 0 + $i1 denote r0.<myThrow.ThrowTest: int outVar>) to (r0.<myThrow.ThrowTest: int outVar> equals 0)
		for(RefinedCondition refinedCondition: list) {
			if (refinedCondition.getOperator().equals("denote")) {
				for (RefinedCondition temp : list) {
					if (temp == refinedCondition) continue;
					if (temp.getLeftStr().equals(refinedCondition.getLeftStr())) {
						temp.setLeftVar(refinedCondition.getRightValue());
					}
					if (temp.getRightStr().equals(refinedCondition.getLeftStr())) {
						temp.setRightValue(refinedCondition.getRightValue());
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
			if (refinedCondition.getOperator().equals("denote")) {  //r0 denote @this: myThrow.ThrowTest
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
		for(RefinedCondition refinedCondition: toBeDel){
			refinedConditions.remove(refinedCondition);
		}
	}



	private void optimizeInInvokeVariable(List<RefinedCondition> list) {
		Set<RefinedCondition> toBeDel = new HashSet<>();
		for(RefinedCondition refinedCondition: list) {
			if (refinedCondition.getOperator().equals("is invoke")) {
				for (RefinedCondition temp : list) {
					if (temp == refinedCondition) continue;
					if (temp.getRightStr().equals(refinedCondition.getLeftStr())) {
						temp.setRightStr(refinedCondition.getRightStr());
						toBeDel.add(refinedCondition);
					}
				}
			}
		}
		for(RefinedCondition refinedCondition: toBeDel){
			refinedConditions.remove(refinedCondition);
		}
	}
	private void optimizeRedundant(List<RefinedCondition> list) {
		Set<RefinedCondition> toBeDel = new HashSet<>();
		for(RefinedCondition refinedCondition: list) {
			if (refinedCondition.getOperator()!=null && refinedCondition.getOperator().equals("denote")) {
				toBeDel.add(refinedCondition);
			}
			if (refinedCondition.getOperator()!=null && refinedCondition.getOperator().equals("phi replace")) {
				toBeDel.add(refinedCondition);
			}
		}
		for(RefinedCondition refinedCondition: toBeDel){
			refinedConditions.remove(refinedCondition);
		}
	}

	private void optimizeOtherVariable(List<RefinedCondition> list) {
		Set<RefinedCondition> toBeDel = new HashSet<>();
		for(RefinedCondition refinedCondition: list) {
			if (!refinedCondition.getLeftStr().contains("this") && !refinedCondition.getLeftStr().contains("parameter")) {
				toBeDel.add(refinedCondition);
			}
		}
		for(RefinedCondition refinedCondition: toBeDel){
			refinedConditions.remove(refinedCondition);
		}
	}

}
