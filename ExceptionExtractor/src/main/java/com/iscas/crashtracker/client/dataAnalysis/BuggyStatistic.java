package com.iscas.crashtracker.client.dataAnalysis;

import com.iscas.crashtracker.base.MyConfig;
import com.iscas.crashtracker.client.exception.RelatedVarType;
import com.iscas.crashtracker.utils.FileUtils;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author hanada
 * @Date 2022/7/5 18:28
 * @Version 1.0
 */
public class BuggyStatistic {
    public void analyze() {
        analyzeAllFiles();
    }

    private void analyzeAllFiles() {
        String folder = MyConfig.getInstance().getResultFolder() +"statistic" +File.separator;
        FileUtils.writeText2File(  folder + "VarTypeStatistic.txt", "", false);
        FileUtils.writeText2File( folder + "RankingStatistic-Category.txt", "", false);
        FileUtils.writeText2File( folder + "RankingStatistic-RVType.txt", "", false);
        String[] fns = {};
        File dir = new File(folder +"buggyRanking");
        if (dir.isDirectory()) {
            fns = dir.list();
        }
        for(String fn : fns){
            List<String> lines = FileUtils.getListFromFile(folder+ "buggyRanking" + File.separator + fn);
            StringBuilder sb = new StringBuilder(fn+ "\tR@1\tR@5\tR@10\tR@1\tR@5\tR@10\tMRR\n");
            analyzeSingleFileRank(lines, sb);
            FileUtils.writeText2File( folder+ File.separator+"RankingStatistic-Category.txt", sb +"\n\n\n", true);

            StringBuilder sb2 = new StringBuilder(fn+ "\tParameter\tField\tParaAndField\tEmpty\tUnknown\n");
            analyzeSingleFileVarType(lines, sb2);
            FileUtils.writeText2File( folder+ File.separator+"VarTypeStatistic.txt", sb2 +"\n\n\n", true);

            StringBuilder sb3 = new StringBuilder(fn+ "\tR@1\tR@5\tR@10\tR@1\tR@5\tR@10\tMRR\n");
            analyzeSingleFileRankRVType(lines, sb3);
            FileUtils.writeText2File( folder + File.separator+"RankingStatistic-RVType.txt", sb3 +"\n\n\n", true);
        }
    }
    private Map initMap() {
        Map map = new HashMap();
        map.put("Recall@1", 0);
        map.put("Recall@5", 0);
        map.put("Recall@10", 0);
        map.put("Recall@all", 0);
        return map;
    }
    private void analyzeSingleFileVarType(List<String> lines, StringBuilder sb) {
        Map<String, Integer> cateAMap = initMap();
        Map<String, Integer> cateBMap = initMap();
        Map<String, Integer> cateCMap = initMap();
        Map<String, Integer> cateAllMap = initMap();

        for(String line: lines){
            String[] ss = line.split("\t");
            if(ss.length>6){
                String type = ss[0];
                String varType = ss[3];
                if(type.equals("A")) {
                    if(!cateAMap.containsKey(varType))
                        cateAMap.put(varType,0);
                    cateAMap.put(varType, cateAMap.get(varType)+1);
                }
                if(type.equals("B")) {
                    if(!cateBMap.containsKey(varType))
                        cateBMap.put(varType,0);
                    cateBMap.put(varType, cateBMap.get(varType)+1);
                }
                if(type.equals("C")) {
                    if(!cateCMap.containsKey(varType))
                        cateCMap.put(varType,0);
                    cateCMap.put(varType, cateCMap.get(varType)+1);
                }
                if(!cateAllMap.containsKey(varType))
                    cateAllMap.put(varType,0);
                cateAllMap.put(varType, cateAllMap.get(varType)+1);
            }
        }
        printTypeInfo2SB("All", cateAllMap, sb);
        printTypeInfo2SB("C", cateCMap, sb);
        printTypeInfo2SB("B", cateBMap, sb);
        printTypeInfo2SB("A", cateAMap, sb);


    }

