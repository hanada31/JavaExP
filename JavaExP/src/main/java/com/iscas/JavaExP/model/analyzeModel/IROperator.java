package com.iscas.JavaExP.model.analyzeModel;

public class IROperator {


	public static final String phiReplaceOp = "phi replace";
	public static final String denoteOP = "denote";
	public static final String isiInvokeOP = "is invoke";

	public static final String largerOrEqualOP = "larger or equal";
	public static final String smallerOrEqualOP = "smaller or equal";
	public static final String smallerOP = "smaller than";
	public static final String largerOP = "larger than";

	public static final String notEqualsOp = "not equals";
	public static final String equalsOp = "equals";

	public static final String notStartsWithOP = "not startsWith";
	public static final String startsWithOP = "startsWith";

	public static final String notEndsWithOP = "not endsWith";
	public static final String endsWithOP = "endsWith";

	public static final String notContainsOP = "not contains";
	public static final String containsOP = "contains";

	public static final String isNotOP = "is not";
	public static final String isOP = "is";

	public static String[] operators = {phiReplaceOp, denoteOP, isiInvokeOP, largerOrEqualOP, smallerOrEqualOP, smallerOP, largerOP,
			notEqualsOp, equalsOp, notStartsWithOP, startsWithOP, notEndsWithOP, endsWithOP, notContainsOP, containsOP, isNotOP, isOP};


	public static boolean isReverseOperator(String op1, String op2) {
		if(op1.equals(isOP) && op2.equals(isNotOP)) return true;
		if(op1.equals(equalsOp) && op2.equals(notEqualsOp)) return true;
		if(op1.equals(smallerOP) && op2.equals(largerOrEqualOP)) return true;
		if(op1.equals(largerOP) && op2.equals(smallerOrEqualOP)) return true;
		if(op1.equals(containsOP) && op2.equals(notContainsOP)) return true;
		if(op1.equals(startsWithOP) && op2.equals(notStartsWithOP)) return true;
		if(op1.equals(endsWithOP) && op2.equals(notEndsWithOP)) return true;
		return false;
	}
}