//package com.iscas.exceptionextractor.solver;
//
//import com.iscas.exceptionextractor.model.analyzeModel.ConditionWithValueSet;
//import com.microsoft.z3.BoolExpr;
//import com.microsoft.z3.Context;
//import com.microsoft.z3.Solver;
//import com.microsoft.z3.Status;
//import org.sosy_lab.common.ShutdownManager;
//import org.sosy_lab.common.ShutdownNotifier;
//import org.sosy_lab.common.configuration.Configuration;
//import org.sosy_lab.common.configuration.InvalidConfigurationException;
//import org.sosy_lab.common.log.BasicLogManager;
//import org.sosy_lab.common.log.LogManager;
//import org.sosy_lab.java_smt.SolverContextFactory;
//import org.sosy_lab.java_smt.api.*;
//
//import java.util.*;
//
//public class ConstrainSolver {
//
//	public static  Set<String> checkSatisfiability(List<ConditionWithValueSet> globalPath) {
//		Set<String> res = new HashSet<String>();
//		List<StringBuilder> stmtStrs = createSTMTFileList(globalPath);
//		for(StringBuilder stmtStr : stmtStrs){
//			String modelofPath = checkSatisfiability(stmtStr.toString(),globalPath);
//			String  modelStr= modelofPath.toString().replace("\n", " ").replace("(", "").replace(")", "");
//	        while(modelStr.contains("  "))
//	        	modelStr = modelStr.replace("  ", " ");
//	        if(!res.contains(modelStr)){
//	        	res.add(modelStr);
//	        }
////	        Utils.printInfo(stmtStr+"\n"+PrintUtils.printset(res)+"\n");
//		}
//		return res;
//	}
//
//	public static String checkSatisfiability(String stmtStr, List<ConditionWithValueSet> globalPath) {
//		String res = "";
//		HashMap<String, String> cfg = new HashMap<String, String>();
//        cfg.put("model", "true");
//		Context ctx = new Context(cfg);
//        Solver s;
//        try{
//	        s = ctx.mkSolver();
//	        BoolExpr stmt = ctx.mkAnd(ctx.parseSMTLIB2String(stmtStr, null, null, null, null));
//	        s.add(stmt);
//	        Status result = s.check();
//	        if (result == Status.SATISFIABLE){
//	        	res = s.getModel().toString();
//	        }else{
//	        	res = "unsat!";
//	        }
//        }catch(Exception e){
//        	e.printStackTrace();
//        }finally{
//			ctx.close();
//			System.gc();
//        }
//		return res;
//	}
//
//	public static List<StringBuilder> createSTMTFileList(List<ConditionWithValueSet> globalPath) {
//		List<StringBuilder> sbs = new ArrayList<StringBuilder>();
//		sbs.add(new StringBuilder("(set-option :smt.string_solver z3str3)\n"));
//		//!!!!
//		sbs.add(new StringBuilder("(set-option :timeout 10000)\n"));
//		for (ConditionWithValueSet att: globalPath){
//			ListIterator <StringBuilder> iterator  = sbs.listIterator();
//			while (iterator.hasNext()) {
//				 StringBuilder sb = iterator.next();
//				 String ss[] = att.getValue().split(", ");
//				 for(int i = 0; i< ss.length; i++){
//						if(i!=ss.length-1){
//							StringBuilder sb_new = new StringBuilder(sb);
//							completeSb(att,sb_new,i);
//							iterator.add(sb_new);
//						}else{
//							completeSb(att,sb,i);
//						}
//					}
//			}
//		}
//		return sbs;
//	}
//
//	public void testZ3(){
//		Configuration  config = Configuration.defaultConfiguration();
//		LogManager logger = null;
//		ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
//		try {
//			logger = BasicLogManager.create(config);
//		} catch (InvalidConfigurationException e) {
//			e.printStackTrace();
//		}
//		ShutdownManager shutdown = ShutdownManager.create();
//		try (SolverContext context = SolverContextFactory.createSolverContext(
//				config, logger, shutdownNotifier, SolverContextFactory.Solvers.SMTINTERPOL)) {
//			IntegerFormulaManager imgr = context.getFormulaManager().getIntegerFormulaManager();
//
//			// Create formula "a = b" with two integer variables
//			NumeralFormula.IntegerFormula a = imgr.makeVariable("a");
//			NumeralFormula.IntegerFormula b = imgr.makeVariable("b");
//			BooleanFormula f = imgr.equal(a, b);
//
//			// Solve formula, get model, and print variable assignment
//			try (ProverEnvironment prover = context.newProverEnvironment(SolverContext.ProverOptions.GENERATE_MODELS)) {
//				prover.addConstraint(f);
//				boolean isUnsat = prover.isUnsat();
//				assert !isUnsat;
//				try (Model model = prover.getModel()) {
//					System.out.printf("SAT with a = %s, b = %s", model.evaluate(a), model.evaluate(b));
//				}
//			}
//		} catch (InvalidConfigurationException | SolverException | InterruptedException e) {
//			e.printStackTrace();
//		}
//	}
//	private static void completeSb(ConditionWithValueSet att, StringBuilder sb, int i) {
//		if(!att.getType().equals("extra")){
//			addADTStmt2Sb(att, sb, i);
////			Utils.printInfo(att);
//		}
//	}
//
//	public static void addADTStmt2Sb(ConditionWithValueSet att, StringBuilder sb, int i) {
//		String declareStr = "(declare-const " + att.getType() + " String)\n";
//		if(!sb.toString().contains(declareStr))
//			sb.append(declareStr);
//		String leftVal  = getLeftCondition(att);
//		String assertStr = "";
//		if(att.getValue().length()==0){
//			assertStr = "(= (str.len "+leftVal+") 0)";
//		}else{
//			if(att.getCondition().equals("equals") || att.getCondition().equals("contentEquals") || att.getCondition().equals("equalsIgnoreCase")){
//				String ss[] = att.getValue().split(", ");
//				assertStr = "(= " +leftVal+ " "+ss[i]+")";
//			}
//			else if(att.getCondition().equals("contains")){
//				String ss[] = att.getValue().split(", ");
//				assertStr = "(= " +leftVal+ " "+ss[i]+")";
//			}
//			else if(att.getCondition().equals("startsWith")){
//				String ss[] = att.getValue().split(", ");
//				assertStr = " (str.prefixof " +ss[i]+ " "+leftVal+")";
//			}
//			else if(att.getCondition().equals("endsWith")){
//				String ss[] = att.getValue().split(", ");
//				assertStr = " (str.suffixof " +ss[i]+ " "+leftVal+")";;
//			}
//			else if(att.getCondition().equals("contains")){
//				String ss[] = att.getValue().split(", ");
//				assertStr = "(= " +leftVal+ " "+ss[i]+")";
//			}
//			else if(att.getCondition().equals("nullChecker")){
//				assertStr = "(= (str.len "+leftVal+") 0)";
//			}
//			else if(att.getCondition().equals("isEmpty")){
//				assertStr = "(= (str.len "+leftVal+") 0)";
//			}
//		}
//		if(assertStr.length()>0){
//			if(att.getIsSatisfy() >0)
//				assertStr = "(assert " + assertStr + ")\n";
//			else
//				assertStr = "(assert (not " + assertStr + "))\n";
//		}
//		sb.append(assertStr);
//	}
//
//
//
//	private static String getLeftCondition(ConditionWithValueSet att) {
//		String leftVal = att.getType();
//		String conds[] = att.getConditionOfLeft().split(",");
//		for(String cond :conds){
//			if(cond.length()==0) continue;
//			String condLeft = cond.split(" ")[0];
//			if(condLeft.contains("substring")){
//				String[] ss = cond.split(" ");
//				//(str.substr action 0 3)
//				if(ss.length==2){
//					int e = att.getValue().length()-Integer.parseInt(ss[1]);
//					leftVal = "(str.substr " +leftVal + " "+ss[1]+" "+e+ ")";
//				}
//				else {
//					leftVal = "(str.substr " +leftVal + " "+ss[1]+" "+ss[2]+ ")";
//				}
//			}else if (condLeft.contains("charAt")){
//				//(str.at action 2)
//				String[] ss = cond.split(" ");
//				leftVal = "(str.at " +leftVal + " "+ss[1]+ ")";
//				try{
//					int charInt = Integer.parseInt(att.getValue().replace("\"", ""));
//					char ch = (char) charInt;
//					att.setValue("\""+String.valueOf(ch)+"\"");
//				}catch(NumberFormatException e) {
//					//nothing
//				}
//			}else if (condLeft.contains("concat")){
//				//(str.++ action 2)
//				String[] ss = cond.split(" ");
//				leftVal = "(str.++ " +leftVal + " \""+ss[1]+ "\")";
//			}
//		}
//		return leftVal;
//	}
//}