    private void printTypeInfo2SB(String tag, Map<String, Integer> map, StringBuilder sb) {
        sb.append(tag + "\t");
        sb.append(map.get(RelatedVarType.Parameter.toString()) + "\t");
        sb.append(map.get(RelatedVarType.Field.toString()) + "\t");
        sb.append(map.get(RelatedVarType.ParaAndField.toString()) + "\t");
        sb.append(map.get(RelatedVarType.Empty.toString()) + "\t");
        sb.append(map.get(RelatedVarType.Unknown.toString()) + "\n");
    }

    private void analyzeSingleFileRank(List<String> lines, StringBuilder sb) {
        Map<String, Integer> cateAMap = initMap();
        Map<String, Integer> cateBMap = initMap();
        Map<String, Integer> cateCMap = initMap();
        Map<String, Integer> cateAllMap = initMap();
        
        int count = 0,countA= 0,countB= 0,countC= 0;
        int find = 0,rankSum = 0,candiSum= 0;
        double candiAvg= 0;
        double MRR = 0.0,MRRA= 0.0,MRRB= 0.0,MRRC= 0.0;

        for(String line: lines){
            String[] ss = line.split("\t");
            if(ss.length>6){
                count++;
                String type = ss[0];
                int rank = Integer.parseInt(ss[4]);
                if(rank!=999){
                    find++;
                    rankSum += rank;
                    MRR += 1.0/rank;
                }

                int candi = Integer.parseInt(ss[6]);
                candiSum+=candi;

                if(type.equals("A")) {
                    addRanking2Type(cateAMap, rank);
                    MRRA += 1.0/rank;
                    countA++;
                }
                if(type.equals("B")) {
                    addRanking2Type(cateBMap, rank);
                    MRRB += 1.0/rank;
                    countB++;
                }
                if(type.equals("C")) {
                    addRanking2Type(cateCMap, rank);
                    MRRC += 1.0/rank;
                    countC++;
                }
                addRanking2Type(cateAllMap, rank);

            }
        }
        candiAvg = candiSum*1.0/count;

//        log.info(countA +"  "+countB +"  "+ countC);
        printRankInfo2SB("All", cateAllMap, sb, count==0?0:MRR/count);
//        printRankInfo2SB("A", cateAMap, sb, countA==0?0:MRRA/countA);
        printRankInfo2SB("B", cateBMap, sb, countB==0?0:MRRB/countB);
//        printRankInfo2SB("C", cateCMap, sb, countC==0?0:MRRC/countC);

        int find_base = 568, rankSum_base = 717, CandiSum_base =3555;
        find_base = 0; rankSum_base = 0; CandiSum_base =0;
        double CandiAvg_base = 5.94+0.189;
        CandiAvg_base = 0.0;

        sb.append("Find\t"+ (find-find_base) + "\t");
        sb.append("rankSum\t"+ (rankSum-rankSum_base) + "\n");
        sb.append("CandiSum\t"+ (candiSum-CandiSum_base) + "\t");
        BigDecimal candiAvgb = new BigDecimal(candiAvg-CandiAvg_base);
        sb.append("CandiSum\t"+ candiAvgb.setScale(3, RoundingMode.HALF_UP).doubleValue() + "\n");
    }

    private void analyzeSingleFileRankRVType(List<String> lines, StringBuilder sb) {
        Map<String, Integer> catePMap = initMap();
        Map<String, Integer> cateFMap = initMap();
        Map<String, Integer> catePFMap = initMap();
        Map<String, Integer> cateEMap = initMap();
        Map<String, Integer> cateUMap = initMap();

        int count = 0,countP= 0,countF= 0,countPF= 0,countE= 0,countU= 0;
        double MRR = 0.0,MRRP= 0.0,MRRF= 0.0,MRRPF= 0.0,MRRE= 0.0,MRRU= 0.0;

        for(String line: lines){
            String[] ss = line.split("\t");
            if(ss.length>6){
                count++;
                String type = ss[3];
                int rank = Integer.parseInt(ss[4]);
                if(rank!=999){
                    MRR += 1.0/rank;
                }
                if(type.equals(RelatedVarType.Parameter.toString())) {
                    addRanking2Type(catePMap, rank);
                    MRRP += 1.0/rank;
                    countP++;
                }
                if(type.equals(RelatedVarType.Field.toString())) {
                    addRanking2Type(cateFMap, rank);
                    MRRF += 1.0/rank;
                    countF++;
                }
                if(type.equals(RelatedVarType.ParaAndField.toString())) {
                    addRanking2Type(catePFMap, rank);
                    MRRPF += 1.0/rank;
                    countPF++;
                }
                if(type.equals(RelatedVarType.Empty.toString())) {
                    addRanking2Type(cateEMap, rank);
                    MRRE += 1.0/rank;
                    countE++;
                }
                if(type.equals(RelatedVarType.Unknown.toString())) {
                    addRanking2Type(cateUMap, rank);
                    MRRU += 1.0/rank;
                    countU++;
                }
            }
        }
        printRankInfo2SB(RelatedVarType.Parameter.toString(), catePMap, sb, countP==0?0:MRRP/countP);
        printRankInfo2SB(RelatedVarType.Field.toString(), cateFMap, sb, countF==0?0:MRRF/countF);
        printRankInfo2SB(RelatedVarType.ParaAndField.toString(), catePFMap, sb, countPF==0?0:MRRPF/countPF);
        printRankInfo2SB(RelatedVarType.Empty.toString(), cateEMap, sb, countE==0?0:MRRE/countE);
        printRankInfo2SB(RelatedVarType.Unknown.toString(), cateUMap, sb, countU==0?0:MRRU/countU);

        sb.append("countP\t"+ countP + "\t");
        sb.append("countF\t"+ countF + "\t");
        sb.append("countPF\t"+ countPF + "\t");
        sb.append("countE\t"+ countE + "\t");
        sb.append("countU\t"+ countU + "\t");
    }

    private void printRankInfo2SB(String tag, Map<String, Integer> map, StringBuilder sb, double MRR) {
        int r1 = 0, r5 = 0,r10 = 0; double mrr = 0.0;
        if(tag.equals("All")){
            r1 = 499; r5 = 562; r10 = 567; mrr= 0.906;
            r1 = 0; r5 = 0; r10 = 0; mrr=0;
        }else if(tag.equals("B")){
            r1 = 14; r5 = 38; r10 = 43; mrr= 0.433;
            r1 = 0; r5 = 0; r10 = 0; mrr= 0;
        }
        sb.append(tag + "\t");
        sb.append(map.get("Recall@1")-r1 + "\t");
        sb.append(map.get("Recall@5")-r5 + "\t");
        sb.append(map.get("Recall@10")-r10 + "\t");

        BigDecimal b1 = new BigDecimal(map.get("Recall@1")==0?0:1.0* map.get("Recall@1")/map.get("Recall@all"));
        sb.append(b1.setScale(3,   BigDecimal.ROUND_HALF_UP).doubleValue() + "\t");

        BigDecimal b2 = new BigDecimal(map.get("Recall@5")==0?0:1.0* map.get("Recall@5")/map.get("Recall@all"));
        sb.append(b2.setScale(3,   BigDecimal.ROUND_HALF_UP).doubleValue() + "\t");

        BigDecimal b3 = new BigDecimal(map.get("Recall@10")==0?0:1.0* map.get("Recall@10")/map.get("Recall@all"));
        sb.append(b3.setScale(3,   BigDecimal.ROUND_HALF_UP).doubleValue() + "\t");

        BigDecimal b = new BigDecimal(MRR-mrr);
        sb.append(b.setScale(3,   BigDecimal.ROUND_HALF_UP).doubleValue() + "\n");
    }

    private void addRanking2Type(Map<String, Integer> map, int rank) {
        if(rank == 1){
            map.put("Recall@1", map.get("Recall@1")+1);
            map.put("Recall@5", map.get("Recall@5")+1);
            map.put("Recall@10", map.get("Recall@10")+1);
        }else if(rank <=5){
            map.put("Recall@5", map.get("Recall@5")+1);
            map.put("Recall@10", map.get("Recall@10")+1);
        }else if(rank <=10){
            map.put("Recall@10", map.get("Recall@10")+1);
        }
        map.put("Recall@all", map.get("Recall@all")+1);
    }


}
